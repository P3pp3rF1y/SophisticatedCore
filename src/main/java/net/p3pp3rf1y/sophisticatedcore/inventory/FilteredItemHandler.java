package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class FilteredItemHandler<T extends IItemHandler> implements IItemHandler {
	protected final T inventoryHandler;
	protected final List<FilterLogic> inputFilters;
	protected final List<FilterLogic> outputFilters;

	public FilteredItemHandler(T inventoryHandler, List<FilterLogic> inputFilters, List<FilterLogic> outputFilters) {
		this.inventoryHandler = inventoryHandler;
		this.inputFilters = inputFilters;
		this.outputFilters = outputFilters;
	}

	@Override
	public int getSlots() {
		return inventoryHandler.getSlots();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return inventoryHandler.getStackInSlot(slot);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (inputFilters.isEmpty()) {
			return inventoryHandler.insertItem(slot, stack, simulate);
		}

		if (matchesFilters(stack, inputFilters)) {
			return inventoryHandler.insertItem(slot, stack, simulate);
		}

		return stack;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (outputFilters.isEmpty()) {
			return inventoryHandler.extractItem(slot, amount, simulate);
		}

		if (matchesFilters(getStackInSlot(slot), outputFilters)) {
			return inventoryHandler.extractItem(slot, amount, simulate);
		}

		return ItemStack.EMPTY;
	}

	protected boolean matchesFilters(ItemStack stack, List<FilterLogic> filters) {
		boolean matchAll = shouldMatchAllFilters(filters);

		for (FilterLogic filter : filters) {
			if (matchAll && !filter.matchesFilter(stack)) {
				return false;
			} else if (!matchAll && filter.matchesFilter(stack)) {
				return true;
			}
		}
		return matchAll;
	}

	private boolean shouldMatchAllFilters(List<FilterLogic> filters) {
		if (filters.size() < 2) {
			return false;
		}

		for (FilterLogic filter : filters) {
			if (!filter.isAllowList()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getSlotLimit(int slot) {
		return inventoryHandler.getSlotLimit(slot);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		if (matchesFilters(stack, inputFilters)) {
			return inventoryHandler.isItemValid(slot, stack);
		}
		return false;
	}

	public static class Modifiable extends FilteredItemHandler<ITrackedContentsItemHandler> implements ITrackedContentsItemHandler {
		public Modifiable(ITrackedContentsItemHandler inventoryHandler, List<FilterLogic> inputFilters, List<FilterLogic> outputFilters) {
			super(inventoryHandler, inputFilters, outputFilters);
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			inventoryHandler.setStackInSlot(slot, stack);
		}

		@Override
		public ItemStack insertItem(ItemStack stack, boolean simulate) {
			if (inputFilters.isEmpty()) {
				return inventoryHandler.insertItem(stack, simulate);
			}

			if (matchesFilters(stack, inputFilters)) {
				return inventoryHandler.insertItem(stack, simulate);
			}

			return stack;
		}

		@Override
		public Set<ItemStackKey> getTrackedStacks() {
			Set<ItemStackKey> ret = new HashSet<>();

			inventoryHandler.getTrackedStacks().forEach(ts -> {
				if (matchesFilters(ts.stack(), inputFilters)) {
					ret.add(ts);
				}
			});

			return ret;
		}

		@Override
		public void registerTrackingListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot) {
			inventoryHandler.registerTrackingListeners(
					isk -> {
						if (matchesFilters(isk.stack(), inputFilters)) {
							onAddStackKey.accept(isk);
						}
					},
					isk -> {
						if (matchesFilters(isk.stack(), inputFilters)) {
							onRemoveStackKey.accept(isk);
						}
					},
					onAddFirstEmptySlot,
					onRemoveLastEmptySlot
			);
		}

		@Override
		public void unregisterStackKeyListeners() {
			inventoryHandler.unregisterStackKeyListeners();
		}

		@Override
		public boolean hasEmptySlots() {
			return inventoryHandler.hasEmptySlots();
		}

		@Override
		public boolean isInsertBlocked() {
			return inventoryHandler.isInsertBlocked();
		}

		@Override
		public ItemStack extractItem(ItemStack stack, boolean simulate) {
			if (outputFilters.isEmpty()) {
				return inventoryHandler.extractItem(stack, simulate);
			}

			if (matchesFilters(stack, outputFilters)) {
				return inventoryHandler.extractItem(stack, simulate);
			}

			return ItemStack.EMPTY;
		}
	}
}
