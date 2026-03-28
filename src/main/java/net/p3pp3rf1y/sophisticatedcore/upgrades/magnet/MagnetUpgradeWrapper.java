package net.p3pp3rf1y.sophisticatedcore.upgrades.magnet;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import net.p3pp3rf1y.sophisticatedcore.util.XpHelper;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class MagnetUpgradeWrapper extends UpgradeWrapperBase<MagnetUpgradeWrapper, MagnetUpgradeItem>
		implements IContentsFilteredUpgrade, ITickableUpgrade, IPickupResponseUpgrade {
	private static final String PREVENT_REMOTE_MOVEMENT = "PreventRemoteMovement";
	private static final String ALLOW_MACHINE_MOVEMENT = "AllowMachineRemoteMovement";
	private static final int COOLDOWN_TICKS = 10;

	private static long nextTickTime = Long.MIN_VALUE;

	public static void globalPostTick(LevelTickEvent.Pre event) {
		if (event.getLevel().isClientSide()) {
			return;
		}

		long gameTime = event.getLevel().getGameTime();
		if (gameTime > nextTickTime) {
			nextTickTime = gameTime + COOLDOWN_TICKS;
		}
	}

	public static void onWorldUnload(LevelEvent.Unload evt) {
		nextTickTime = Long.MIN_VALUE;
	}

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

	private boolean isInCooldown(Level level, @Nullable Entity entity) {
		if (!(entity instanceof Player)) {
			return super.isInCooldown(level);
		}

		return nextTickTime > level.getGameTime();
	}

	@Override
	public ContentsFilterLogic getFilterLogic() {
		return filterLogic;
	}

	@Override
	public int pickup(Level level, ItemResource resource, int amount, TransactionContext tx) {
		if (!shouldPickupItems() || !filterLogic.matchesFilter(resource)) {
			return 0;
		}

		return storageWrapper.getInventoryForUpgradeProcessing().insert(resource, amount, tx);
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (isInCooldown(level, entity)) {
			return;
		}

		int cooldown = shouldPickupItems() ? pickupItems(entity, level, pos) : FULL_COOLDOWN_TICKS;

		if (shouldPickupXp() && canFillStorageWithXp()) {
			cooldown = Math.min(cooldown, pickupXpOrbs(entity, level, pos));
		}

		setCooldown(level, cooldown);
	}

	private boolean canFillStorageWithXp() {
		return storageWrapper.getFluidHandler().map(fluidHandler -> {
			try (Transaction tx = Transaction.openRoot()) {
				return fluidHandler.insert(ModFluids.EXPERIENCE_TAG, 1, ModFluids.XP_STILL.get(), tx) > 0;
			}
		}).orElse(false);
	}

	private int pickupXpOrbs(@Nullable Entity entity, Level level, BlockPos pos) {
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

	private boolean tryToFillTank(ExperienceOrb xpOrb, @Nullable Entity entity, Level level) {
		int amountToTransfer = XpHelper.experienceToLiquid(xpOrb.getValue());

		return storageWrapper.getFluidHandler().map(fluidHandler -> {
			int amountAdded;
			try (Transaction tx = Transaction.openRoot()) {
				amountAdded = fluidHandler.insert(ModFluids.EXPERIENCE_TAG, amountToTransfer, ModFluids.XP_STILL.get(), tx);
				if (amountAdded > 0) {
					tx.commit();
				}
			}

			if (amountAdded > 0) {
				Vec3 pos = xpOrb.position();
				xpOrb.setValue(0);
				xpOrb.discard();

				Player player = entity instanceof Player ? (Player) entity : null;

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

	private int pickupItems(@Nullable Entity entity, Level level, BlockPos pos) {
		List<ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(upgradeItem.getRadius()), e -> true);
		if (itemEntities.isEmpty()) {
			return COOLDOWN_TICKS;
		}

		Player player = entity instanceof Player ? (Player) entity : null;

		int cooldown = FULL_COOLDOWN_TICKS;
		for (ItemEntity itemEntity : itemEntities) {
			if (!itemEntity.isAlive() || itemEntity.pickupDelay == ItemEntity.INFINITE_PICKUP_DELAY || !filterLogic.matchesFilter(itemEntity.getItem()) || canNotPickup(itemEntity, entity)) {
				continue;
			}
			if (tryToInsertItem(player, itemEntity)) {
				if (player != null) {
					playItemPickupSound(level, player);
				}
				cooldown = COOLDOWN_TICKS;
			}
		}
		return cooldown;
	}

	@SuppressWarnings("squid:S1764")
	// this actually isn't a case of identical values being used as both side are random float value thus -1 to 1 as a result
	private static void playItemPickupSound(Level level, @Nonnull Player player) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 1.4F + 2.0F);
	}

	@SuppressWarnings("squid:S1764")
	// this actually isn't a case of identical values being used as both side are random float value thus -1 to 1 as a result
	private static void playXpPickupSound(Level level, @Nonnull Player player) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1F, (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.35F + 0.9F);
	}

	private boolean isBlockedBySomething(Entity entity) {
		for (IMagnetPreventionChecker checker : magnetCheckers) {
			if (checker.isBlocked(entity)) {
				return true;
			}
		}
		return false;
	}

	private boolean canNotPickup(Entity pickedUpEntity, @Nullable Entity entity) {
		if (isBlockedBySomething(pickedUpEntity)) {
			return true;
		}

		CompoundTag data = pickedUpEntity.getPersistentData();
		return entity instanceof Player ? data.contains(PREVENT_REMOTE_MOVEMENT) : data.contains(PREVENT_REMOTE_MOVEMENT) && !data.contains(ALLOW_MACHINE_MOVEMENT);
	}

	private boolean tryToInsertItem(@Nullable Player player, ItemEntity itemEntity) {
		ItemStack stack = itemEntity.getItem();
		ItemResource resource = ItemResource.of(stack);

		int inserted;
		try (Transaction tx = Transaction.openRoot()) {
			inserted = storageWrapper.getInventoryForUpgradeProcessing().insert(resource, stack.getCount(), tx);
			if (inserted == 0) {
				return false;
			}
			tx.commit();
		}
		itemEntity.setItem(resource.toStack(stack.getCount() - inserted));
		if (player != null) {
			player.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), stack.getCount() - inserted);
		}
		return true;
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
