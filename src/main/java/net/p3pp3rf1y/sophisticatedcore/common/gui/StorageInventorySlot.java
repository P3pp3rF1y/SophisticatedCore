package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.api.ISlotChangeResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;

public class StorageInventorySlot extends SlotSuppliedHandler {
	private final boolean isClientSide;
	private final IStorageWrapper storageWrapper;
	private final int slotIndex;

	public StorageInventorySlot(boolean isClientSide, IStorageWrapper storageWrapper, int slotIndex) {
		super(storageWrapper::getInventoryHandler, slotIndex, 0, 0);
		this.isClientSide = isClientSide;
		this.storageWrapper = storageWrapper;
		this.slotIndex = slotIndex;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		// saving here as well because there are many cases where vanilla modifies stack directly without and inventory handler isn't aware of it
		// however it does notify the slot of change
		storageWrapper.getInventoryHandler().onContentsChanged(slotIndex);
		processSlotChangeResponse(slotIndex, storageWrapper.getInventoryHandler(), storageWrapper);
	}

	private void processSlotChangeResponse(int slot, IItemHandler handler, IStorageWrapper storageWrapper) {
		if (!isClientSide) {
			storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(ISlotChangeResponseUpgrade.class).forEach(u -> u.onSlotChange(handler, slot));
		}
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return storageWrapper.getInventoryHandler().getStackLimit(slotIndex, stack);
	}

	@Override
	public ItemStack safeInsert(ItemStack stack, int maxCount) {
		if (!stack.isEmpty() && mayPlace(stack)) {
			ItemStack itemstack = getItem();
			int i = Math.min(Math.min(maxCount, stack.getCount()), getMaxStackSize(stack) - itemstack.getCount());
			if (itemstack.isEmpty()) {
				set(stack.split(i));
			} else if (ItemStack.isSameItemSameComponents(itemstack, stack)) {
				stack.shrink(i);
				ItemStack copy = itemstack.copy();
				copy.grow(i);
				set(copy);
			}

			return stack;
		} else {
			return stack;
		}
	}
}
