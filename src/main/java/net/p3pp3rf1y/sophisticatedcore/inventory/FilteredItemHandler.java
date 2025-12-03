package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class FilteredItemHandler<T extends ResourceHandler<ItemResource>> implements ResourceHandler<ItemResource> {
	protected final T inventoryHandler;
	protected final List<FilterLogic> inputFilters;
	private final List<FilterLogic> outputFilters;

	public FilteredItemHandler(T inventoryHandler, List<FilterLogic> inputFilters, List<FilterLogic> outputFilters) {
		this.inventoryHandler = inventoryHandler;
		this.inputFilters = inputFilters;
		this.outputFilters = outputFilters;
	}

	@Override
	public int size() {
		return inventoryHandler.size();
	}

	@Override
	public ItemResource getResource(int i) {
		return inventoryHandler.getResource(i);
	}

	@Override
	public long getAmountAsLong(int i) {
		return inventoryHandler.getAmountAsLong(i);
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transactionContext) {
		if (matchesFilters(resource, inputFilters)) {
			return inventoryHandler.insert(index, resource, amount, transactionContext);
		}
		return 0;
	}

	@Override
	public int insert(ItemResource resource, int amount, TransactionContext transaction) {
		if (matchesFilters(resource, inputFilters)) {
			return inventoryHandler.insert(resource, amount, transaction);
		}
		return 0;
	}

	@Override
	public int extract(ItemResource resource, int amount, TransactionContext transaction) {
		if (matchesFilters(resource, outputFilters)) {
			return inventoryHandler.extract(resource, amount, transaction);
		}
		return 0;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transactionContext) {
		if (matchesFilters(resource, outputFilters)) {
			return inventoryHandler.extract(index, resource, amount, transactionContext);
		}
		return 0;
	}

	protected boolean matchesFilters(ItemResource resource, List<FilterLogic> filters) {
		if (filters.isEmpty()) {
			return true;
		}

		boolean matchAll = shouldMatchAllFilters(filters);

		for (FilterLogic filter : filters) {
			if (matchAll && !filter.matchesFilter(resource)) {
				return false;
			} else if (!matchAll && filter.matchesFilter(resource)) {
				return true;
			}
		}
		return matchAll;
	}

	protected boolean shouldMatchAllFilters(List<FilterLogic> filters) {
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
	public long getCapacityAsLong(int i, ItemResource resource) {
		return inventoryHandler.getCapacityAsLong(i, resource);
	}

	@Override
	public boolean isValid(int i, ItemResource resource) {
		if (matchesFilters(resource, inputFilters)) {
			return inventoryHandler.isValid(i, resource);
		}
		return false;
	}

	public static class Modifiable extends FilteredItemHandler<ITrackedContentsItemResourceHandler> implements ITrackedContentsItemResourceHandler {
		public Modifiable(ITrackedContentsItemResourceHandler inventoryHandler, List<FilterLogic> inputFilters, List<FilterLogic> outputFilters) {
			super(inventoryHandler, inputFilters, outputFilters);
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			inventoryHandler.setStackInSlot(slot, stack);
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transactionContext) {
			return super.insert(index, resource, amount, transactionContext);
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return inventoryHandler.getStackInSlot(slot);
		}

		@Override
		public int insert(ItemResource resource, int amount, TransactionContext transaction) {
			return super.insert(resource, amount, transaction);
		}

		protected boolean matchesFilters(ItemStack stack, List<FilterLogic> filters) {
			if (filters.isEmpty()) {
				return true;
			}

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
		public int getInternalSlotLimit(int slot) {
			return inventoryHandler.getInternalSlotLimit(slot);
		}
	}
}
