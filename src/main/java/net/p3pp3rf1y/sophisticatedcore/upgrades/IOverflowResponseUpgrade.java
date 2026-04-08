package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;

public interface IOverflowResponseUpgrade {

	FilterLogic getFilterLogic();

	boolean worksInGui();

	ItemStack onSlotOverflow(ItemStack stack);

	ItemStack onStorageOverflow(ItemStack stack);

	boolean stackMatchesFilter(ItemStack stack);

	default boolean hasSlotOverflowMatch(InventoryHandler inventoryHandler, ItemStack stack) {
		return inventoryHandler.getSlotTracker().getFullStacks().contains(ItemStackKey.of(stack));
	}
}
