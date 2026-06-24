package net.p3pp3rf1y.sophisticatedcore.upgrades.xppump;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ItemResourceHelper;
import net.p3pp3rf1y.sophisticatedcore.util.XpHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class XpPumpUpgradeWrapper extends UpgradeWrapperBase<XpPumpUpgradeWrapper, XpPumpUpgradeItem> implements ITickableUpgrade {
	private static final int DEFAULT_LEVEL = 10;
	private static final int COOLDOWN = 5;
	private static final int ALL_LEVELS = 10000;
	private static final int PLAYER_SEARCH_RANGE = 3;

	private final XpPumpUpgradeConfig xpPumpUpgradeConfig;

	protected XpPumpUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		xpPumpUpgradeConfig = upgradeItem.getXpPumpUpgradeConfig();
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (isInCooldown(level)) {
			return;
		}

		if (entity instanceof Player player) {
			interactWithPlayer(player);
			mendItems(player);
		} else {
			AABB searchBox = new AABB(pos).inflate(PLAYER_SEARCH_RANGE);
			for (Player player : level.players()) {
				if (searchBox.contains(player.getX(), player.getY(), player.getZ())) {
					interactWithPlayer(player);
					mendItems(player);
				}
			}
		}

		setCooldown(level, COOLDOWN);
	}

	private void mendItems(Player player) {
		if (!xpPumpUpgradeConfig.mendingOn.get() || !shouldMendItems()) {
			return;
		}

		getRandomDamagedItemWithMending(player).ifPresent(itemInfo -> {
			ItemResource resource = itemInfo.handler.getResource(itemInfo.slot);
			ItemStack itemStack = resource.toStack(itemInfo.handler.getAmountAsInt(itemInfo.slot));
			if (!itemStack.isEmpty() && itemStack.isDamaged() && itemStack.getXpRepairRatio() > 0) {
				float xpToTryDrain = Math.min(xpPumpUpgradeConfig.maxXpPointsPerMending.get(), itemStack.getDamageValue() / itemStack.getXpRepairRatio());
				if (xpToTryDrain > 0) {
					storageWrapper.getFluidHandler().ifPresent(fluidHandler -> {
						int extracted;
						try (Transaction tx = Transaction.openRoot()) {
							extracted = fluidHandler.extract(ModFluids.EXPERIENCE_TAG, XpHelper.experienceToLiquid(xpToTryDrain), tx, false);
							if (extracted == 0) {
								return;
							}
							tx.commit();
						}
						float xpDrained = XpHelper.liquidToExperience(extracted);
						int durabilityToRepair = (int) (xpDrained * itemStack.getXpRepairRatio());
						itemStack.setDamageValue(itemStack.getDamageValue() - durabilityToRepair);
						InventoryHelper.set(itemInfo.handler, itemInfo.slot, ItemResource.of(itemStack), itemStack.getCount());
					});
				}
			}
		});
	}

	private Optional<DamagedItemInfo> getRandomDamagedItemWithMending(Player player) {
		List<DamagedItemInfo> matchingItems = new ArrayList<>();
		List<ResourceHandler<ItemResource>> equipmentHandlers = InventoryHelper.getEquipmentItemHandlersFromPlayer(player);

		for (ResourceHandler<ItemResource> handler : equipmentHandlers) {
			for (int slot = 0; slot < handler.size(); slot++) {
				ItemResource resource = handler.getResource(slot);
				int amount = handler.getAmountAsInt(slot);
				if (ItemResourceHelper.isDamageable(resource)) {
					ItemStack stack = resource.toStack(amount);
					if (stack.getItem().isDamaged(stack)) {
						ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

						for (Object2IntMap.Entry<Holder<Enchantment>> enchantmentEntry : enchantments.entrySet()) {
							Holder<Enchantment> enchantmentHolder = enchantmentEntry.getKey();
							Enchantment enchantment = enchantmentHolder.value();

							if (enchantment.effects().has(EnchantmentEffectComponents.REPAIR_WITH_XP)) {
								matchingItems.add(new DamagedItemInfo(handler, slot));
							}
						}
					}
				}
			}
		}

		return Util.getRandomSafe(matchingItems, player.getRandom());
	}

	private record DamagedItemInfo(ResourceHandler<ItemResource> handler, int slot) {
	}

	private void interactWithPlayer(Player player) {
		storageWrapper.getFluidHandler().ifPresent(fluidHandler -> {
			int level = getLevel();
			AutomationDirection direction = getDirection();
			if (direction == AutomationDirection.OFF) {
				return;
			}

			if ((direction == AutomationDirection.INPUT || direction == AutomationDirection.KEEP)
					&& (level < player.experienceLevel || (level == player.experienceLevel && player.experienceProgress > 0))) {
				tryFillTankWithPlayerExperience(player, fluidHandler, level, false);
			} else if ((direction == AutomationDirection.OUTPUT || direction == AutomationDirection.KEEP) && level > player.experienceLevel) {
				tryGivePlayerExperienceFromTank(player, fluidHandler, level, false);
			}
		});
	}

	private void tryGivePlayerExperienceFromTank(Player player, IStorageFluidHandler fluidHandler, int stopAtLevel) {
		tryGivePlayerExperienceFromTank(player, fluidHandler, stopAtLevel, true);
	}

	private void tryGivePlayerExperienceFromTank(Player player, IStorageFluidHandler fluidHandler, int stopAtLevel, boolean ignoreInOutLimit) {
		int maxXpPointsToGive = XpHelper.getExperienceForLevel(stopAtLevel) - XpHelper.getPlayerTotalExperience(player);
		try (Transaction tx = Transaction.openRoot()) {
			int extracted = fluidHandler.extract(ModFluids.EXPERIENCE_TAG, XpHelper.experienceToLiquid(maxXpPointsToGive), tx, ignoreInOutLimit);
			if (extracted > 0) {
				tx.commit();
				player.giveExperiencePoints((int) XpHelper.liquidToExperience(extracted));
			}
		}
	}

	private void tryFillTankWithPlayerExperience(Player player, IStorageFluidHandler fluidHandler, int stopAtLevel) {
		tryFillTankWithPlayerExperience(player, fluidHandler, stopAtLevel, true);
	}

	private void tryFillTankWithPlayerExperience(Player player, IStorageFluidHandler fluidHandler, int stopAtLevel, boolean ignoreInOutLimit) {
		int maxXpPointsToTake = XpHelper.getPlayerTotalExperience(player) - XpHelper.getExperienceForLevel(stopAtLevel);
		try (Transaction tx = Transaction.openRoot()) {
			int filled = fluidHandler.insert(ModFluids.EXPERIENCE_TAG, XpHelper.experienceToLiquid(maxXpPointsToTake), ModFluids.XP_STILL.get(), tx,
					ignoreInOutLimit);
			if (filled > 0) {
				tx.commit();
				player.giveExperiencePoints((int) -XpHelper.liquidToExperience(filled));
			}
		}
	}

	public void takeLevelsFromPlayer(Player player) {
		storageWrapper.getFluidHandler()
				.ifPresent(fluidHandler -> tryFillTankWithPlayerExperience(player, fluidHandler, Math.max(player.experienceLevel - getLevelsToStore(), 0)));
	}

	public void takeAllExperienceFromPlayer(Player player) {
		storageWrapper.getFluidHandler().ifPresent(fluidHandler -> tryFillTankWithPlayerExperience(player, fluidHandler, 0));
	}

	public void giveLevelsToPlayer(Player player) {
		storageWrapper.getFluidHandler()
				.ifPresent(fluidHandler -> tryGivePlayerExperienceFromTank(player, fluidHandler, player.experienceLevel + getLevelsToTake()));
	}

	public void giveAllExperienceToPlayer(Player player) {
		storageWrapper.getFluidHandler().ifPresent(fluidHandler -> tryGivePlayerExperienceFromTank(player, fluidHandler, ALL_LEVELS));
	}

	public AutomationDirection getDirection() {
		return upgrade.getOrDefault(ModCoreDataComponents.AUTOMATION_DIRECTION, AutomationDirection.INPUT);
	}

	public void setDirection(AutomationDirection direction) {
		upgrade.set(ModCoreDataComponents.AUTOMATION_DIRECTION, direction);
		save();
	}

	public void setLevel(int level) {
		upgrade.set(ModCoreDataComponents.LEVEL, level);
		save();
	}

	public int getLevel() {
		return upgrade.getOrDefault(ModCoreDataComponents.LEVEL, DEFAULT_LEVEL);
	}

	public void setLevelsToStore(int levelsToStore) {
		upgrade.set(ModCoreDataComponents.LEVELS_TO_STORE, levelsToStore);
		save();
	}

	public int getLevelsToStore() {
		return upgrade.getOrDefault(ModCoreDataComponents.LEVELS_TO_STORE, 1);
	}

	public void setLevelsToTake(int levelsToTake) {
		upgrade.set(ModCoreDataComponents.LEVELS_TO_TAKE, levelsToTake);
		save();
	}

	public int getLevelsToTake() {
		return upgrade.getOrDefault(ModCoreDataComponents.LEVELS_TO_TAKE, 1);
	}

	public boolean shouldMendItems() {
		return upgrade.getOrDefault(ModCoreDataComponents.MEND_ITEMS, true);
	}

	public void setMendItems(boolean mendItems) {
		upgrade.set(ModCoreDataComponents.MEND_ITEMS, mendItems);
		save();
	}
}
