package net.p3pp3rf1y.sophisticatedcore.compat.accessories;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AccessoriesCompat implements ICompat {
	@Override
	public void setup() {
		InventoryHelper.registerPlayerInventoryProvider(player -> AccessoriesCapability.getOptionally(player)
				.<ResourceHandler<ItemResource>>map(cap -> new AccessoriesHandler(player, cap)).orElse(EmptyResourceHandler.instance()));
		InventoryHelper.registerEquipmentInventoryProvider(player -> AccessoriesCapability.getOptionally(player)
				.<ResourceHandler<ItemResource>>map(cap -> new AccessoriesHandler(player, cap)).orElse(EmptyResourceHandler.instance()));
	}

	private static class AccessoriesHandler implements ResourceHandler<ItemResource> {
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
		public int size() {
			return totalSize;
		}

		@Override
		public ItemResource getResource(int i) {
			if (totalSize <= i) {
				return ItemResource.EMPTY;
			}
			return ItemResource.of(getContainer(i).getAccessories().getItem(getLocalIndex(i)));
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
		public long getAmountAsLong(int i) {
			if (totalSize <= i) {
				return 0;
			}
			return getContainer(i).getAccessories().getItem(getLocalIndex(i)).getCount();
		}

		@Override
		public long getCapacityAsLong(int i, ItemResource resource) {
			if (totalSize <= i) {
				return 0;
			}
			return getContainer(i).getAccessories().getMaxStackSize(resource.toStack());
		}

		@Override
		public boolean isValid(int i, ItemResource resource) {
			if (totalSize <= i) {
				return false;
			}

			return getContainer(i).getAccessories().canPlaceItem(getLocalIndex(i), resource.toStack());
		}

		@Override
		public int insert(int i, ItemResource resource, int amount, TransactionContext transactionContext) {
			if (totalSize <= i) {
				return 0;
			}

			int localIndex = getLocalIndex(i);
			SlotReference slotReference = SlotReference.of(player, getSlotType(i).name(), localIndex);
			ItemStack currentStack = slotReference.getStack();
			if (currentStack != null && !currentStack.isEmpty() && !resource.matches(currentStack)) {
				return 0;
			}

			int currentCount = currentStack != null ? currentStack.getCount() : 0;
			ItemStack resourceStack = resource.toStack();
			int countToInsert = Math.min(amount, getContainer(i).getAccessories().getMaxStackSize(resourceStack) - currentCount);

			if (countToInsert <= 0 || !getContainer(i).getAccessories().canPlaceItem(localIndex, resourceStack)) {
				return 0;
			}

			new SlotReferenceJournal(slotReference).updateSnapshots(transactionContext);

			slotReference.setStack(resource.toStack(countToInsert + currentCount));
			return countToInsert;
		}

		@Override
		public int extract(int i, ItemResource resource, int amount, TransactionContext transactionContext) {
			if (totalSize <= i) {
				return 0;
			}

			int localIndex = getLocalIndex(i);
			SlotReference slotReference = SlotReference.of(player, getSlotType(i).name(), localIndex);
			ItemStack currentStack = slotReference.getStack();
			if (currentStack == null || currentStack.isEmpty() || !resource.matches(currentStack)) {
				return 0;
			}

			int countToExtract = Math.min(amount, currentStack.getCount());

			new SlotReferenceJournal(slotReference).updateSnapshots(transactionContext);

			if (currentStack.getCount() == countToExtract) {
				slotReference.setStack(ItemStack.EMPTY);
			} else {
				slotReference.setStack(currentStack.copyWithCount(currentStack.getCount() - countToExtract));
			}
			return countToExtract;
		}

		private static class SlotReferenceJournal extends SnapshotJournal<ItemStack> {
			private final SlotReference slotReference;

			public SlotReferenceJournal(SlotReference slotReference) {
				this.slotReference = slotReference;
			}

			@Override
			protected ItemStack createSnapshot() {
				ItemStack stack = slotReference.getStack();
				return stack != null ? stack.copy() : ItemStack.EMPTY;
			}

			@Override
			protected void revertToSnapshot(ItemStack stack) {
				slotReference.setStack(stack);
			}
		}
	}
}
