package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;

public interface IOverflowResponseUpgrade {

	FilterLogic getFilterLogic();

	boolean worksInGui();

	ItemStack onSlotOverflow(ItemStack stack);

	int onSlotOverflow(ItemResource resource, int amount);

	int onStorageOverflow(ItemResource resource, int amount);

	boolean stackMatchesFilter(ItemStack stack);

	boolean matchesFilter(ItemResource resource);

	default boolean hasSlotOverflowMatch(InventoryHandler inventoryHandler, ItemStack stack) {
		return inventoryHandler.getSlotTracker().getFullStacks().contains(ItemStackKey.of(stack));
	}

	default boolean hasSlotOverflowMatch(InventoryHandler inventoryHandler, ItemResource resource) {
		return inventoryHandler.getSlotTracker().getFullStacks().contains(ItemStackKey.of(resource));
	}
}
