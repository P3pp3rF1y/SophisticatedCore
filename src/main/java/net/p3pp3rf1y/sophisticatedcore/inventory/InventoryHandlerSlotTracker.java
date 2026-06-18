package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.SlotValueMap;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class InventoryHandlerSlotTracker implements ISlotTracker {
	private record SlotTrackerSlotSnapshot(int slot, @Nullable ItemStackKey fullSlotStack, @Nullable ItemStackKey partiallyFilledSlotStack, boolean emptySlot) implements Snapshot {
	}

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
	public Set<Integer> getFullSlots(ItemStackKey key) {
		return fullStackSlots.getOrDefault(key, Collections.emptySet());
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
			if (fullStackSlots.containsKey(stackKey) && stackMatcher.test(stackKey.stack())) {
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
				SophisticatedCore.LOGGER.error("Unstable ItemStack detected in slot tracking: {}", () -> stackKey != null ? stackKey.stack().toString() : "null");
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
				SophisticatedCore.LOGGER.error("Unstable ItemStack detected in slot tracking: {}", () -> stackKey != null ? stackKey.stack().toString() : "null");
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
		itemStackKeys.computeIfPresent(stackKey.stack().getItem(), (i, stackKeys) -> {
			stackKeys.remove(stackKey);
			return stackKeys;
		});
		if (itemStackKeys.containsKey(stackKey.stack().getItem())) {
			if (itemStackKeys.get(stackKey.stack().getItem()).isEmpty()) {
				itemStackKeys.remove(stackKey.stack().getItem());
			}
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
		fullStackSlots.keySet().forEach(sk -> onRemoveStackKey.accept(sk));
		fullStackSlots.clear();
		fullSlotStacks.clear();
		partiallyFilledStackSlots.keySet().forEach(sk -> onRemoveStackKey.accept(sk));
		partiallyFilledStackSlots.clear();
		partiallyFilledSlotStacks.clear();
		itemStackKeys.clear();

		emptySlots.clear();
		onRemoveLastEmptySlot.run();

		for (int slot = 0; slot < itemHandler.size(); slot++) {
			ItemStack stack = itemHandler.getStackInSlot(slot);
			set(itemHandler, slot, stack);
		}
	}

	@Override
	public Snapshot createSlotSnapshot(int slot) {
		return new SlotTrackerSlotSnapshot(slot, fullSlotStacks.get(slot), partiallyFilledSlotStacks.get(slot), emptySlots.contains(slot));
	}

	@Override
	public void restoreSlotFromSnapshot(Snapshot snapshot) {
		if (!(snapshot instanceof SlotTrackerSlotSnapshot slotTrackerSlotSnapshot)) {
			return;
		}

		int slot = slotTrackerSlotSnapshot.slot();
		ItemStackKey currentStackKey = getSlotStackKey(slot);
		ItemStackKey snapshotStackKey = getSnapshotStackKey(slotTrackerSlotSnapshot);
		boolean currentStackKeyWasTracked = currentStackKey != null && isTracked(currentStackKey);
		boolean snapshotStackKeyWasTracked = snapshotStackKey != null && isTracked(snapshotStackKey);
		boolean hadEmptySlots = !emptySlots.isEmpty();

		restoreSlotStateWithoutListeners(slotTrackerSlotSnapshot);

		if (currentStackKey != null && currentStackKeyWasTracked && !isTracked(currentStackKey)) {
			onRemoveStackKey.accept(currentStackKey);
		}
		if (snapshotStackKey != null && !snapshotStackKeyWasTracked && isTracked(snapshotStackKey)) {
			onAddStackKey.accept(snapshotStackKey);
		}

		boolean hasEmptySlots = !emptySlots.isEmpty();
		if (hadEmptySlots != hasEmptySlots) {
			if (hasEmptySlots) {
				onAddFirstEmptySlot.run();
			} else {
				onRemoveLastEmptySlot.run();
			}
		}
	}

	private @Nullable ItemStackKey getSlotStackKey(int slot) {
		ItemStackKey fullSlotStack = fullSlotStacks.get(slot);
		return fullSlotStack == null ? partiallyFilledSlotStacks.get(slot) : fullSlotStack;
	}

	private @Nullable ItemStackKey getSnapshotStackKey(SlotTrackerSlotSnapshot snapshot) {
		return snapshot.fullSlotStack() == null ? snapshot.partiallyFilledSlotStack() : snapshot.fullSlotStack();
	}

	private boolean isTracked(ItemStackKey stackKey) {
		return fullStackSlots.containsKey(stackKey) || partiallyFilledStackSlots.containsKey(stackKey);
	}

	private void restoreSlotStateWithoutListeners(SlotTrackerSlotSnapshot snapshot) {
		runWithListenersDisabled(() -> {
			int slot = snapshot.slot();
			removePartiallyFilled(slot);
			removeFull(slot);
			if (emptySlots.contains(slot)) {
				removeEmpty(slot);
			}

			if (snapshot.fullSlotStack() != null) {
				addFull(slot, snapshot.fullSlotStack().stack());
			} else if (snapshot.partiallyFilledSlotStack() != null) {
				addPartiallyFilled(slot, snapshot.partiallyFilledSlotStack().stack());
			} else if (snapshot.emptySlot()) {
				addEmptySlot(slot);
			}
		});
	}

	private void runWithListenersDisabled(Runnable runnable) {
		Consumer<ItemStackKey> originalOnAddStackKey = onAddStackKey;
		Consumer<ItemStackKey> originalOnRemoveStackKey = onRemoveStackKey;
		Runnable originalOnAddFirstEmptySlot = onAddFirstEmptySlot;
		Runnable originalOnRemoveLastEmptySlot = onRemoveLastEmptySlot;
		onAddStackKey = sk -> {
		};
		onRemoveStackKey = sk -> {
		};
		onAddFirstEmptySlot = () -> {
		};
		onRemoveLastEmptySlot = () -> {
		};
		try {
			runnable.run();
		} finally {
			onAddStackKey = originalOnAddStackKey;
			onRemoveStackKey = originalOnRemoveStackKey;
			onAddFirstEmptySlot = originalOnAddFirstEmptySlot;
			onRemoveLastEmptySlot = originalOnRemoveLastEmptySlot;
		}
	}

	private boolean isPartiallyFilled(InventoryHandler itemHandler, int slot, ItemStack stack) {
		return stack.getCount() < itemHandler.getCapacityNoInit(slot, ItemResource.of(stack));
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
		onAddStackKey = sk -> {
		};
		onRemoveStackKey = sk -> {
		};
	}

	@Override
	public boolean hasEmptySlots() {
		return shouldInsertIntoEmpty.getAsBoolean() && !emptySlots.isEmpty();
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

	@Override
	public Set<Integer> getPartialSlots(ItemStackKey key) {
		return partiallyFilledStackSlots.getOrDefault(key, Collections.emptySet());
	}

	@Override
	public Set<Integer> getEmptySlots() {
		return emptySlots;
	}
}
