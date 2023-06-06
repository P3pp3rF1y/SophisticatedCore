package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;

public interface IUpgradeItem<T extends IUpgradeWrapper> {
	UpgradeType<T> getType();

	default UpgradeSlotChangeResult canAddUpgradeTo(IStorageWrapper storageWrapper, ItemStack upgradeStack, boolean firstLevelStorage, boolean isClientSide) {
		return new UpgradeSlotChangeResult.Success();
	}

	default UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storageWrapper, boolean isClientSide) {
		return new UpgradeSlotChangeResult.Success();
	}

	default UpgradeSlotChangeResult canSwapUpgradeFor(ItemStack upgradeStackToPut, IStorageWrapper storageWrapper, boolean isClientSide) {
		return canRemoveUpgradeFrom(storageWrapper, isClientSide);
	}

	default int getInventoryColumnsTaken() {
		return 0;
	}

	default ItemStack getCleanedUpgradeStack(ItemStack upgradeStack) {
		return upgradeStack;
	}
}
