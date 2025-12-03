package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
import net.p3pp3rf1y.sophisticatedcore.util.MutableStackItemAccess;
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
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (isInCooldown(level)) {
			return;
		}
		setCooldown(level, storageWrapper.getFluidHandler().map(storageFluidHandler -> tick(storageFluidHandler, entity, level, pos)).orElse(DID_NOTHING_COOLDOWN_TIME));
	}

	private int tick(ResourceHandler<FluidResource> storageFluidHandler, @Nullable Entity entity, Level level, BlockPos pos) {
		if (shouldInteractWithHand()) {
			if (entity instanceof Player player) {
				if (handleFluidContainerInHands(player, storageFluidHandler)) {
					lastHandActionTime = level.getGameTime();
					return HAND_INTERACTION_COOLDOWN_TIME;
				}
			} else if (handleFluidContainersInHandsOfNearbyPlayers(level, pos, storageFluidHandler)) {
				lastHandActionTime = level.getGameTime();
				return HAND_INTERACTION_COOLDOWN_TIME;
			}
		}
		return handleInWorldInteractions(storageFluidHandler, entity, level, pos)
				.orElseGet(() -> lastHandActionTime + 10 * HAND_INTERACTION_COOLDOWN_TIME > level.getGameTime() ? HAND_INTERACTION_COOLDOWN_TIME : DID_NOTHING_COOLDOWN_TIME);
	}

	private Optional<Integer> handleInWorldInteractions(ResourceHandler<FluidResource> storageFluidHandler, @Nullable Entity entity, Level level, BlockPos pos) {
		if (shouldInteractWithWorld()) {
			Optional<Integer> newCooldown = interactWithWorld(level, pos, storageFluidHandler, entity);
			if (newCooldown.isPresent()) {
				return newCooldown;
			}
		}
		if (shouldInteractWithFluidHandlers()) {
			return interactWithAttachedFluidHandlers(level, pos, storageFluidHandler);
		}
		return Optional.empty();
	}

	private Optional<Integer> interactWithAttachedFluidHandlers(Level level, BlockPos pos, ResourceHandler<FluidResource> storageFluidHandler) {
		for (Direction dir : Direction.values()) {
			boolean successful = WorldHelper.getBlockEntity(level, pos.offset(dir.getUnitVec3i())).map(be ->
					CapabilityHelper.<Boolean>getFromFluidHandler(be, dir.getOpposite(), fluidHandler -> {
						if (isInput()) {
							return tryFluidTransfer(fluidHandler, storageFluidHandler, getMaxInOut());
						} else {
							return tryFluidTransfer(storageFluidHandler, fluidHandler, getMaxInOut());
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

	private Optional<Integer> interactWithWorld(Level level, BlockPos pos, ResourceHandler<FluidResource> storageFluidHandler, @Nullable Entity entity) {
		if (isInput()) {
			return fillFromBlockInRange(level, pos, storageFluidHandler, entity);
		} else {
			for (Direction dir : Direction.values()) {
				BlockPos offsetPos = pos.offset(dir.getUnitVec3i());
				if (placeFluidInWorld(level, storageFluidHandler, dir, offsetPos)) {
					return Optional.of(WORLD_INTERACTION_COOLDOWN_TIME);
				}
			}
		}
		return Optional.empty();
	}

	private boolean placeFluidInWorld(Level level, ResourceHandler<FluidResource> storageFluidHandler, Direction dir, BlockPos offsetPos) {
		if (dir != Direction.UP) {
			try (Transaction tx = Transaction.openRoot()) {
				for (int tank = 0; tank < storageFluidHandler.size(); tank++) {
					FluidResource tankFluid = storageFluidHandler.getResource(tank);
					if (!tankFluid.isEmpty() && fluidFilterLogic.fluidMatches(tankFluid) && isValidForFluidPlacement(level, offsetPos)
							&& storageFluidHandler.extract(tankFluid, FluidType.BUCKET_VOLUME, tx) == FluidType.BUCKET_VOLUME
							&& FluidUtil.tryPlaceFluid(tankFluid, null, level, InteractionHand.MAIN_HAND, offsetPos)) {
						tx.commit();
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isValidForFluidPlacement(Level level, BlockPos offsetPos) {
		BlockState blockState = level.getBlockState(offsetPos);
		return blockState.isAir() || (!blockState.getFluidState().isEmpty() && !blockState.getFluidState().isSource());
	}

	private Optional<Integer> fillFromBlockInRange(Level level, BlockPos basePos, ResourceHandler<FluidResource> storageFluidHandler, @Nullable Entity entity) {
		LinkedList<BlockPos> nextPositions = new LinkedList<>();
		Set<BlockPos> searchedPositions = new HashSet<>();
		nextPositions.add(basePos);

		while (!nextPositions.isEmpty()) {
			BlockPos pos = nextPositions.poll();
			if (fillFromBlock(level, pos, storageFluidHandler, entity)) {
				return Optional.of((int) (Math.max(1, Math.sqrt(basePos.distSqr(pos))) * WORLD_INTERACTION_COOLDOWN_TIME));
			}

			for (Direction dir : Direction.values()) {
				BlockPos offsetPos = pos.offset(dir.getUnitVec3i());
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

	private boolean fillFromBlock(Level level, BlockPos pos, ResourceHandler<FluidResource> storageFluidHandler, @Nullable Entity entity) {
		FluidState fluidState = level.getFluidState(pos);
		if (!fluidState.isEmpty()) {
			BlockState state = level.getBlockState(pos);
			Block block = state.getBlock();
			if (block instanceof BucketPickup bucketPickup) {
				return pickupBlock(level, pos, storageFluidHandler, bucketPickup, fluidState, state);
			}
			ResourceHandler<FluidResource> fluidHandler = level.getCapability(Capabilities.Fluid.BLOCK, pos, null);
			if (fluidHandler == null) {
				return false;
			}
			return tryFluidTransfer(fluidHandler, storageFluidHandler);
		}
		return false;
	}

	private static boolean pickupBlock(Level level, BlockPos pos, ResourceHandler<FluidResource> storageFluidHandler, BucketPickup bucketPickup, FluidState fluidState, BlockState state) {
		Fluid fluid = fluidState.getType();
		try (Transaction tx = Transaction.openRoot()) {
			if (storageFluidHandler.insert(FluidResource.of(fluid), FluidType.BUCKET_VOLUME, tx) == FluidType.BUCKET_VOLUME) {
				bucketPickup.pickupBlock(null, level, pos, state);
				tx.commit();
				return true;
			}
		}
		return false;
	}

	private boolean handleFluidContainersInHandsOfNearbyPlayers(Level level, BlockPos pos, ResourceHandler<FluidResource> storageFluidHandler) {
		AABB searchBox = new AABB(pos).inflate(PLAYER_SEARCH_RANGE);
		for (Player player : level.players()) {
			if (searchBox.contains(player.getX(), player.getY(), player.getZ()) && handleFluidContainerInHands(player, storageFluidHandler)) {
				return true;
			}
		}
		return false;
	}

	private boolean handleFluidContainerInHands(Player player, ResourceHandler<FluidResource> storageFluidHandler) {
		return handleFluidContainerInHand(storageFluidHandler, player, InteractionHand.MAIN_HAND) || handleFluidContainerInHand(storageFluidHandler, player, InteractionHand.OFF_HAND);
	}

	private boolean handleFluidContainerInHand(ResourceHandler<FluidResource> storageFluidHandler, Player player, InteractionHand hand) {
		ItemStack itemInHand = player.getItemInHand(hand);
		if (itemInHand.getCount() != 1 || itemInHand == storageWrapper.getWrappedStorageStack()) {
			return false;
		}
		MutableStackItemAccess itemAccess = new MutableStackItemAccess(itemInHand);
		return CapabilityHelper.getFromFluidHandler(itemAccess, itemFluidHandler -> {
			if (isInput()) {
				if (tryFluidTransfer(itemFluidHandler, storageFluidHandler)) {
					player.setItemInHand(hand, itemAccess.getStack());
					return true;
				}
				return false;
			} else {
				if (tryFluidTransfer(storageFluidHandler, itemFluidHandler)) {
					player.setItemInHand(hand, itemAccess.getStack());
					return true;
				}
				return false;
			}
		}, false);
	}

	private boolean tryFluidTransfer(ResourceHandler<FluidResource> fluidHandler, ResourceHandler<FluidResource> storageFluidHandler) {
		return tryFluidTransfer(fluidHandler, storageFluidHandler, FluidType.BUCKET_VOLUME);
	}

	private boolean tryFluidTransfer(ResourceHandler<FluidResource> source, ResourceHandler<FluidResource> target, int maxDrain) {
		for (int i = 0; i < source.size(); i++) {
			FluidResource sourceFluid = source.getResource(i);
			if (!sourceFluid.isEmpty() && fluidFilterLogic.fluidMatches(sourceFluid)) {
				try (Transaction tx = Transaction.openRoot()) {
					int inserted = target.insert(sourceFluid, Math.min(maxDrain, source.getAmountAsInt(i)), tx);
					if (inserted <= 0) {
						continue;
					}
					if (source.extract(sourceFluid, inserted, tx) == inserted) {
						tx.commit();
						return true;
					}
				}
			}
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

	public void setInteractWithFluidHandlers(boolean interactWithFluidHandlers) {
		upgrade.set(ModCoreDataComponents.INTERACT_WITH_FLUID_HANDLERS, interactWithFluidHandlers);
		save();
	}

	public boolean shouldInteractWithFluidHandlers() {
		return upgrade.getOrDefault(ModCoreDataComponents.INTERACT_WITH_FLUID_HANDLERS, upgradeItem.getInteractWithFluidHandlersDefault());
	}
}
