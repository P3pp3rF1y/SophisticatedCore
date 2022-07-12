package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class FilteredItemHandler<T extends IItemHandler> implements IItemHandler {
	protected final T inventoryHandler;
	protected final List<FilterLogic> inputFilters;
	private final List<FilterLogic> outputFilters;

	public FilteredItemHandler(T inventoryHandler, List<FilterLogic> inputFilters, List<FilterLogic> outputFilters) {
		this.inventoryHandler = inventoryHandler;
		this.inputFilters = inputFilters;
		this.outputFilters = outputFilters;
	}

	@Override
	public int getSlots() {
		return inventoryHandler.getSlots();
	}

	@Nonnull
	@Override
	public ItemStack getStackInSlot(int slot) {
		return inventoryHandler.getStackInSlot(slot);
	}

	@Nonnull
	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (inputFilters.isEmpty()) {
			return inventoryHandler.insertItem(slot, stack, simulate);
		}

		for (FilterLogic filter : inputFilters) {
			if (filter.matchesFilter(stack)) {
				return inventoryHandler.insertItem(slot, stack, simulate);
			}
		}
		return stack;
	}

	@Nonnull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (outputFilters.isEmpty()) {
			return inventoryHandler.extractItem(slot, amount, simulate);
		}

		for (FilterLogic filter : outputFilters) {
			if (filter.matchesFilter(getStackInSlot(slot))) {
				return inventoryHandler.extractItem(slot, amount, simulate);
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public int getSlotLimit(int slot) {
		return inventoryHandler.getSlotLimit(slot);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return inventoryHandler.isItemValid(slot, stack);
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

			for (FilterLogic filter : inputFilters) {
				if (filter.matchesFilter(stack)) {
					return inventoryHandler.insertItem(stack, simulate);
				}
			}
			return stack;
		}

		@Override
		public Set<ItemStackKey> getTrackedStacks() {
			Set<ItemStackKey> ret = new HashSet<>();

			inventoryHandler.getTrackedStacks().forEach(ts -> {
				if (inputFiltersMatchStack(ts.stack())) {
					ret.add(ts);
				}
			});

			return ret;
		}

		private boolean inputFiltersMatchStack(ItemStack stack) {
			for (FilterLogic filter : inputFilters) {
				if (filter.matchesFilter(stack)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void registerTrackingListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot) {
			inventoryHandler.registerTrackingListeners(
					isk -> {
						if (inputFiltersMatchStack(isk.stack())) {
							onAddStackKey.accept(isk);
						}
					},
					isk -> {
						if (inputFiltersMatchStack(isk.stack())) {
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
	}
}
