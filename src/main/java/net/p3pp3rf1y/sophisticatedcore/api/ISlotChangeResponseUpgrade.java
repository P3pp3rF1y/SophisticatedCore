package net.p3pp3rf1y.sophisticatedcore.api;

import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

public interface ISlotChangeResponseUpgrade {
	void onSlotChange(InventoryHandler inventoryHandler, int slot);
}
