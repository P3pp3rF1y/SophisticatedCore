package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Set;
import java.util.function.Consumer;

public class OverflowAwareInventoryHandler implements ITrackedContentsItemResourceHandler{
	private final InventoryHandler inventoryHandler;

	public OverflowAwareInventoryHandler(InventoryHandler inventoryHandler) {
		this.inventoryHandler = inventoryHandler;
	}

	@Override
	public Set<ItemStackKey> getTrackedStacks() {
		return inventoryHandler.getTrackedStacks();
	}

	@Override
	public void registerTrackingListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot) {
		inventoryHandler.registerTrackingListeners(onAddStackKey, onRemoveStackKey, onAddFirstEmptySlot, onRemoveLastEmptySlot);
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
	public long getCapacityAsLong(int i, ItemResource resource) {
		return inventoryHandler.getOverflowAwareCapacity(i, resource);
	}

	@Override
	public boolean isValid(int i, ItemResource resource) {
		return inventoryHandler.isValid(i, resource);
	}

	@Override
	public int insert(int i, ItemResource resource, int i1, TransactionContext transactionContext) {
		return inventoryHandler.insert(i, resource, i1, transactionContext);
	}

	@Override
	public int extract(int i, ItemResource resource, int i1, TransactionContext transactionContext) {
		return inventoryHandler.extract(i, resource, i1, transactionContext);
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return inventoryHandler.getStackInSlot(slot);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		inventoryHandler.setStackInSlot(slot, stack);
	}

	@Override
	public int insert(ItemResource resource, int amount, TransactionContext transaction) {
		return inventoryHandler.insert(resource, amount, transaction);
	}

	@Override
	public int extract(ItemResource resource, int amount, TransactionContext transaction) {
		return inventoryHandler.extract(resource, amount, transaction);
	}
}
