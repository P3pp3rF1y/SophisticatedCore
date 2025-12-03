package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.api.ISlotChangeResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

public class StorageInventorySlot extends SlotSuppliedHandler {
	private final IStorageWrapper storageWrapper;
	private final int slotIndex;
	private final Player player;

	public StorageInventorySlot(boolean isClientSide, IStorageWrapper storageWrapper, int slotIndex, Player player) {
		super(storageWrapper::getInventoryHandler,
				(i, resource, amount) -> {
					storageWrapper.getInventoryHandler().setStackInSlot(i, resource.toStack(amount));
					if (!isClientSide) {
						processSlotChangeResponse(slotIndex, storageWrapper.getInventoryHandler(), storageWrapper);
					}
				},
				slotIndex, 0, 0);
		this.storageWrapper = storageWrapper;
		this.slotIndex = slotIndex;
		this.player = player;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return storageWrapper.getInventoryHandler().isItemValid(slotIndex, stack, player);
	}

	private static void processSlotChangeResponse(int slot, InventoryHandler handler, IStorageWrapper storageWrapper) {
		storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(ISlotChangeResponseUpgrade.class).forEach(u -> u.onSlotChange(handler, slot));
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return storageWrapper.getInventoryHandler().getCapacityAsInt(slotIndex, ItemResource.of(stack));
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
