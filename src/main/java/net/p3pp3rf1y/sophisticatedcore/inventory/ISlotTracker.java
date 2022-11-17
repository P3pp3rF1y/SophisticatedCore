package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public interface ISlotTracker {

	void setShouldInsertIntoEmpty(BooleanSupplier shouldInsertIntoEmpty);

	Set<ItemStackKey> getFullStacks();

	Set<ItemStackKey> getPartialStacks();

	void removeAndSetSlotIndexes(InventoryHandler inventoryHandler, int slot, ItemStack stack);

	void clear();

	void refreshSlotIndexesFrom(InventoryHandler itemHandler);

	ItemStack insertItemIntoHandler(InventoryHandler itemHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> overflowHandler, ItemStack stack, boolean simulate);

	ItemStack insertItemIntoHandler(InventoryHandler itemHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> overflowHandler, int slot, ItemStack stack, boolean simulate);

	void registerListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot);

	void unregisterStackKeyListeners();

	boolean hasEmptySlots();

	interface IItemHandlerInserter {
		ItemStack insertItem(int slot, ItemStack stack, boolean simulate);
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
		public ItemStack insertItemIntoHandler(InventoryHandler itemHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> overflowHandler, ItemStack stack, boolean simulate) {
			return stack;
		}

		@Override
		public ItemStack insertItemIntoHandler(InventoryHandler itemHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> overflowHandler, int slot, ItemStack stack, boolean simulate) {
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
	}
}
