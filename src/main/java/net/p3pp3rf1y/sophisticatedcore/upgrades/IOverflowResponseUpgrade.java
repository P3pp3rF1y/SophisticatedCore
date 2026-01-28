package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.world.item.ItemStack;

public interface IOverflowResponseUpgrade {

	FilterLogic getFilterLogic();

	boolean worksInGui();

	ItemStack onSlotOverflow(ItemStack stack);

	ItemStack onStorageOverflow(ItemStack stack);

	boolean stackMatchesFilter(ItemStack stack);
}
