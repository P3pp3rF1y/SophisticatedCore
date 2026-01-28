package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

public interface IInsertResponseUpgrade {
	default ItemStack onBeforeInsert(InventoryHandler inventoryHandler, int slot, ItemStack stack, boolean simulate) {
		return stack;
	}

	default ItemStack onBeforeInsert(InventoryHandler inventoryHandler, ItemStack stack, boolean simulate) {
		return stack;
	}

	default void onAfterInsert(IItemHandlerSimpleInserter inventoryHandler, int slot) {
		//noop by default
	}
}
