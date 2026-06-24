package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

public interface IInsertResponseUpgrade {
	default int onBeforeInsert(InventoryHandler inventoryHandler, ItemResource resource, int amount) {
		return 0;
	}

	default int onBeforeInsert(InventoryHandler inventoryHandler, int slot, ItemResource resource, int amount) {
		return 0;
	}

	default void onAfterInsert(InventoryHandler inventoryHandler, int slot, TransactionContext tx) {
		// noop by default
	}
}
