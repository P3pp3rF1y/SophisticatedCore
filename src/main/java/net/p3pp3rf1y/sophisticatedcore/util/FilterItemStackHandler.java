package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotStackAccessor;

import java.util.Objects;

public class FilterItemStackHandler extends ItemStacksResourceHandler implements ISlotStackAccessor {
	private boolean onlyEmptyFilters = true;

	public FilterItemStackHandler(int size) {
		super(size);
	}

	@Override
	protected int getCapacity(int index, ItemResource resource) {
		return 1;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		return 0;
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		return 0;
	}

	@Override
	protected void onContentsChanged(int slot, ItemStack previousContents) {
		super.onContentsChanged(slot, previousContents);

		updateEmptyFilters();
	}

	@Override
	public void deserialize(ValueInput input) {
		super.deserialize(input);
		updateEmptyFilters();
	}

	protected void updateEmptyFilters() {
		onlyEmptyFilters = InventoryHelper.iterate(this, (s, filter) -> filter.isEmpty(), () -> true, result -> !result);
	}

	public boolean hasOnlyEmptyFilters() {
		return onlyEmptyFilters;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		Objects.checkIndex(slot, size());
		return stacks.get(slot);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		Objects.checkIndex(slot, size());
		stacks.set(slot, stack);
		onContentsChanged(slot, stack);
	}
}
