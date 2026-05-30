package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class TransferItemsMessage {
	private final boolean transferToInventory;
	private final boolean filterByContents;

	public TransferItemsMessage(boolean transferToInventory, boolean filterByContents) {
		this.transferToInventory = transferToInventory;
		this.filterByContents = filterByContents;
	}

	public static TransferItemsMessage decode(FriendlyByteBuf packetBuffer) {
		return new TransferItemsMessage(packetBuffer.readBoolean(), packetBuffer.readBoolean());
	}

	public static void encode(TransferItemsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeBoolean(msg.transferToInventory);
		packetBuffer.writeBoolean(msg.filterByContents);
	}

	public static void onMessage(TransferItemsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(ServerPlayer player, TransferItemsMessage msg) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> storageMenu)) {
			return;
		}
		IStorageWrapper storageWrapper = storageMenu.getStorageWrapper();
		if (msg.transferToInventory) {
			if (msg.filterByContents) {
				mergeToPlayersInventoryFiltered(player, storageWrapper);
			} else {
				mergeToPlayersInventory(storageWrapper, player);
			}
		} else {
			InventoryHelper.transfer(new PlayerMainInvWithoutHotbarWrapper(player.getInventory()), new FilteredStorageItemHandler(storageWrapper, msg.filterByContents), s -> {});
		}
	}

	private static void mergeToPlayersInventory(IStorageWrapper storageWrapper, Player player) {
		InventoryHelper.iterate(storageWrapper.getInventoryHandler(), (slot, stack) -> {
			if (stack.isEmpty() || !storageWrapper.getInventoryHandler().isSlotAccessible(slot)) {
				return;
			}

			ItemStack result = InventoryHelper.mergeIntoPlayerInventory(player, stack, 9);
			if (result.getCount() != stack.getCount()) {
				storageWrapper.getInventoryHandler().setStackInSlot(slot, result);
			}
		});
	}

	private static void mergeToPlayersInventoryFiltered(Player player, IStorageWrapper storageWrapper) {
		Set<ItemStackKey> uniqueStacks = InventoryHelper.getUniqueStacks(new PlayerMainInvWrapper(player.getInventory()));
		InventoryHelper.iterate(storageWrapper.getInventoryHandler(), (slot, stack) -> {
			if (stack.isEmpty() || !storageWrapper.getInventoryHandler().isSlotAccessible(slot) || !uniqueStacks.contains(ItemStackKey.of(stack))) {
				return;
			}
			ItemStack result = InventoryHelper.mergeIntoPlayerInventory(player, stack, 0);
			if (result.getCount() != stack.getCount()) {
				storageWrapper.getInventoryHandler().setStackInSlot(slot, result);
			}
		});
	}

	public boolean transferToInventory() {
		return transferToInventory;
	}

	public boolean filterByContents() {
		return filterByContents;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (TransferItemsMessage) obj;
		return this.transferToInventory == that.transferToInventory &&
				this.filterByContents == that.filterByContents;
	}

	@Override
	public int hashCode() {
		return Objects.hash(transferToInventory, filterByContents);
	}

	@Override
	public String toString() {
		return "TransferItemsMessage[" +
				"transferToInventory=" + transferToInventory + ", " +
				"filterByContents=" + filterByContents + ']';
	}


	private static class PlayerMainInvWithoutHotbarWrapper extends InvWrapper {
		private final int minSlot;
		private final int maxSlot;

		private final Inventory inventoryPlayer;

		public PlayerMainInvWithoutHotbarWrapper(Inventory inv) {
			super(inv);
			this.inventoryPlayer = inv;
			this.minSlot = 9;
			this.maxSlot = inv.items.size();
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			ItemStack rest = super.insertItem(slot, stack, simulate);
			if (rest.getCount() != stack.getCount()) {
				ItemStack inSlot = this.getStackInSlot(slot);
				if (!inSlot.isEmpty()) {
					if (inventoryPlayer.player.level().isClientSide) {
						inSlot.setPopTime(5);
					} else if (inventoryPlayer.player instanceof ServerPlayer) {
						inventoryPlayer.player.containerMenu.broadcastChanges();
					}
				}
			}

			return rest;
		}

		@Override
		public int getSlots() {
			return this.maxSlot - this.minSlot;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return this.checkSlot(slot) ? super.getStackInSlot(slot + this.minSlot) : ItemStack.EMPTY;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return this.checkSlot(slot) ? super.extractItem(slot + this.minSlot, amount, simulate) : ItemStack.EMPTY;
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			if (this.checkSlot(slot)) {
				super.setStackInSlot(slot + this.minSlot, stack);
			}

		}

		@Override
		public int getSlotLimit(int slot) {
			return this.checkSlot(slot) ? super.getSlotLimit(slot + this.minSlot) : 0;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return this.checkSlot(slot) && super.isItemValid(slot + this.minSlot, stack);
		}

		private boolean checkSlot(int localSlot) {
			return localSlot + this.minSlot < this.maxSlot;
		}
	}

	private static class FilteredStorageItemHandler extends FilteredItemHandler<ITrackedContentsItemHandler> implements IItemHandlerSimpleInserter {
		private final IStorageWrapper storageWrapper;

		public FilteredStorageItemHandler(IStorageWrapper storageWrapper, boolean smart) {
			super(storageWrapper.getInventoryHandler(), smart);
			this.storageWrapper = storageWrapper;
		}

		@Override
		protected Set<ItemStackKey> getUniqueStacks(ITrackedContentsItemHandler itemHandler) {
			return itemHandler.getTrackedStacks();
		}

		@Override
		protected boolean matchesFilter(ItemStack stack) {
			return super.matchesFilter(stack) || storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).matchesFilter(stack);
		}

		@Nonnull
		@Override
		public ItemStack insertItem(ItemStack stack, boolean simulate) {
			if (!matchContents || matchesFilter(stack)) {
				return itemHandler.insertItem(stack, simulate);
			} else {
				return stack;
			}
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			itemHandler.setStackInSlot(slot, stack);
		}
	}

	private static class FilteredItemHandler<T extends IItemHandler> implements IItemHandler {
		protected final T itemHandler;
		protected final boolean matchContents;
		private final Set<ItemStackKey> uniqueStacks;

		public FilteredItemHandler(T itemHandler, boolean matchContents) {
			this.itemHandler = itemHandler;
			this.matchContents = matchContents;
			uniqueStacks = getUniqueStacks(itemHandler);
		}

		protected Set<ItemStackKey> getUniqueStacks(T itemHandler) {
			return InventoryHelper.getUniqueStacks(itemHandler);
		}

		@Override
		public int getSlots() {
			return itemHandler.getSlots();
		}

		@Nonnull
		@Override
		public ItemStack getStackInSlot(int slot) {
			return itemHandler.getStackInSlot(slot);
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (!matchContents || matchesFilter(stack)) {
				return itemHandler.insertItem(slot, stack, simulate);
			} else {
				return stack;
			}
		}

		protected boolean matchesFilter(ItemStack stack) {
			return uniqueStacks.contains(ItemStackKey.of(stack));
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return itemHandler.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return itemHandler.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return itemHandler.isItemValid(slot, stack);
		}
	}
}
