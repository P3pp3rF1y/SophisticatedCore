package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

public interface IOverflowResponseUpgrade {

	FilterLogic getFilterLogic();

	boolean worksInGui();

	ItemStack onSlotOverflow(ItemStack stack);

	int onSlotOverflow(ItemResource resource, int amount);

	int onStorageOverflow(ItemResource resource, int amount);

	boolean stackMatchesFilter(ItemStack stack);

	boolean matchesFilter(ItemResource resource);
}
