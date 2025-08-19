package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class CachedFailedInsertInventoryHandler implements IItemHandlerModifiable {
	private final Supplier<IItemHandlerModifiable> wrappedHandlerGetter;
	private final LongSupplier timeSupplier;
	private long currentCacheTime = 0;
	private final Set<ItemStack> failedInsertStacks = new HashSet<>();

	public CachedFailedInsertInventoryHandler(Supplier<IItemHandlerModifiable> wrappedHandlerGetter, LongSupplier timeSupplier) {
		this.wrappedHandlerGetter = wrappedHandlerGetter;
		this.timeSupplier = timeSupplier;
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		wrappedHandlerGetter.get().setStackInSlot(slot, stack);
	}

	@Override
	public int getSlots() {
		return wrappedHandlerGetter.get().getSlots();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return wrappedHandlerGetter.get().getStackInSlot(slot);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (simulate) {
			return wrappedHandlerGetter.get().insertItem(slot, stack, true);
		}

		if (currentCacheTime != timeSupplier.getAsLong()) {
			failedInsertStacks.clear();
			currentCacheTime = timeSupplier.getAsLong();
		}

		if (failedInsertStacks.contains(stack)) {
			return stack;
		}

		ItemStack result = wrappedHandlerGetter.get().insertItem(slot, stack, false);

		if (result == stack) {
			failedInsertStacks.add(stack); //only working with stack references because this logic is meant to handle the case where something tries to insert the same stack number of slots times
		}

		return result;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return wrappedHandlerGetter.get().extractItem(slot, amount, simulate);
	}

	@Override
	public int getSlotLimit(int slot) {
		return wrappedHandlerGetter.get().getSlotLimit(slot);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return wrappedHandlerGetter.get().isItemValid(slot, stack);
	}
}
