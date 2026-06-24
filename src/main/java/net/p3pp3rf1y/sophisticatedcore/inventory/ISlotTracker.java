package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface ISlotTracker {

	void setShouldInsertIntoEmpty(BooleanSupplier shouldInsertIntoEmpty);

	Set<ItemStackKey> getFullStacks();

	Set<Integer> getFullSlots(ItemStackKey key);

	Set<ItemStackKey> getPartialStacks();

	Set<Item> getItems();

	boolean hasMatchingFullStack(ItemStack stack, Predicate<ItemStack> stackMatcher);

	void removeAndSetSlotIndexes(InventoryHandler inventoryHandler, int slot, ItemStack stack);

	void clear();

	void refreshSlotIndexesFrom(InventoryHandler itemHandler);

	Snapshot createSlotSnapshot(int slot);

	void restoreSlotFromSnapshot(Snapshot snapshot);

	void registerListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot,
			Runnable onRemoveLastEmptySlot);

	void unregisterStackKeyListeners();

	boolean hasEmptySlots();

	boolean hasExactStackMemorized(ItemStackKey stackKey);

	boolean hasItemMemorizedOrFiltered(Item item);

	int getFirstMatchingSlot(ItemStackKey stackKey);

	Set<Integer> getPartialSlots(ItemStackKey key);

	Set<Integer> getEmptySlots();

	interface Snapshot {
	}

	interface IItemHandlerInserter {
		ItemStack insertItem(int slot, ItemStack stack, boolean simulate);
	}

	class Noop implements ISlotTracker {
		private enum NoopSnapshot implements Snapshot {
			INSTANCE
		}

		@Override
		public void setShouldInsertIntoEmpty(BooleanSupplier shouldInsertIntoEmpty) {
			// noop
		}

		@Override
		public Set<ItemStackKey> getFullStacks() {
			return Collections.emptySet();
		}

		@Override
		public Set<ItemStackKey> getPartialStacks() {
			return Collections.emptySet();
		}

		@Override
		public Set<Item> getItems() {
			return Collections.emptySet();
		}

		@Override
		public boolean hasMatchingFullStack(ItemStack stack, Predicate<ItemStack> stackMatcher) {
			return false;
		}

		@Override
		public void removeAndSetSlotIndexes(InventoryHandler inventoryHandler, int slot, ItemStack stack) {
			// noop
		}

		@Override
		public void clear() {
			// noop
		}

		@Override
		public void refreshSlotIndexesFrom(InventoryHandler itemHandler) {
			// noop
		}

		@Override
		public Snapshot createSlotSnapshot(int slot) {
			return NoopSnapshot.INSTANCE;
		}

		@Override
		public void restoreSlotFromSnapshot(Snapshot snapshot) {
			// noop
		}

		@Override
		public void registerListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot,
				Runnable onRemoveLastEmptySlot) {
			// noop
		}

		@Override
		public void unregisterStackKeyListeners() {
			// noop
		}

		@Override
		public boolean hasEmptySlots() {
			return false;
		}

		@Override
		public int getFirstMatchingSlot(ItemStackKey stackKey) {
			return -1;
		}

		@Override
		public boolean hasExactStackMemorized(ItemStackKey stackKey) {
			return false;
		}

		@Override
		public boolean hasItemMemorizedOrFiltered(Item item) {
			return false;
		}

		@Override
		public Set<Integer> getPartialSlots(ItemStackKey key) {
			return Collections.emptySet();
		}

		@Override
		public Set<Integer> getEmptySlots() {
			return Collections.emptySet();
		}

		@Override
		public Set<Integer> getFullSlots(ItemStackKey key) {
			return Collections.emptySet();
		}
	}
}
