package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.function.LongSupplier;

public class CachedFailedInsertInventoryHandler implements IItemHandlerModifiable {
	private final IItemHandlerModifiable wrapped;
	private final LongSupplier timeSupplier;
	private long currentCacheTime = 0;
	private final Set<Integer> failedInsertStackHashes = new HashSet<>();

	public CachedFailedInsertInventoryHandler(IItemHandlerModifiable wrapped, LongSupplier timeSupplier) {
		this.wrapped = wrapped;
		this.timeSupplier = timeSupplier;
	}

	@Override
	public void setStackInSlot(int slot, @NotNull ItemStack stack) {
		wrapped.setStackInSlot(slot, stack);
	}

	@Override
	public int getSlots() {
		return wrapped.getSlots();
	}

	@NotNull
	@Override
	public ItemStack getStackInSlot(int slot) {
		return wrapped.getStackInSlot(slot);
	}

	@NotNull
	@Override
	public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		if (currentCacheTime != timeSupplier.getAsLong()) {
			failedInsertStackHashes.clear();
			currentCacheTime = timeSupplier.getAsLong();
		}

		boolean hashCalculated = false;
		int stackHash = 0;
		if (!failedInsertStackHashes.isEmpty()) {
			stackHash = ItemStackKey.getHashCode(stack);
			hashCalculated = true;
			if (failedInsertStackHashes.contains(stackHash)) {
				return stack;
			}
		}

		ItemStack result = wrapped.insertItem(slot, stack, simulate);

		if (result == stack) {
			if (!hashCalculated) {
				stackHash = ItemStackKey.getHashCode(stack);
			}
			failedInsertStackHashes.add(stackHash);
		}

		return result;
	}

	@NotNull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return wrapped.extractItem(slot, amount, simulate);
	}

	@Override
	public int getSlotLimit(int slot) {
		return wrapped.getSlotLimit(slot);
	}

	@Override
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return wrapped.isItemValid(slot, stack);
	}
}
