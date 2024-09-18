package net.p3pp3rf1y.sophisticatedcore.upgrades.magnet;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import net.p3pp3rf1y.sophisticatedcore.util.XpHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class MagnetUpgradeWrapper extends UpgradeWrapperBase<MagnetUpgradeWrapper, MagnetUpgradeItem>
		implements IContentsFilteredUpgrade, ITickableUpgrade, IPickupResponseUpgrade {
	private static final String PREVENT_REMOTE_MOVEMENT = "PreventRemoteMovement";
	private static final String ALLOW_MACHINE_MOVEMENT = "AllowMachineRemoteMovement";

	private static final int COOLDOWN_TICKS = 10;
	private static final int FULL_COOLDOWN_TICKS = 40;
	private final ContentsFilterLogic filterLogic;

	private static final Set<IMagnetPreventionChecker> magnetCheckers = new HashSet<>();

	public static void addMagnetPreventionChecker(IMagnetPreventionChecker checker) {
		magnetCheckers.add(checker);
	}

	public MagnetUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new ContentsFilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(),
				storageWrapper::getInventoryHandler, storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class),
				ModCoreDataComponents.FILTER_ATTRIBUTES);
	}

	@Override
	public ContentsFilterLogic getFilterLogic() {
		return filterLogic;
	}

	@Override
	public ItemStack pickup(Level level, ItemStack stack, boolean simulate) {
		if (!shouldPickupItems() || !filterLogic.matchesFilter(stack)) {
			return stack;
		}

		return storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
	}

	@Override
	public void tick(@Nullable LivingEntity entity, Level level, BlockPos pos) {
		if (isInCooldown(level)) {
			return;
		}

		int cooldown = shouldPickupItems() ? pickupItems(entity, level, pos) : FULL_COOLDOWN_TICKS;

		if (shouldPickupXp() && canFillStorageWithXp()) {
			cooldown = Math.min(cooldown, pickupXpOrbs(entity, level, pos));
		}

		setCooldown(level, cooldown);
	}

	private boolean canFillStorageWithXp() {
		return storageWrapper.getFluidHandler().map(fluidHandler -> fluidHandler.fill(ModFluids.EXPERIENCE_TAG, 1, ModFluids.XP_STILL.get(), IFluidHandler.FluidAction.SIMULATE) > 0).orElse(false);
	}

	private int pickupXpOrbs(@Nullable LivingEntity entity, Level level, BlockPos pos) {
		List<ExperienceOrb> xpEntities = level.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(upgradeItem.getRadius()), e -> true);
		if (xpEntities.isEmpty()) {
			return COOLDOWN_TICKS;
		}

		int cooldown = COOLDOWN_TICKS;
		for (ExperienceOrb xpOrb : xpEntities) {
			if (xpOrb.isAlive() && !canNotPickup(xpOrb, entity) && !tryToFillTank(xpOrb, entity, level)) {
				cooldown = FULL_COOLDOWN_TICKS;
				break;
			}
		}
		return cooldown;
	}

	private boolean tryToFillTank(ExperienceOrb xpOrb, @Nullable LivingEntity entity, Level level) {
		int amountToTransfer = XpHelper.experienceToLiquid(xpOrb.getValue());

		return storageWrapper.getFluidHandler().map(fluidHandler -> {
			int amountAdded = fluidHandler.fill(ModFluids.EXPERIENCE_TAG, amountToTransfer, ModFluids.XP_STILL.get(), IFluidHandler.FluidAction.EXECUTE);

			if (amountAdded > 0) {
				Vec3 pos = xpOrb.position();
				xpOrb.value = 0;
				xpOrb.discard();

				Player player = (Player) entity;

				if (player != null) {
					playXpPickupSound(level, player);
				}

				if (amountToTransfer > amountAdded) {
					level.addFreshEntity(new ExperienceOrb(level, pos.x(), pos.y(), pos.z(), (int) XpHelper.liquidToExperience(amountToTransfer - amountAdded)));
				}
				return true;
			}
			return false;
		}).orElse(false);
	}

	private int pickupItems(@Nullable LivingEntity entity, Level level, BlockPos pos) {
		List<ItemEntity> itemEntities = level.getEntities(EntityType.ITEM, new AABB(pos).inflate(upgradeItem.getRadius()), e -> true);
		if (itemEntities.isEmpty()) {
			return COOLDOWN_TICKS;
		}

		int cooldown = COOLDOWN_TICKS;

		Player player = (Player) entity;

		for (ItemEntity itemEntity : itemEntities) {
			if (!itemEntity.isAlive() || !filterLogic.matchesFilter(itemEntity.getItem()) || canNotPickup(itemEntity, entity)) {
				continue;
			}
			if (tryToInsertItem(player, itemEntity)) {
				if (player != null) {
					playItemPickupSound(level, player);
				}
			} else {
				cooldown = FULL_COOLDOWN_TICKS;
			}
		}
		return cooldown;
	}

	@SuppressWarnings("squid:S1764") // this actually isn't a case of identical values being used as both side are random float value thus -1 to 1 as a result
	private static void playItemPickupSound(Level level, @Nonnull Player player) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, (level.random.nextFloat() - level.random.nextFloat()) * 1.4F + 2.0F);
	}

	@SuppressWarnings("squid:S1764") // this actually isn't a case of identical values being used as both side are random float value thus -1 to 1 as a result
	private static void playXpPickupSound(Level level, @Nonnull Player player) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1F, (level.random.nextFloat() - level.random.nextFloat()) * 0.35F + 0.9F);
	}

	private boolean isBlockedBySomething(Entity entity) {
		for (IMagnetPreventionChecker checker : magnetCheckers) {
			if (checker.isBlocked(entity)) {
				return true;
			}
		}
		return false;
	}

	private boolean canNotPickup(Entity entity, @Nullable LivingEntity player) {
		if (isBlockedBySomething(entity)) {
			return true;
		}

		CompoundTag data = entity.getPersistentData();
		return player != null ? data.contains(PREVENT_REMOTE_MOVEMENT) : data.contains(PREVENT_REMOTE_MOVEMENT) && !data.contains(ALLOW_MACHINE_MOVEMENT);
	}

	private boolean tryToInsertItem(@Nullable Player player, ItemEntity itemEntity) {
		ItemStack stack = itemEntity.getItem();
		IItemHandlerSimpleInserter inventory = storageWrapper.getInventoryForUpgradeProcessing();
		ItemStack remaining = inventory.insertItem(stack, true);
		boolean insertedSomething = false;
		if (remaining.getCount() != stack.getCount()) {
			insertedSomething = true;
			int originalCount = stack.getCount();
			Item item = stack.getItem();
			remaining = inventory.insertItem(stack, false);
			itemEntity.setItem(remaining);
			if (player != null) {
				player.awardStat(Stats.ITEM_PICKED_UP.get(item), originalCount - remaining.getCount());
			}
		}
		return insertedSomething;
	}

	public void setPickupItems(boolean pickupItems) {
		upgrade.set(ModCoreDataComponents.PICKUP_ITEMS, pickupItems);
		save();
	}

	public boolean shouldPickupItems() {
		return upgrade.getOrDefault(ModCoreDataComponents.PICKUP_ITEMS, true);
	}

	public void setPickupXp(boolean pickupXp) {
		upgrade.set(ModCoreDataComponents.PICKUP_XP, pickupXp);
		save();
	}

	public boolean shouldPickupXp() {
		return upgrade.getOrDefault(ModCoreDataComponents.PICKUP_XP, true);
	}
}
