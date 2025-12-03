package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.ItemStack;

public interface ISlotStackAccessor {
	ItemStack getStackInSlot(int slot);

	void setStackInSlot(int slot, ItemStack stack);
}
