package net.p3pp3rf1y.sophisticatedcore.compat.quark;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public class TransferMessage {
	private final boolean isRestock;
	private final boolean smartTransfer;

	public TransferMessage(boolean isRestock, boolean smartTransfer) {
		this.isRestock = isRestock;
		this.smartTransfer = smartTransfer;
	}

	public static void encode(TransferMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeBoolean(msg.isRestock);
		packetBuffer.writeBoolean(msg.smartTransfer);
	}

	public static TransferMessage decode(FriendlyByteBuf packetBuffer) {
		return new TransferMessage(packetBuffer.readBoolean(), packetBuffer.readBoolean());
	}

	static void onMessage(TransferMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(@Nullable ServerPlayer player, TransferMessage msg) {
		if (player == null || !(player.containerMenu instanceof StorageContainerMenuBase<?> storageMenu)) {
			return;
		}
		IStorageWrapper storageWrapper = storageMenu.getStorageWrapper();
		if (msg.isRestock) {
			player.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(playerInv ->
					InventoryHelper.transfer(storageWrapper.getInventoryHandler(), new FilteredItemHandler<>(playerInv, msg.smartTransfer), s -> {}));
		} else {
			Inventory inv = player.getInventory();
			FilteredStorageItemHandler targetInventory = new FilteredStorageItemHandler(storageWrapper, msg.smartTransfer);
			for (int i = Inventory.getSelectionSize(); i < inv.items.size(); i++) {
				ItemStack stackAt = inv.getItem(i);
				if (!stackAt.isEmpty()) {
					inv.setItem(i, targetInventory.insertItem(stackAt, false));
				}
			}
		}
	}

	private static class FilteredStorageItemHandler extends FilteredItemHandler<ITrackedContentsItemHandler> implements IItemHandlerSimpleInserter {
		private final IStorageWrapper storageWrapper;

		public FilteredStorageItemHandler(IStorageWrapper storageWrapper, boolean smart) {
			super(storageWrapper.getInventoryHandler(), smart);
			this.storageWrapper = storageWrapper;
		}

		@Override
		protected boolean matchesFilter(ItemStack stack) {
			return super.matchesFilter(stack) || storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getFilterItemSlots().containsKey(stack.getItem());
		}

		@Nonnull
		@Override
		public ItemStack insertItem(ItemStack stack, boolean simulate) {
			if (!smart || matchesFilter(stack)) {
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
		protected final boolean smart;

		public FilteredItemHandler(T itemHandler, boolean smart) {
			this.itemHandler = itemHandler;
			this.smart = smart;
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
			if (!smart || matchesFilter(stack)) {
				return itemHandler.insertItem(slot, stack, simulate);
			} else {
				return stack;
			}
		}

		protected boolean matchesFilter(ItemStack stack) {
			int slots = getSlots();

			for (int i = 0; i < slots; ++i) {
				ItemStack stackAt = getStackInSlot(i);
				if (!stackAt.isEmpty()) {
					boolean itemEqual = stack.getItem() == stackAt.getItem();
					boolean damageEqual = stack.getDamageValue() == stackAt.getDamageValue();
					boolean nbtEqual = ItemStack.tagMatches(stackAt, stack);
					if (itemEqual && damageEqual && nbtEqual) {
						return true;
					}

					if (stack.isDamageableItem() && stack.getMaxStackSize() == 1 && itemEqual && nbtEqual) {
						return true;
					}
				}
			}

			return false;
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
