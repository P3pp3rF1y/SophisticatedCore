package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ReiSlotAccessor implements SlotAccessor {
	public static SlotAccessor fromSlot(Slot slot) {
		return new ReiSlotAccessor(slot);
	}

	protected Slot slot;

	public ReiSlotAccessor(Slot slot) {
		this.slot = slot;
	}

	@Override
	public ItemStack getItemStack() {
		return slot.getItem();
	}

	@Override
	public void setItemStack(ItemStack stack) {
		this.slot.set(stack);
	}

	@Override
	public ItemStack takeStack(int amount) {
		return slot.remove(amount);
	}

	public int getIndex() {
		return slot.index;
	}
}