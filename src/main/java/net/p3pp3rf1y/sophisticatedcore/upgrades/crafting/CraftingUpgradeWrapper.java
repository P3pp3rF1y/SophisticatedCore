package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;

import java.util.function.Consumer;

public class CraftingUpgradeWrapper extends UpgradeWrapperBase<CraftingUpgradeWrapper, CraftingUpgradeItem> {
	private final ComponentItemHandler inventory;

	public CraftingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);

		inventory = new ComponentItemHandler(upgrade, DataComponents.CONTAINER, 9) {
			@Override
			protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {
				super.onContentsChanged(slot, oldStack, newStack);
				save();
			}

			@Override
			public int getSlotLimit(int slot) {
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return true;
			}
		};
	}

	public ComponentItemHandler getInventory() {
		return inventory;
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	public boolean shouldShiftClickIntoStorage() {
		return upgrade.getOrDefault(ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE, true);
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		upgrade.set(ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE, shiftClickIntoStorage);
		save();
	}
}
