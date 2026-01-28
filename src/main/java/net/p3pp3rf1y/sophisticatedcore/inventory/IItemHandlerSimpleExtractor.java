package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.ItemStack;

public interface IItemHandlerSimpleExtractor {
	ItemStack extractItem(ItemStack stack, boolean simulate);
}
