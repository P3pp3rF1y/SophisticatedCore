package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

public interface IInsertResponseUpgrade {
	int onBeforeInsert(InventoryHandler inventoryHandler, int slot, ItemResource resource, int amount);

	void onAfterInsert(InventoryHandler inventoryHandler, int slot, TransactionContext tx);
}
