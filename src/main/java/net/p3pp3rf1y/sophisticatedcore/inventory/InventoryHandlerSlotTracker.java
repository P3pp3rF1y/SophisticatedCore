package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class InventoryHandlerSlotTracker implements ISlotTracker {
	private final Map<ItemStackKey, Set<Integer>> fullStackSlots = new HashMap<>();
	private final Map<Integer, ItemStackKey> fullSlotStacks = new HashMap<>();
	private final Map<ItemStackKey, Set<Integer>> partiallyFilledStackSlots = new HashMap<>();
	private final Map<Integer, ItemStackKey> partiallyFilledSlotStacks = new HashMap<>();
	private final Set<Integer> emptySlots = new TreeSet<>();
	private final MemorySettingsCategory memorySettings;
	private Consumer<ItemStackKey> onAddStackKey = sk -> {};
	private Consumer<ItemStackKey> onRemoveStackKey = sk -> {};

	private Runnable onAddFirstEmptySlot = () -> {};
	private Runnable onRemoveLastEmptySlot = () -> {};

	public InventoryHandlerSlotTracker(MemorySettingsCategory memorySettings) {
		this.memorySettings = memorySettings;
	}

	public void addPartiallyFilled(int slot, ItemStack stack) {
		ItemStackKey stackKey = new ItemStackKey(stack);
		partiallyFilledStackSlots.computeIfAbsent(stackKey, k -> {
			if (!fullStackSlots.containsKey(k)) {
				onAddStackKey.accept(k);
			}
			return new TreeSet<>();
		}).add(slot);
		partiallyFilledSlotStacks.put(slot, stackKey);
	}

	@Override
	public Set<ItemStackKey> getFullStacks() {
		return fullStackSlots.keySet();
	}

	@Override
	public Set<ItemStackKey> getPartialStacks() {
		return partiallyFilledStackSlots.keySet();
	}

	public void addFull(int slot, ItemStack stack) {
		ItemStackKey stackKey = new ItemStackKey(stack);
		fullStackSlots.computeIfAbsent(stackKey, k -> {
			if (!partiallyFilledStackSlots.containsKey(k)) {
				onAddStackKey.accept(k);
			}
			return new HashSet<>();
		}).add(slot);
		fullSlotStacks.put(slot, stackKey);
	}

	public void removePartiallyFilled(int slot) {
		if (partiallyFilledSlotStacks.containsKey(slot)) {
			ItemStackKey stackKey = partiallyFilledSlotStacks.remove(slot);
			Set<Integer> partialSlots = partiallyFilledStackSlots.get(stackKey);
			partialSlots.remove(slot);
			if (partialSlots.isEmpty()) {
				partiallyFilledStackSlots.remove(stackKey);
				if (!fullStackSlots.containsKey(stackKey)) {
					onRemoveStackKey.accept(stackKey);
				}
			}
		}
	}

	private void removeFull(int slot) {
		if (fullSlotStacks.containsKey(slot)) {
			ItemStackKey stackKey = fullSlotStacks.remove(slot);
			Set<Integer> fullSlots = fullStackSlots.get(stackKey);
			fullSlots.remove(slot);
			if (fullSlots.isEmpty()) {
				fullStackSlots.remove(stackKey);
				if (!partiallyFilledStackSlots.containsKey(stackKey)) {
					onRemoveStackKey.accept(stackKey);
				}
			}
		}
	}

	@Override
	public void removeAndSetSlotIndexes(InventoryHandler inventoryHandler, int slot, ItemStack stack) {
		if (stack.isEmpty()) {
			removePartiallyFilled(slot);
			removeFull(slot);
			addEmptySlot(slot);
			return;
		}

		if (emptySlots.contains(slot)) {
			removeEmpty(slot);
		}

		if (isPartiallyFilled(inventoryHandler, slot, stack)) {
			boolean containsSlot = partiallyFilledSlotStacks.containsKey(slot);
			if (!containsSlot || !partiallyFilledSlotStacks.get(slot).hashCodeEquals(stack)) {
				if (containsSlot) {
					removePartiallyFilled(slot);
				}
				addPartiallyFilled(slot, stack);
			}
			if (fullSlotStacks.containsKey(slot)) {
				removeFull(slot);
			}
		} else {
			boolean containsSlot = fullSlotStacks.containsKey(slot);
			if (!containsSlot || !fullSlotStacks.get(slot).hashCodeEquals(stack)) {
				if (containsSlot) {
					removeFull(slot);
				}
				addFull(slot, stack);
			}
			if (partiallyFilledSlotStacks.containsKey(slot)) {
				removePartiallyFilled(slot);
			}
		}
	}

	private void removeEmpty(int slot) {
		emptySlots.remove(slot);
		if (emptySlots.isEmpty()) {
			onRemoveLastEmptySlot.run();
		}
	}

	private void set(InventoryHandler inventoryHandler, int slot, ItemStack stack) {
		if (stack.isEmpty()) {
			addEmptySlot(slot);
		} else {
			if (isPartiallyFilled(inventoryHandler, slot, stack)) {
				addPartiallyFilled(slot, stack);
			} else {
				addFull(slot, stack);
			}
		}
	}

	private void addEmptySlot(int slot) {
		emptySlots.add(slot);
		if (emptySlots.size() == 1) {
			onAddFirstEmptySlot.run();
		}
	}

	@Override
	public void clear() {
		partiallyFilledStackSlots.clear();
		partiallyFilledSlotStacks.clear();
	}

	@Override
	public void refreshSlotIndexesFrom(InventoryHandler itemHandler) {
		fullStackSlots.keySet().forEach(sk -> onRemoveStackKey.accept(sk));
		fullStackSlots.clear();
		fullSlotStacks.clear();
		partiallyFilledStackSlots.keySet().forEach(sk -> onRemoveStackKey.accept(sk));
		partiallyFilledStackSlots.clear();
		partiallyFilledSlotStacks.clear();

		emptySlots.clear();
		onRemoveLastEmptySlot.run();

		for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
			ItemStack stack = itemHandler.getStackInSlot(slot);
			set(itemHandler, slot, stack);
		}
	}

	private boolean isPartiallyFilled(InventoryHandler itemHandler, int slot, ItemStack stack) {
		return stack.getCount() < itemHandler.getStackLimit(slot, stack);
	}

	@Override
	public ItemStack insertItemIntoHandler(InventoryHandler itemHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> overflowHandler, ItemStack stack, boolean simulate) {
		ItemStackKey stackKey = new ItemStackKey(stack);
		ItemStack remainingStack = handleOverflow(overflowHandler, stackKey, stack);
		if (remainingStack.isEmpty()) {
			return remainingStack;
		}
		remainingStack = insertIntoSlotsThatMatchStack(inserter, remainingStack, simulate, stackKey);
		if (!remainingStack.isEmpty()) {
			remainingStack = insertIntoEmptySlots(inserter, remainingStack, simulate);
		}
		return remainingStack;
	}

	@Override
	public ItemStack insertItemIntoHandler(InventoryHandler itemHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> overflowHandler, int slot, ItemStack stack, boolean simulate) {
		ItemStackKey stackKey = new ItemStackKey(stack);
		ItemStack remainingStack = stack;
		remainingStack = handleOverflow(overflowHandler, stackKey, remainingStack);
		if (remainingStack.isEmpty()) {
			return remainingStack;
		}

		ItemStack existing = itemHandler.getStackInSlot(slot);
		boolean wasEmpty = existing.isEmpty();

		boolean doesNotMatchCurrentSlot = !ItemHandlerHelper.canItemStacksStack(stack, existing);
		if (wasEmpty || doesNotMatchCurrentSlot) {
			remainingStack = insertIntoSlotsThatMatchStack(inserter, remainingStack, simulate, stackKey);
		}
		if (!remainingStack.isEmpty() && doesNotMatchCurrentSlot) {
			remainingStack = insertIntoEmptySlots(inserter, remainingStack, simulate);
		}
		if (!remainingStack.isEmpty()) {
			remainingStack = inserter.insertItem(slot, remainingStack, simulate);
		}

		return remainingStack;
	}

	@Override
	public void registerListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot) {
		this.onAddStackKey = onAddStackKey;
		this.onRemoveStackKey = onRemoveStackKey;
		this.onAddFirstEmptySlot = onAddFirstEmptySlot;
		this.onRemoveLastEmptySlot = onRemoveLastEmptySlot;
	}

	@Override
	public void unregisterStackKeyListeners() {
		onAddStackKey = sk -> {};
		onRemoveStackKey = sk -> {};
	}

	@Override
	public boolean hasEmptySlots() {
		return !emptySlots.isEmpty();
	}

	private ItemStack handleOverflow(UnaryOperator<ItemStack> overflowHandler, ItemStackKey stackKey, ItemStack remainingStack) {
		if (fullStackSlots.containsKey(stackKey) && !fullStackSlots.get(stackKey).isEmpty()) {
			remainingStack = overflowHandler.apply(remainingStack);
		}
		return remainingStack;
	}

	private ItemStack insertIntoSlotsThatMatchStack(IItemHandlerInserter inserter, ItemStack stack, boolean simulate, ItemStackKey stackKey) {
		ItemStack remainingStack = stack;

		int sizeBefore = partiallyFilledStackSlots.containsKey(stackKey) ? partiallyFilledStackSlots.get(stackKey).size() : 0;
		int i = 0;
		// Always taking first element here and iterating while not empty as iterating using iterator would produce CME due to void/compacting reacting to inserts
		// and going into this logic as well and because of that causing collection to be updated outside of first level iterator. The increment is here just
		// in case updating cache fails to prevent infinite loop
		while (partiallyFilledStackSlots.get(stackKey) != null && !partiallyFilledStackSlots.get(stackKey).isEmpty() && i++ < sizeBefore) {
			int matchingSlot = partiallyFilledStackSlots.get(stackKey).iterator().next();
			remainingStack = inserter.insertItem(matchingSlot, remainingStack, simulate);
			if (remainingStack.isEmpty()) {
				break;
			}
		}
		return remainingStack;
	}

	private ItemStack insertIntoEmptySlots(IItemHandlerInserter inserter, ItemStack stack, boolean simulate) {
		ItemStack remainingStack = stack.copy();
		remainingStack = insertIntoEmptyMemorySlots(inserter, simulate, remainingStack);
		if (!remainingStack.isEmpty()) {
			int sizeBefore = emptySlots.size();
			int i = 0;
			// Always taking first element here and iterating while not empty as iterating using iterator would produce CME due to void/compacting reacting to inserts
			// and going into this logic as well and because of that causing collection to be updated outside of first level iterator. The increment is here just
			// in case updating cache fails to prevent infinite loop
			while (!emptySlots.isEmpty() && i++ < sizeBefore) {
				Iterator<Integer> it = emptySlots.iterator();
				int slot = it.next();
				while (memorySettings.isSlotSelected(slot)) {
					if (!it.hasNext()) {
						return remainingStack;
					}
					slot = it.next();
				}

				remainingStack = inserter.insertItem(slot, remainingStack, simulate);
				if (remainingStack.isEmpty()) {
					break;
				}
			}
		}

		return remainingStack;
	}

	private ItemStack insertIntoEmptyMemorySlots(IItemHandlerInserter inserter, boolean simulate, ItemStack stack) {
		ItemStack remainingStack = stack;
		Map<Item, Set<Integer>> memoryFilterItemSlots = memorySettings.getFilterItemSlots();
		Item item = remainingStack.getItem();
		if (memoryFilterItemSlots.containsKey(item)) {
			for (int memorySlot : memoryFilterItemSlots.get(item)) {
				if (emptySlots.contains(memorySlot)) {
					remainingStack = inserter.insertItem(memorySlot, remainingStack, simulate);
					if (remainingStack.isEmpty()) {
						break;
					}
				}
			}
		}
		return remainingStack;
	}
}
