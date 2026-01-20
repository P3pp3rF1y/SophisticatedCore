package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

public interface IExtractResponseUpgrade {
	void onAfterExtract(InventoryHandler inventoryHandler, int slot, ItemResource originalResource);
}
