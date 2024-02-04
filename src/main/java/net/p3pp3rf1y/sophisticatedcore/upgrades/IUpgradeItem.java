package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.HashSet;
import java.util.Set;

public interface IUpgradeItem<T extends IUpgradeWrapper> {
	UpgradeType<T> getType();

	default UpgradeSlotChangeResult canAddUpgradeTo(IStorageWrapper storageWrapper, ItemStack upgradeStack, boolean firstLevelStorage, boolean isClientSide) {
		return checkUpgradePerStorageTypeLimit(storageWrapper);
	}

	private UpgradeSlotChangeResult checkUpgradePerStorageTypeLimit(IStorageWrapper storageWrapper) {
		int upgradesPerStorage = getUpgradesPerStorage(storageWrapper.getStorageType());
		int upgradesInGroupPerStorage = getUpgradesInGroupPerStorage(storageWrapper.getStorageType());

		if (upgradesPerStorage == Integer.MAX_VALUE && upgradesInGroupPerStorage == Integer.MAX_VALUE) {
			return new UpgradeSlotChangeResult.Success();
		}

		if (upgradesPerStorage == 0) {
			return new UpgradeSlotChangeResult.Fail(TranslationHelper.INSTANCE.translError("add.upgrade_not_allowed", getName(), storageWrapper.getDisplayName()), Set.of(), Set.of(), Set.of());
		} else if (upgradesInGroupPerStorage == 0) {
			return new UpgradeSlotChangeResult.Fail(TranslationHelper.INSTANCE.translError("add.upgrade_not_allowed", Component.translatable(getUpgradeGroup().translName()), storageWrapper.getDisplayName()), Set.of(), Set.of(), Set.of());
		}

		Set<Integer> slotsWithUpgrade = new HashSet<>();
		InventoryHelper.iterate(storageWrapper.getUpgradeHandler(), (slot, stack) -> {
			if (stack.getItem() == this) {
				slotsWithUpgrade.add(slot);
			}
		});

		if (slotsWithUpgrade.size() >= upgradesPerStorage) {
			return new UpgradeSlotChangeResult.Fail(TranslationHelper.INSTANCE.translError("add.only_x_upgrades_allowed", upgradesPerStorage, getName(), storageWrapper.getDisplayName(), upgradesPerStorage), slotsWithUpgrade, Set.of(), Set.of());
		}

		Set<Integer> slotsWithUgradeGroup = new HashSet<>();
		InventoryHelper.iterate(storageWrapper.getUpgradeHandler(), (slot, stack) -> {
			if (stack.getItem() instanceof IUpgradeItem<?> upgradeItem && upgradeItem.getUpgradeGroup() == getUpgradeGroup()) {
				slotsWithUgradeGroup.add(slot);
			}
		});

		if (slotsWithUgradeGroup.size() >= upgradesInGroupPerStorage) {
			return new UpgradeSlotChangeResult.Fail(TranslationHelper.INSTANCE.translError("add.only_x_upgrades_allowed", upgradesInGroupPerStorage, Component.translatable(getUpgradeGroup().translName()), storageWrapper.getDisplayName()), slotsWithUpgrade, Set.of(), Set.of());
		}

		return new UpgradeSlotChangeResult.Success();
	}

	default UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storageWrapper, boolean isClientSide) {
		return new UpgradeSlotChangeResult.Success();
	}

	default UpgradeSlotChangeResult canSwapUpgradeFor(ItemStack upgradeStackToPut, IStorageWrapper storageWrapper, boolean isClientSide) {
		if (upgradeStackToPut.getItem() instanceof IUpgradeItem<?> upgradeToPut) {
			int upgradesPerStorage = upgradeToPut.getUpgradesPerStorage(storageWrapper.getStorageType());
			int upgradesInGroupPerStorage = upgradeToPut.getUpgradesInGroupPerStorage(storageWrapper.getStorageType());
			if (upgradesPerStorage < upgradesInGroupPerStorage) {
				if (upgradeStackToPut.getItem() != this) {
					UpgradeSlotChangeResult result = upgradeToPut.checkUpgradePerStorageTypeLimit(storageWrapper);
					if (!result.isSuccessful()) {
						return result;
					}
				}
			} else {
				if (upgradeToPut.getUpgradeGroup() != getUpgradeGroup()) {
					UpgradeSlotChangeResult result = upgradeToPut.checkUpgradePerStorageTypeLimit(storageWrapper);
					if (!result.isSuccessful()) {
						return result;
					}
				}
			}
		}

		return canRemoveUpgradeFrom(storageWrapper, isClientSide);
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
}
