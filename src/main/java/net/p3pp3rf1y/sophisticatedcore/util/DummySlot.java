package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DummySlot extends Slot {
	public static final DummySlot INSTANCE = new DummySlot();

	private DummySlot() {
		super(new SimpleContainer(0), -1, 0, 0);
	}

	@Override
	public ItemStack getItem() {
		return ItemStack.EMPTY;
	}

	@Override
	public void set(ItemStack p_40240_) {
		//noop
	}

	@Override
	public void setChanged() {
		//noop
	}
}
