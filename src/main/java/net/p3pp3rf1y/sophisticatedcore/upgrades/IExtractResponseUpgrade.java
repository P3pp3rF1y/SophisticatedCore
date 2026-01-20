package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;

public interface IExtractResponseUpgrade {
	void onAfterExtract(IItemHandlerSimpleInserter inventoryHandler, int slot, ItemStack originalContents);
}
