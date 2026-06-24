package net.p3pp3rf1y.sophisticatedcore.compat.accessories;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.EmptyHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AccessoriesCompat implements ICompat {
	@Override
	public void setup() {
		InventoryHelper.registerPlayerInventoryProvider(player -> AccessoriesCapability.getOptionally(player)
				.<IItemHandler>map(cap -> new AccessoriesHandler(player, cap)).orElse(EmptyHandler.INSTANCE));
		InventoryHelper.registerEquipmentInventoryProvider(player -> AccessoriesCapability.getOptionally(player)
				.<IItemHandler>map(cap -> new AccessoriesHandler(player, cap)).orElse(EmptyHandler.INSTANCE));
	}

	private static class AccessoriesHandler implements IItemHandlerModifiable {
		private final AccessoriesCapability cap;
		private final Player player;
		private Map<SlotType, Integer> identifierBaseIndexes = new LinkedHashMap<>();
		private final int totalSize;

		public AccessoriesHandler(Player player, AccessoriesCapability cap) {
			this.cap = cap;
			this.player = player;
			AtomicInteger totalSlots = new AtomicInteger(0);
			cap.getContainers().forEach((identifier, container) -> {
				identifierBaseIndexes.put(container.slotType(), totalSlots.get());
				totalSlots.addAndGet(container.getSize());
			});
			totalSize = totalSlots.get();
		}

		@Override
		public int getSlots() {
			return totalSize;
		}

		@Override
		public ItemStack getStackInSlot(int i) {
			if (totalSize <= i) {
				return ItemStack.EMPTY;
			}
			return getContainer(i).getAccessories().getItem(getLocalIndex(i));
		}

		private AccessoriesContainer getContainer(int slot) {
			for (Map.Entry<SlotType, Integer> entry : identifierBaseIndexes.entrySet()) {
				SlotType slotType = entry.getKey();
				int baseIndex = entry.getValue();
				AccessoriesContainer container = cap.getContainer(slotType);
				if (slot < baseIndex + container.getSize()) {
					return container;
				}
			}
			throw new IndexOutOfBoundsException("Slot " + slot + " is out of bounds for accessories inventory of size " + totalSize);
		}

		private int getLocalIndex(int slot) {
			for (Map.Entry<SlotType, Integer> entry : identifierBaseIndexes.entrySet()) {
				SlotType slotType = entry.getKey();
				int baseIndex = entry.getValue();
				AccessoriesContainer container = cap.getContainer(slotType);
				if (slot < baseIndex + container.getSize()) {
					return slot - baseIndex;
				}
			}
			throw new IndexOutOfBoundsException("Slot " + slot + " is out of bounds for accessories inventory of size " + totalSize);
		}

		private SlotType getSlotType(int slot) {
			for (Map.Entry<SlotType, Integer> entry : identifierBaseIndexes.entrySet()) {
				SlotType slotType = entry.getKey();
				int baseIndex = entry.getValue();
				AccessoriesContainer container = cap.getContainer(slotType);
				if (slot < baseIndex + container.getSize()) {
					return slotType;
				}
			}
			throw new IndexOutOfBoundsException("Slot " + slot + " is out of bounds for accessories inventory of size " + totalSize);
		}

		@Override
		public int getSlotLimit(int i) {
			if (totalSize <= i) {
				return 0;
			}
			return getContainer(i).getAccessories().getMaxStackSize();
		}

		@Override
		public boolean isItemValid(int i, ItemStack itemStack) {
			if (totalSize <= i) {
				return false;
			}

			return getContainer(i).getAccessories().canPlaceItem(getLocalIndex(i), itemStack);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack itemStack, boolean simulate) {
			if (totalSize <= slot) {
				return itemStack;
			}

			int localIndex = getLocalIndex(slot);
			SlotReference slotReference = SlotReference.of(player, getSlotType(slot).name(), localIndex);
			ItemStack currentStack = slotReference.getStack();
			if (currentStack != null && !currentStack.isEmpty() && !ItemStack.isSameItemSameTags(itemStack, currentStack)) {
				return itemStack;
			}

			int currentCount = currentStack != null ? currentStack.getCount() : 0;
			int countToInsert = Math.min(itemStack.getCount(), getContainer(slot).getAccessories().getMaxStackSize() - currentCount);

			if (countToInsert <= 0 || !getContainer(slot).getAccessories().canPlaceItem(localIndex, itemStack)) {
				return itemStack;
			}

			if (!simulate) {
				slotReference.setStack(itemStack.copyWithCount(countToInsert + currentCount));
			}

			if (itemStack.getCount() == countToInsert) {
				return ItemStack.EMPTY;
			} else {
				return itemStack.copyWithCount(itemStack.getCount() - countToInsert);
			}
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (totalSize <= slot) {
				return ItemStack.EMPTY;
			}

			int localIndex = getLocalIndex(slot);
			SlotReference slotReference = SlotReference.of(player, getSlotType(slot).name(), localIndex);
			ItemStack currentStack = slotReference.getStack();
			if (currentStack == null || currentStack.isEmpty()) {
				return ItemStack.EMPTY;
			}

			int countToExtract = Math.min(amount, currentStack.getCount());
			ItemStack extractedStack = currentStack.copyWithCount(countToExtract);

			if (!simulate) {
				if (currentStack.getCount() == countToExtract) {
					slotReference.setStack(ItemStack.EMPTY);
				} else {
					slotReference.setStack(currentStack.copyWithCount(currentStack.getCount() - countToExtract));
				}
			}

			return extractedStack;
		}

		@Override
		public void setStackInSlot(int i, ItemStack itemStack) {
			if (totalSize <= i) {
				return;
			}
			SlotReference slotReference = SlotReference.of(player, getSlotType(i).name(), getLocalIndex(i));
			slotReference.setStack(itemStack);
		}
	}
}
