package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.SlotValueMap;

import javax.annotation.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class InventoryHandlerSlotTracker implements ISlotTracker {
	private final Map<ItemStackKey, Set<Integer>> fullStackSlots = new HashMap<>();
	private final Map<Integer, ItemStackKey> fullSlotStacks = new HashMap<>();
	private final Map<ItemStackKey, Set<Integer>> partiallyFilledStackSlots = new HashMap<>();
	private final Map<Integer, ItemStackKey> partiallyFilledSlotStacks = new HashMap<>();
	private final Map<Item, Set<ItemStackKey>> itemStackKeys = new HashMap<>();
	private final Set<Integer> emptySlots = new TreeSet<>();
	private final MemorySettingsCategory memorySettings;
	private final SlotValueMap<Item> filterItemSlots;
	private Consumer<ItemStackKey> onAddStackKey = sk -> {
	};
	private Consumer<ItemStackKey> onRemoveStackKey = sk -> {
	};

	private Runnable onAddFirstEmptySlot = () -> {
	};
	private Runnable onRemoveLastEmptySlot = () -> {
	};

	private BooleanSupplier shouldInsertIntoEmpty = () -> true;

	public InventoryHandlerSlotTracker(MemorySettingsCategory memorySettings, SlotValueMap<Item> filterItemSlots) {
		this.memorySettings = memorySettings;
		this.filterItemSlots = filterItemSlots;
	}

	@Override
	public void setShouldInsertIntoEmpty(BooleanSupplier shouldInsertIntoEmpty) {
		this.shouldInsertIntoEmpty = shouldInsertIntoEmpty;
	}

	public void addPartiallyFilled(int slot, ItemStack stack) {
		ItemStackKey stackKey = ItemStackKey.of(stack);
		partiallyFilledStackSlots.computeIfAbsent(stackKey, k -> {
			if (!fullStackSlots.containsKey(k)) {
				onAddStackKey.accept(k);
			}
			return new TreeSet<>();
		}).add(slot);
		partiallyFilledSlotStacks.put(slot, stackKey);
		itemStackKeys.computeIfAbsent(stack.getItem(), i -> new HashSet<>()).add(stackKey);
	}

	@Override
	public Set<ItemStackKey> getFullStacks() {
		return fullStackSlots.keySet();
	}

	@Override
	public Set<ItemStackKey> getPartialStacks() {
		return partiallyFilledStackSlots.keySet();
	}

	@Override
	public boolean hasExactStackMemorized(ItemStackKey stackKey) {
		return memorySettings.matchesStackKey(stackKey);
	}

	@Override
	public boolean hasItemMemorizedOrFiltered(Item item) {
		return memorySettings.matchesItem(item) || filterItemSlots.containsValue(item);
	}

	@Override
	public Set<Item> getItems() {
		return itemStackKeys.keySet();
	}

	@Override
	public boolean hasMatchingFullStack(ItemStack stack, Predicate<ItemStack> stackMatcher) {
		Set<ItemStackKey> stackKeys = itemStackKeys.get(stack.getItem());
		if (stackKeys == null || stackKeys.isEmpty()) {
			return false;
		}

		for (ItemStackKey stackKey : stackKeys) {
			if (fullStackSlots.containsKey(stackKey) && stackMatcher.test(stackKey.getStack())) {
				return true;
			}
		}

		return false;
	}

	public void addFull(int slot, ItemStack stack) {
		ItemStackKey stackKey = ItemStackKey.of(stack);
		fullStackSlots.computeIfAbsent(stackKey, k -> {
			if (!partiallyFilledStackSlots.containsKey(k)) {
				onAddStackKey.accept(k);
			}
			return new TreeSet<>();
		}).add(slot);
		fullSlotStacks.put(slot, stackKey);
		itemStackKeys.computeIfAbsent(stack.getItem(), i -> new HashSet<>()).add(stackKey);
	}

	public void removePartiallyFilled(int slot) {
		if (partiallyFilledSlotStacks.containsKey(slot)) {
			ItemStackKey stackKey = partiallyFilledSlotStacks.remove(slot);
			@Nullable
			Set<Integer> partialSlots = partiallyFilledStackSlots.get(stackKey);
			if (partialSlots == null) {
				SophisticatedCore.LOGGER.error("Unstable ItemStack detected in slot tracking: {}",
						() -> stackKey != null ? stackKey.stack().toString() : "null");
			} else {
				partialSlots.remove(slot);
			}
			if (partialSlots == null || partialSlots.isEmpty()) {
				partiallyFilledStackSlots.remove(stackKey);
				if (!fullStackSlots.containsKey(stackKey)) {
					onStackKeyRemoved(stackKey);
				}
			}
		}
	}

	private void removeFull(int slot) {
		if (fullSlotStacks.containsKey(slot)) {
			ItemStackKey stackKey = fullSlotStacks.remove(slot);
			@Nullable
			Set<Integer> fullSlots = fullStackSlots.get(stackKey);
			if (fullSlots == null) {
				SophisticatedCore.LOGGER.error("Unstable ItemStack detected in slot tracking: {}",
						() -> stackKey != null ? stackKey.stack().toString() : "null");
			} else {
				fullSlots.remove(slot);
			}
			if (fullSlots == null || fullSlots.isEmpty()) {
				fullStackSlots.remove(stackKey);
				if (!partiallyFilledStackSlots.containsKey(stackKey)) {
					onStackKeyRemoved(stackKey);
				}
			}
		}
	}

	private void onStackKeyRemoved(ItemStackKey stackKey) {
		itemStackKeys.computeIfPresent(stackKey.getStack().getItem(), (i, stackKeys) -> {
			stackKeys.remove(stackKey);
			return stackKeys;
		});
		if (itemStackKeys.containsKey(stackKey.getStack().getItem()) && itemStackKeys.get(stackKey.getStack().getItem()).isEmpty()) {
			itemStackKeys.remove(stackKey.getStack().getItem());
		}

		onRemoveStackKey.accept(stackKey);
	}

	@Override
	public void removeAndSetSlotIndexes(InventoryHandler inventoryHandler, int slot, ItemStack stack) {
		if (stack.isEmpty()) {
			removePartiallyFilled(slot);
			removeFull(slot);
			if (inventoryHandler.isSlotAccessible(slot)) {
				addEmptySlot(slot);
			}
			return;
		}

		if (emptySlots.contains(slot)) {
			removeEmpty(slot);
		}

		if (isPartiallyFilled(inventoryHandler, slot, stack)) {
			setPartiallyFilled(slot, stack);
		} else {
			setFull(slot, stack);
		}
	}

	private void setFull(int slot, ItemStack stack) {
		boolean containsSlot = fullSlotStacks.containsKey(slot);
		if (!containsSlot || fullSlotStacks.get(slot).hashCodeNotEquals(stack)) {
			if (containsSlot) {
				removeFull(slot);
			}
			addFull(slot, stack);
		}
		if (partiallyFilledSlotStacks.containsKey(slot)) {
			removePartiallyFilled(slot);
		}
	}

	private void setPartiallyFilled(int slot, ItemStack stack) {
		boolean containsSlot = partiallyFilledSlotStacks.containsKey(slot);
		if (!containsSlot || partiallyFilledSlotStacks.get(slot).hashCodeNotEquals(stack)) {
			if (containsSlot) {
				removePartiallyFilled(slot);
			}
			addPartiallyFilled(slot, stack);
		}
		if (fullSlotStacks.containsKey(slot)) {
			removeFull(slot);
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
			if (inventoryHandler.isSlotAccessible(slot)) {
				addEmptySlot(slot);
			}
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
		fullStackSlots.keySet().forEach(onRemoveStackKey::accept);
		fullStackSlots.clear();
		fullSlotStacks.clear();
		partiallyFilledStackSlots.keySet().forEach(onRemoveStackKey::accept);
		partiallyFilledStackSlots.clear();
		partiallyFilledSlotStacks.clear();
		itemStackKeys.clear();

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
	public ItemStack insertItemIntoHandler(InventoryHandler itemHandler, BiFunction<ItemStack, Boolean, ItemStack> beforeInsertHandler,
			IItemHandlerInserter inserter, UnaryOperator<ItemStack> slotOverflowHandler, UnaryOperator<ItemStack> storageOverflowHandler, ItemStack stack,
			boolean simulate) {
		if (emptySlots.isEmpty() && !itemStackKeys.containsKey(stack.getItem())) {
			return stack;
		}

		ItemStackKey stackKey = ItemStackKey.of(stack);
		ItemStack remainingStack = beforeInsertHandler.apply(stack, simulate);
		remainingStack = handleOverflow(itemHandler, slotOverflowHandler, storageOverflowHandler, stackKey, remainingStack);
		if (remainingStack.isEmpty()) {
			return remainingStack;
		}
		int countBeforeInsert = remainingStack.getCount();
		Set<Integer> simulatedSlots = simulate ? new HashSet<>() : Collections.emptySet();
		remainingStack = insertIntoSlotsThatMatchStack(inserter, remainingStack, simulate, stackKey, simulatedSlots);
		if (!remainingStack.isEmpty()) {
			remainingStack = insertIntoEmptySlots(inserter, remainingStack, simulate, simulatedSlots);
		}
		if (!remainingStack.isEmpty()) {
			remainingStack = handleOverflow(itemHandler, slotOverflowHandler, storageOverflowHandler, stackKey, remainingStack,
					hasOneFullStackOfItem(stackKey) || (simulate && remainingStack.getCount() < countBeforeInsert), simulatedSlots);
		}
		return remainingStack;
	}

	@Override
	public ItemStack insertItemIntoHandler(InventoryHandler itemHandler, BiFunction<ItemStack, Boolean, ItemStack> beforeInsertHandler,
			IItemHandlerInserter inserter, UnaryOperator<ItemStack> slotOverflowHandler, UnaryOperator<ItemStack> storageOverflowHandler, int slot,
			ItemStack stack, boolean simulate) {
		if (simulate) {
			ItemStack remainingStack = beforeInsertHandler.apply(stack, simulate);
			if (remainingStack.isEmpty()) {
				return remainingStack;
			}

			ItemStackKey stackKey = ItemStackKey.of(remainingStack);
			int countBeforeInsert = remainingStack.getCount();
			Set<Integer> simulatedSlots = Collections.emptySet();
			if ((emptySlots.contains(slot) && canInsertIntoEmptySlot(itemHandler, slot, remainingStack))
					|| (partiallyFilledSlotStacks.containsKey(slot) && !partiallyFilledSlotStacks.get(slot).hashCodeNotEquals(remainingStack))) {
				remainingStack = inserter.insertItem(slot, remainingStack, true);
				if (remainingStack.isEmpty()) {
					return remainingStack;
				}
				simulatedSlots = Set.of(slot);
			}
			if (fullSlotStacks.containsKey(slot) || emptySlots.contains(slot) || partiallyFilledSlotStacks.containsKey(slot)) {
				return handleOverflow(itemHandler, slotOverflowHandler, storageOverflowHandler, stackKey, remainingStack,
						remainingStack.getCount() < countBeforeInsert || hasOneFullStackOfItem(stackKey), simulatedSlots);
			}
			return remainingStack;
		}

		return insertItemIntoHandler(itemHandler, beforeInsertHandler, inserter, slotOverflowHandler, storageOverflowHandler, stack, simulate);
	}

	@Override
	public ItemStack extractItemFromHandler(InventoryHandler inventoryHandler, IItemHandlerExtractor extractItemInternal, ItemStack stack, boolean simulate) {
		if (partiallyFilledSlotStacks.isEmpty() && fullSlotStacks.isEmpty()) {
			return ItemStack.EMPTY;
		}

		ItemStackKey stackKey = ItemStackKey.of(stack);
		int extracted = 0;

		Set<Integer> slots = partiallyFilledStackSlots.get(stackKey);
		if (slots != null && !slots.isEmpty()) {
			int originalSize = slots.size();
			int i = 0;
			while (partiallyFilledStackSlots.get(stackKey) != null && !partiallyFilledStackSlots.get(stackKey).isEmpty() && i++ < originalSize) {
				int matchingSlot = partiallyFilledStackSlots.get(stackKey).iterator().next();
				extracted += extractItemInternal.extractItem(matchingSlot, stack.getCount() - extracted, simulate).getCount();
				if (extracted >= stack.getCount()) {
					break;
				}
			}
		}

		slots = fullStackSlots.get(stackKey);
		if (slots != null && !slots.isEmpty() && stack.getCount() > extracted) {
			int originalSize = slots.size();
			int i = 0;
			while (fullStackSlots.get(stackKey) != null && !fullStackSlots.get(stackKey).isEmpty() && i++ < originalSize) {
				int matchingSlot = fullStackSlots.get(stackKey).iterator().next();
				extracted += extractItemInternal.extractItem(matchingSlot, stack.getCount() - extracted, simulate).getCount();
				if (extracted >= stack.getCount()) {
					break;
				}
			}
		}

		return extracted > 0 ? stack.copyWithCount(extracted) : ItemStack.EMPTY;
	}

	@Override
	public void registerListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot,
			Runnable onRemoveLastEmptySlot) {
		this.onAddStackKey = onAddStackKey;
		this.onRemoveStackKey = onRemoveStackKey;
		this.onAddFirstEmptySlot = onAddFirstEmptySlot;
		this.onRemoveLastEmptySlot = onRemoveLastEmptySlot;
	}

	@Override
	public void unregisterStackKeyListeners() {
		onAddStackKey = sk -> {
		};
		onRemoveStackKey = sk -> {
		};
	}

	@Override
	public boolean hasEmptySlots() {
		return shouldInsertIntoEmpty.getAsBoolean() && !emptySlots.isEmpty();
	}

	private ItemStack handleOverflow(InventoryHandler itemHandler, UnaryOperator<ItemStack> slotOverflowHandler,
			UnaryOperator<ItemStack> storageOverflowHandler, ItemStackKey stackKey, ItemStack remainingStack) {
		boolean hasOneFullStackOfItem = hasOneFullStackOfItem(stackKey);
		return handleOverflow(itemHandler, slotOverflowHandler, storageOverflowHandler, stackKey, remainingStack, hasOneFullStackOfItem,
				Collections.emptySet());
	}

	private boolean hasOneFullStackOfItem(ItemStackKey stackKey) {
		return fullStackSlots.containsKey(stackKey) && !fullStackSlots.get(stackKey).isEmpty();
	}

	private ItemStack handleOverflow(InventoryHandler itemHandler, UnaryOperator<ItemStack> slotOverflowHandler,
			UnaryOperator<ItemStack> storageOverflowHandler, ItemStackKey stackKey, ItemStack remainingStack, boolean hasOneFullSlotOfItem,
			Set<Integer> simulatedSlots) {
		if (hasOneFullSlotOfItem) {
			if (!hasInsertionSpace(itemHandler, stackKey, remainingStack, simulatedSlots)) {
				remainingStack = storageOverflowHandler.apply(remainingStack);
				if (remainingStack.isEmpty()) {
					return remainingStack;
				}
			}
			remainingStack = slotOverflowHandler.apply(remainingStack);
		}
		return remainingStack;
	}

	private boolean hasInsertionSpace(InventoryHandler itemHandler, ItemStackKey stackKey, ItemStack stack, Set<Integer> simulatedSlots) {
		for (int slot : partiallyFilledStackSlots.getOrDefault(stackKey, Collections.emptySet())) {
			if (!simulatedSlots.contains(slot) && itemHandler.isItemValid(slot, stack)
					&& itemHandler.getStackInSlot(slot).getCount() < itemHandler.getStackLimit(slot, stack)) {
				return true;
			}
		}
		for (int slot : emptySlots) {
			if (!simulatedSlots.contains(slot) && canInsertIntoEmptySlot(itemHandler, slot, stack)) {
				return true;
			}
		}
		return false;
	}

	private boolean canInsertIntoEmptySlot(InventoryHandler itemHandler, int slot, ItemStack stack) {
		return (shouldInsertIntoEmpty.getAsBoolean() || memorySettings.isSlotSelected(slot) || filterItemSlots.containsSlot(slot))
				&& memorySettings.matchesFilter(slot, stack) && !filterItemSlots.containsSlotAndDoesNotMatch(slot, stack.getItem())
				&& itemHandler.isItemValid(slot, stack) && itemHandler.getStackLimit(slot, stack) > 0;
	}

	private ItemStack insertIntoSlotsThatMatchStack(IItemHandlerInserter inserter, ItemStack stack, boolean simulate, ItemStackKey stackKey,
			Set<Integer> simulatedSlots) {
		ItemStack remainingStack = stack;

		Set<Integer> slots = partiallyFilledStackSlots.get(stackKey);
		if (slots == null || slots.isEmpty()) {
			return remainingStack;
		}

		// Inserts can reenter this tracker through upgrades, so iterate a snapshot and recheck current membership.
		for (int matchingSlot : new ArrayList<>(slots)) {
			if (!partiallyFilledStackSlots.getOrDefault(stackKey, Collections.emptySet()).contains(matchingSlot)) {
				continue;
			}
			remainingStack = inserter.insertItem(matchingSlot, remainingStack, simulate);
			if (simulate) {
				simulatedSlots.add(matchingSlot);
			}
			if (remainingStack.isEmpty()) {
				break;
			}
		}
		return remainingStack;
	}

	private ItemStack insertIntoEmptySlots(IItemHandlerInserter inserter, ItemStack stack, boolean simulate, Set<Integer> simulatedSlots) {
		ItemStack remainingStack = stack;
		remainingStack = insertIntoEmptyMemorySlots(inserter, simulate, remainingStack, simulatedSlots);
		remainingStack = insertIntoEmptyFilterSlots(inserter, simulate, remainingStack, simulatedSlots);
		if (shouldInsertIntoEmpty.getAsBoolean() && !remainingStack.isEmpty()) {
			for (int slot : new ArrayList<>(emptySlots)) {
				if (!emptySlots.contains(slot) || simulatedSlots.contains(slot) || memorySettings.isSlotSelected(slot) || filterItemSlots.containsSlot(slot)) {
					continue;
				}

				remainingStack = inserter.insertItem(slot, remainingStack, simulate);
				if (simulate) {
					simulatedSlots.add(slot);
				}
				if (remainingStack.isEmpty()) {
					break;
				}
			}
		}

		return remainingStack;
	}

	private ItemStack insertIntoEmptyFilterSlots(IItemHandlerInserter inserter, boolean simulate, ItemStack remainingStack, Set<Integer> simulatedSlots) {
		Item item = remainingStack.getItem();
		if (filterItemSlots.containsValue(item)) {
			for (int filterSlot : new ArrayList<>(filterItemSlots.getSlots(item))) {
				if (emptySlots.contains(filterSlot) && !simulatedSlots.contains(filterSlot)) {
					remainingStack = inserter.insertItem(filterSlot, remainingStack, simulate);
					if (simulate) {
						simulatedSlots.add(filterSlot);
					}
					if (remainingStack.isEmpty()) {
						break;
					}
				}
			}
		}
		return remainingStack;
	}

	private ItemStack insertIntoEmptyMemorySlots(IItemHandlerInserter inserter, boolean simulate, ItemStack stack, Set<Integer> simulatedSlots) {
		ItemStack remainingStack = stack;
		Map<Item, Set<Integer>> memoryFilterItemSlots = memorySettings.getFilterItemSlots();
		Item item = remainingStack.getItem();
		if (memoryFilterItemSlots.containsKey(item)) {
			for (int memorySlot : new ArrayList<>(memoryFilterItemSlots.get(item))) {
				if (emptySlots.contains(memorySlot) && !simulatedSlots.contains(memorySlot)) {
					remainingStack = inserter.insertItem(memorySlot, remainingStack, simulate);
					if (simulate) {
						simulatedSlots.add(memorySlot);
					}
					if (remainingStack.isEmpty()) {
						break;
					}
				}
			}
		}

		Map<Integer, Set<Integer>> memoryFilterStackSlots = memorySettings.getFilterStackSlots();
		if (!memoryFilterStackSlots.isEmpty()) {
			int stackHash = ItemStack.hashItemAndComponents(remainingStack);
			if (memoryFilterStackSlots.containsKey(stackHash)) {
				for (int memorySlot : new ArrayList<>(memoryFilterStackSlots.get(stackHash))) {
					if (emptySlots.contains(memorySlot) && !simulatedSlots.contains(memorySlot)) {
						remainingStack = inserter.insertItem(memorySlot, remainingStack, simulate);
						if (simulate) {
							simulatedSlots.add(memorySlot);
						}
						if (remainingStack.isEmpty()) {
							break;
						}
					}
				}
			}
		}
		return remainingStack;
	}

	@Override
	public int getFirstMatchingSlot(ItemStackKey stackKey) {
		Set<Integer> slots = partiallyFilledStackSlots.get(stackKey);
		if (slots != null && !slots.isEmpty()) {
			return slots.iterator().next();
		}
		slots = fullStackSlots.get(stackKey);
		if (slots != null && !slots.isEmpty()) {
			return slots.iterator().next();
		}
		return -1;
	}
}
