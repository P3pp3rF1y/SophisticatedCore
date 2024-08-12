package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public interface IUpgradeItem<T extends IUpgradeWrapper> {
	UpgradeType<T> getType();

	default UpgradeSlotChangeResult canAddUpgradeTo(IStorageWrapper storageWrapper, ItemStack upgradeStack, boolean firstLevelStorage, boolean isClientSide) {
		UpgradeSlotChangeResult result = checkUpgradePerStorageTypeLimit(storageWrapper);

		if (!result.successful()) {
			return result;
		}

		result = checkForConflictingUpgrades(storageWrapper, getUpgradeConflicts(), -1);
		if (!result.successful()) {
			return result;
		}

		return checkExtraInsertConditions(upgradeStack, storageWrapper, isClientSide);
	}

	private UpgradeSlotChangeResult checkForConflictingUpgrades(IStorageWrapper storageWrapper, List<UpgradeConflictDefinition> upgradeConflicts, int excludeUpgradeSlot) {
		for (UpgradeConflictDefinition conflictDefinition : upgradeConflicts) {
			AtomicInteger conflictingCount = new AtomicInteger(0);
			Set<Integer> conflictingSlots = new HashSet<>();
			InventoryHelper.iterate(storageWrapper.getUpgradeHandler(), (slot, stack) -> {
				if (slot != excludeUpgradeSlot && conflictDefinition.isConflictingItem.test(stack.getItem())) {
					conflictingCount.incrementAndGet();
					conflictingSlots.add(slot);
				}
			});

			if (conflictingCount.get() > conflictDefinition.maxConflictingAllowed) {
				return UpgradeSlotChangeResult.fail(conflictDefinition.errorMessage, conflictingSlots, Set.of(), Set.of());
			}
		}
		return UpgradeSlotChangeResult.success();
	}

	List<UpgradeConflictDefinition> getUpgradeConflicts();

	private UpgradeSlotChangeResult checkUpgradePerStorageTypeLimit(IStorageWrapper storageWrapper) {
		int upgradesPerStorage = getUpgradesPerStorage(storageWrapper.getStorageType());
		int upgradesInGroupPerStorage = getUpgradesInGroupPerStorage(storageWrapper.getStorageType());

		if (upgradesPerStorage == Integer.MAX_VALUE && upgradesInGroupPerStorage == Integer.MAX_VALUE) {
			return UpgradeSlotChangeResult.success();
		}

		if (upgradesPerStorage == 0) {
			return UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("add.upgrade_not_allowed", getName(), storageWrapper.getDisplayName()), Set.of(), Set.of(), Set.of());
		} else if (upgradesInGroupPerStorage == 0) {
			return UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("add.upgrade_not_allowed", Component.translatable(getUpgradeGroup().translName()), storageWrapper.getDisplayName()), Set.of(), Set.of(), Set.of());
		}

		Set<Integer> slotsWithUpgrade = new HashSet<>();
		InventoryHelper.iterate(storageWrapper.getUpgradeHandler(), (slot, stack) -> {
			if (stack.getItem() == this) {
				slotsWithUpgrade.add(slot);
			}
		});

		if (slotsWithUpgrade.size() >= upgradesPerStorage) {
			return UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("add.only_x_upgrades_allowed", upgradesPerStorage, getName(), storageWrapper.getDisplayName(), upgradesPerStorage), slotsWithUpgrade, Set.of(), Set.of());
		}

		Set<Integer> slotsWithUgradeGroup = new HashSet<>();
		InventoryHelper.iterate(storageWrapper.getUpgradeHandler(), (slot, stack) -> {
			if (stack.getItem() instanceof IUpgradeItem<?> upgradeItem && upgradeItem.getUpgradeGroup() == getUpgradeGroup()) {
				slotsWithUgradeGroup.add(slot);
			}
		});

		if (slotsWithUgradeGroup.size() >= upgradesInGroupPerStorage) {
			return UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("add.only_x_upgrades_allowed", upgradesInGroupPerStorage, Component.translatable(getUpgradeGroup().translName()), storageWrapper.getDisplayName()), slotsWithUgradeGroup, Set.of(), Set.of());
		}

		return UpgradeSlotChangeResult.success();
	}

	default UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storageWrapper, boolean isClientSide) {
		return UpgradeSlotChangeResult.success();
	}

	default UpgradeSlotChangeResult canSwapUpgradeFor(ItemStack upgradeStackToPut, int upgradeSlot, IStorageWrapper storageWrapper, boolean isClientSide) {
		if (upgradeStackToPut.getItem() == this) {
			return UpgradeSlotChangeResult.success();
		}

		if (upgradeStackToPut.getItem() instanceof IUpgradeItem<?> upgradeToPut) {
			int upgradesPerStorage = upgradeToPut.getUpgradesPerStorage(storageWrapper.getStorageType());
			int upgradesInGroupPerStorage = upgradeToPut.getUpgradesInGroupPerStorage(storageWrapper.getStorageType());

			if (upgradesPerStorage < upgradesInGroupPerStorage) {
				UpgradeSlotChangeResult result = upgradeToPut.checkUpgradePerStorageTypeLimit(storageWrapper);
				if (!result.successful()) {
					return result;
				}
			} else {
				if (upgradeToPut.getUpgradeGroup() != getUpgradeGroup()) {
					UpgradeSlotChangeResult result = upgradeToPut.checkUpgradePerStorageTypeLimit(storageWrapper);
					if (!result.successful()) {
						return result;
					}
				}
			}

			UpgradeSlotChangeResult result = checkForConflictingUpgrades(storageWrapper, upgradeToPut.getUpgradeConflicts(), upgradeSlot);
			if (!result.successful()) {
				return result;
			}
			return upgradeToPut.checkExtraInsertConditions(upgradeStackToPut, storageWrapper, isClientSide);
		}

		return UpgradeSlotChangeResult.success();
	}

	default UpgradeSlotChangeResult checkExtraInsertConditions(ItemStack upgradeStack, IStorageWrapper storageWrapper, boolean isClientSide) {
		return UpgradeSlotChangeResult.success();
	}

	default int getInventoryColumnsTaken() {
		return 0;
	}

	default ItemStack getCleanedUpgradeStack(ItemStack upgradeStack) {
		return upgradeStack;
	}

	int getUpgradesPerStorage(String storageType);

	int getUpgradesInGroupPerStorage(String storageType);

	default UpgradeGroup getUpgradeGroup() {
		return UpgradeGroup.NONE;
	}

	Component getName();

	record UpgradeConflictDefinition(Predicate<Item> isConflictingItem, int maxConflictingAllowed,
									 Component errorMessage) {
	}
}
