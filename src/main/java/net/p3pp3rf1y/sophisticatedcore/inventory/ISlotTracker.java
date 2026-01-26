package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public interface ISlotTracker {

	void setShouldInsertIntoEmpty(BooleanSupplier shouldInsertIntoEmpty);

	Set<ItemStackKey> getFullStacks();

	Set<ItemStackKey> getPartialStacks();

	Set<Item> getItems();

	void removeAndSetSlotIndexes(InventoryHandler inventoryHandler, int slot, ItemStack stack);

	void clear();

	void refreshSlotIndexesFrom(InventoryHandler itemHandler);

	ItemStack insertItemIntoHandler(InventoryHandler itemHandler, BiFunction<ItemStack, Boolean, ItemStack> beforeInsertHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> slotOverflowHandler, UnaryOperator<ItemStack> storageOverflowHandler, ItemStack stack, boolean simulate);

	ItemStack insertItemIntoHandler(InventoryHandler itemHandler, BiFunction<ItemStack, Boolean, ItemStack> beforeInsertHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> slotOverflowHandler, UnaryOperator<ItemStack> storageOverflowHandler, int slot, ItemStack stack, boolean simulate);

	void registerListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot);

	void unregisterStackKeyListeners();

	boolean hasEmptySlots();

	boolean hasExactStackMemorized(ItemStackKey stackKey);

	boolean hasItemMemorizedOrFiltered(Item item);

	int getFirstMatchingSlot(ItemStackKey stackKey);

	ItemStack extractItemFromHandler(InventoryHandler inventoryHandler, IItemHandlerExtractor extractItemInternal, ItemStack stack, boolean simulate);

	interface IItemHandlerInserter {
		ItemStack insertItem(int slot, ItemStack stack, boolean simulate);
	}

	interface IItemHandlerExtractor {
		ItemStack extractItem(int slot, int amount, boolean simulate);
	}

	class Noop implements ISlotTracker {
		@Override
		public void setShouldInsertIntoEmpty(BooleanSupplier shouldInsertIntoEmpty) {
			//noop
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
		public void removeAndSetSlotIndexes(InventoryHandler inventoryHandler, int slot, ItemStack stack) {
			//noop
		}

		@Override
		public void clear() {
			//noop
		}

		@Override
		public void refreshSlotIndexesFrom(InventoryHandler itemHandler) {
			//noop
		}

		@Override
		public ItemStack insertItemIntoHandler(InventoryHandler itemHandler, BiFunction<ItemStack, Boolean, ItemStack> beforeInsertHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> slotOverflowHandler, UnaryOperator<ItemStack> storageOverflowHandler, ItemStack stack, boolean simulate) {
			return stack;
		}

		@Override
		public ItemStack extractItemFromHandler(InventoryHandler inventoryHandler, IItemHandlerExtractor extractItemInternal, ItemStack stack, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItemIntoHandler(InventoryHandler itemHandler, BiFunction<ItemStack, Boolean, ItemStack> beforeInsertHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> slotOverflowHandler, UnaryOperator<ItemStack> storageOverflowHandler, int slot, ItemStack stack, boolean simulate) {
			return inserter.insertItem(slot, stack, simulate);
		}

		@Override
		public void registerListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot) {
			//noop
		}

		@Override
		public void unregisterStackKeyListeners() {
			//noop
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
	}
}
