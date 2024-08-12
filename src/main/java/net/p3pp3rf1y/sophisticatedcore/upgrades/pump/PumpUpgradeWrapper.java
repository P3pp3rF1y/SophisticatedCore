package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.wrappers.BucketPickupHandlerWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class PumpUpgradeWrapper extends UpgradeWrapperBase<PumpUpgradeWrapper, PumpUpgradeItem> implements ITickableUpgrade {
	private static final int DID_NOTHING_COOLDOWN_TIME = 40;
	private static final int HAND_INTERACTION_COOLDOWN_TIME = 3;
	private static final int WORLD_INTERACTION_COOLDOWN_TIME = 20;
	private static final int FLUID_HANDLER_INTERACTION_COOLDOWN_TIME = 20;
	private static final int PLAYER_SEARCH_RANGE = 3;
	private static final int PUMP_IN_WORLD_RANGE = 4;
	private static final int PUMP_IN_WORLD_RANGE_SQR = PUMP_IN_WORLD_RANGE * PUMP_IN_WORLD_RANGE;

	private long lastHandActionTime = -1;
	private final FluidFilterLogic fluidFilterLogic;
	private final PumpUpgradeConfig pumpUpgradeConfig;

	protected PumpUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		pumpUpgradeConfig = upgradeItem.getPumpUpgradeConfig();
		fluidFilterLogic = new FluidFilterLogic(pumpUpgradeConfig.filterSlots.get(), upgrade, upgradeSaveHandler);
	}

	@Override
	public void tick(@Nullable LivingEntity entity, Level level, BlockPos pos) {
		if (isInCooldown(level)) {
			return;
		}
		setCooldown(level, storageWrapper.getFluidHandler().map(storageFluidHandler -> tick(storageFluidHandler, entity, level, pos)).orElse(DID_NOTHING_COOLDOWN_TIME));
	}

	private int tick(IFluidHandlerItem storageFluidHandler, @Nullable LivingEntity entity, Level level, BlockPos pos) {
		if (entity == null) {
			Optional<Integer> newCooldown = handleInWorldInteractions(storageFluidHandler, (Player) entity, level, pos);
			if (newCooldown.isPresent()) {
				return newCooldown.get();
			}
		} else {
			if (shouldInteractWithHand() && entity instanceof Player player && handleFluidContainerInHands(player, storageFluidHandler)) {
				lastHandActionTime = level.getGameTime();
				return HAND_INTERACTION_COOLDOWN_TIME;
			}
			Optional<Integer> newCooldown = handleInWorldInteractions(storageFluidHandler, (Player) entity, level, pos);
			if (newCooldown.isPresent()) {
				return newCooldown.get();
			}
		}
		return lastHandActionTime + 10 * HAND_INTERACTION_COOLDOWN_TIME > level.getGameTime() ? HAND_INTERACTION_COOLDOWN_TIME : DID_NOTHING_COOLDOWN_TIME;
	}

	private Optional<Integer> handleInWorldInteractions(IFluidHandlerItem storageFluidHandler, @Nullable Player player, Level level, BlockPos pos) {
		if (shouldInteractWithHand() && handleFluidContainersInHandsOfNearbyPlayers(level, pos, storageFluidHandler)) {
			lastHandActionTime = level.getGameTime();
			return Optional.of(HAND_INTERACTION_COOLDOWN_TIME);
		}

		if (shouldInteractWithWorld()) {
			Optional<Integer> newCooldown = interactWithWorld(level, pos, storageFluidHandler, player);
			if (newCooldown.isPresent()) {
				return newCooldown;
			}
		}

		return interactWithAttachedFluidHandlers(level, pos, storageFluidHandler);
	}

	private Optional<Integer> interactWithAttachedFluidHandlers(Level level, BlockPos pos, IFluidHandler storageFluidHandler) {
		for (Direction dir : Direction.values()) {
			boolean successful = WorldHelper.getBlockEntity(level, pos.offset(dir.getNormal())).map(be ->
					CapabilityHelper.<Boolean>getFromFluidHandler(be, dir.getOpposite(), fluidHandler -> {
						if (isInput()) {
							return fillFromFluidHandler(fluidHandler, storageFluidHandler, getMaxInOut());
						} else {
							return fillFluidHandler(fluidHandler, storageFluidHandler, getMaxInOut());
						}
					}, false)).orElse(false);
			if (successful) {
				return Optional.of(FLUID_HANDLER_INTERACTION_COOLDOWN_TIME);
			}
		}

		return Optional.empty();
	}

	private int getMaxInOut() {
		return Math.max(FluidType.BUCKET_VOLUME, pumpUpgradeConfig.maxInputOutput.get() * storageWrapper.getNumberOfSlotRows() * getAdjustedStackMultiplier(storageWrapper));
	}

	public int getAdjustedStackMultiplier(IStorageWrapper storageWrapper) {
		return 1 + (int) (pumpUpgradeConfig.stackMultiplierRatio.get() * (storageWrapper.getInventoryHandler().getStackSizeMultiplier() - 1));
	}

	private Optional<Integer> interactWithWorld(Level level, BlockPos pos, IFluidHandler storageFluidHandler, @Nullable Player player) {
		if (isInput()) {
			return fillFromBlockInRange(level, pos, storageFluidHandler, player);
		} else {
			for (Direction dir : Direction.values()) {
				BlockPos offsetPos = pos.offset(dir.getNormal());
				if (placeFluidInWorld(level, storageFluidHandler, dir, offsetPos)) {
					return Optional.of(WORLD_INTERACTION_COOLDOWN_TIME);
				}
			}
		}
		return Optional.empty();
	}

	private boolean placeFluidInWorld(Level level, IFluidHandler storageFluidHandler, Direction dir, BlockPos offsetPos) {
		if (dir != Direction.UP) {
			for (int tank = 0; tank < storageFluidHandler.getTanks(); tank++) {
				FluidStack tankFluid = storageFluidHandler.getFluidInTank(tank);
				if (!tankFluid.isEmpty() && fluidFilterLogic.fluidMatches(tankFluid)
						&& isValidForFluidPlacement(level, offsetPos) && FluidUtil.tryPlaceFluid(null, level, InteractionHand.MAIN_HAND, offsetPos, storageFluidHandler, tankFluid)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isValidForFluidPlacement(Level level, BlockPos offsetPos) {
		BlockState blockState = level.getBlockState(offsetPos);
		return blockState.isAir() || (!blockState.getFluidState().isEmpty() && !blockState.getFluidState().isSource());
	}

	private Optional<Integer> fillFromBlockInRange(Level level, BlockPos basePos, IFluidHandler storageFluidHandler, @Nullable Player player) {
		LinkedList<BlockPos> nextPositions = new LinkedList<>();
		Set<BlockPos> searchedPositions = new HashSet<>();
		nextPositions.add(basePos);

		while (!nextPositions.isEmpty()) {
			BlockPos pos = nextPositions.poll();
			if (fillFromBlock(level, pos, storageFluidHandler, player)) {
				return Optional.of((int) (Math.max(1, Math.sqrt(basePos.distSqr(pos))) * WORLD_INTERACTION_COOLDOWN_TIME));
			}

			for (Direction dir : Direction.values()) {
				BlockPos offsetPos = pos.offset(dir.getNormal());
				if (!searchedPositions.contains(offsetPos)) {
					searchedPositions.add(offsetPos);
					if (basePos.distSqr(offsetPos) < PUMP_IN_WORLD_RANGE_SQR) {
						nextPositions.add(offsetPos);
					}
				}
			}
		}
		return Optional.empty();
	}

	private boolean fillFromBlock(Level level, BlockPos pos, IFluidHandler storageFluidHandler, @Nullable Player player) {
		FluidState fluidState = level.getFluidState(pos);
		if (!fluidState.isEmpty()) {
			BlockState state = level.getBlockState(pos);
			Block block = state.getBlock();
			IFluidHandler targetFluidHandler;
			if (block instanceof BucketPickup bucketPickup) {
				targetFluidHandler = new BucketPickupHandlerWrapper(player, bucketPickup, level, pos);
			} else {
				Optional<IFluidHandler> fluidHandler = FluidUtil.getFluidHandler(level, pos, null);
				if (fluidHandler.isEmpty()) {
					return false;
				}
				targetFluidHandler = fluidHandler.get();
			}
			return fillFromFluidHandler(targetFluidHandler, storageFluidHandler);
		}
		return false;
	}

	private boolean handleFluidContainersInHandsOfNearbyPlayers(Level level, BlockPos pos, IFluidHandler storageFluidHandler) {
		AABB searchBox = new AABB(pos).inflate(PLAYER_SEARCH_RANGE);
		for (Player player : level.players()) {
			if (searchBox.contains(player.getX(), player.getY(), player.getZ()) && handleFluidContainerInHands(player, storageFluidHandler)) {
				return true;
			}
		}
		return false;
	}

	private boolean handleFluidContainerInHands(Player player, IFluidHandler storageFluidHandler) {
		return handleFluidContainerInHand(storageFluidHandler, player, InteractionHand.MAIN_HAND) || handleFluidContainerInHand(storageFluidHandler, player, InteractionHand.OFF_HAND);
	}

	private boolean handleFluidContainerInHand(IFluidHandler storageFluidHandler, Player player, InteractionHand hand) {
		ItemStack itemInHand = player.getItemInHand(hand);
		if (itemInHand.getCount() != 1 || itemInHand == storageWrapper.getWrappedStorageStack()) {
			return false;
		}
		return CapabilityHelper.getFromFluidHandler(itemInHand, itemFluidHandler -> {
			if (isInput()) {
				return fillFromHand(player, hand, itemFluidHandler, storageFluidHandler);
			} else {
				return fillContainerInHand(player, hand, itemFluidHandler, storageFluidHandler);
			}
		}, false);
	}

	private boolean fillContainerInHand(Player player, InteractionHand hand, IFluidHandlerItem itemFluidHandler, IFluidHandler storageFluidHandler) {
		boolean ret = fillFluidHandler(itemFluidHandler, storageFluidHandler);
		if (ret) {
			player.setItemInHand(hand, itemFluidHandler.getContainer());
		}
		return ret;
	}

	private boolean fillFluidHandler(IFluidHandler fluidHandler, IFluidHandler storageFluidHandler) {
		return fillFluidHandler(fluidHandler, storageFluidHandler, FluidType.BUCKET_VOLUME);
	}

	private boolean fillFluidHandler(IFluidHandler fluidHandler, IFluidHandler storageFluidHandler, int maxFill) {
		boolean ret = false;
		for (int tank = 0; tank < storageFluidHandler.getTanks(); tank++) {
			FluidStack tankFluid = storageFluidHandler.getFluidInTank(tank);
			if (!tankFluid.isEmpty() && fluidFilterLogic.fluidMatches(tankFluid)
					&& !FluidUtil.tryFluidTransfer(fluidHandler, storageFluidHandler, new FluidStack(tankFluid.getFluid(), maxFill), true).isEmpty()) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	private boolean fillFromHand(Player player, InteractionHand hand, IFluidHandlerItem itemFluidHandler, IFluidHandler storageFluidHandler) {
		if (fillFromFluidHandler(itemFluidHandler, storageFluidHandler)) {
			player.setItemInHand(hand, itemFluidHandler.getContainer());
			return true;
		}
		return false;
	}

	private boolean fillFromFluidHandler(IFluidHandler fluidHandler, IFluidHandler storageFluidHandler) {
		return fillFromFluidHandler(fluidHandler, storageFluidHandler, FluidType.BUCKET_VOLUME);
	}

	private boolean fillFromFluidHandler(IFluidHandler fluidHandler, IFluidHandler storageFluidHandler, int maxDrain) {
		FluidStack containedFluid = fluidHandler.drain(maxDrain, IFluidHandler.FluidAction.SIMULATE);
		if (!containedFluid.isEmpty() && fluidFilterLogic.fluidMatches(containedFluid)) {
			return !FluidUtil.tryFluidTransfer(storageFluidHandler, fluidHandler, containedFluid, true).isEmpty();
		}
		return false;
	}

	public void setIsInput(boolean input) {
		upgrade.set(ModCoreDataComponents.IS_INPUT, input);
		save();
	}

	public boolean isInput() {
		return upgrade.getOrDefault(ModCoreDataComponents.IS_INPUT, true);
	}

	public FluidFilterLogic getFluidFilterLogic() {
		return fluidFilterLogic;
	}

	public void setInteractWithHand(boolean interactWithHand) {
		upgrade.set(ModCoreDataComponents.INTERACT_WITH_HAND, interactWithHand);
		save();
	}

	public boolean shouldInteractWithHand() {
		return upgrade.getOrDefault(ModCoreDataComponents.INTERACT_WITH_HAND, upgradeItem.getInteractWithHandDefault());
	}

	public void setInteractWithWorld(boolean interactWithWorld) {
		upgrade.set(ModCoreDataComponents.INTERACT_WITH_WORLD, interactWithWorld);
		save();
	}

	public boolean shouldInteractWithWorld() {
		return upgrade.getOrDefault(ModCoreDataComponents.INTERACT_WITH_WORLD, upgradeItem.getInteractWithWorldDefault());
	}
}
