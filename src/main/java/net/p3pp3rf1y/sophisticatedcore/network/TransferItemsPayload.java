package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.Set;

public record TransferItemsPayload(boolean transferToInventory,
								   boolean filterByContents) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TransferItemsPayload> TYPE = new CustomPacketPayload.Type<>(SophisticatedCore.getIdentifier("transfer_items"));
	public static final StreamCodec<ByteBuf, TransferItemsPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			TransferItemsPayload::transferToInventory,
			ByteBufCodecs.BOOL,
			TransferItemsPayload::filterByContents,
			TransferItemsPayload::new);

	public static void handlePayload(TransferItemsPayload payload, IPayloadContext context) {
		Player player = context.player();

		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> storageMenu)) {
			return;
		}
		IStorageWrapper storageWrapper = storageMenu.getStorageWrapper();
		if (payload.transferToInventory) {
			if (payload.filterByContents) {
				mergeToPlayersInventoryFiltered(player, storageWrapper);
			} else {
				mergeToPlayersInventory(storageWrapper, player);
			}
		} else {
			try(Transaction tx = Transaction.openRoot()) {
				FilteredStorageItemHandler filteredStorage = new FilteredStorageItemHandler(storageWrapper, payload.filterByContents);
				InventoryHelper.iteratePlayerInventory(player, (slot, stack) -> {
					if (slot < 9 || stack.isEmpty()) {
						return;
					}
					int moved = filteredStorage.insert(ItemResource.of(stack), stack.getCount(), tx);
					if (moved > 0) {
						player.getInventory().setItem(slot, stack.getCount() == moved ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved));
					}
				});
				tx.commit();
			}
		}
	}

	private static void mergeToPlayersInventory(IStorageWrapper storageWrapper, Player player) {
		InventoryHelper.iterate(storageWrapper.getInventoryHandler(), (slot, stack) -> {
			if (stack.isEmpty()) {
				return;
			}

			int moved = InventoryHelper.mergeIntoPlayerInventory(player, stack, 9);
			if (moved > 0) {
				storageWrapper.getInventoryHandler().setStackInSlot(slot, stack.getCount() == moved ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved));
			}
		}, () -> false, false);
	}

	private static void mergeToPlayersInventoryFiltered(Player player, IStorageWrapper storageWrapper) {
		Set<ItemStackKey> uniqueStacks = InventoryHelper.getUniqueStacks(PlayerInventoryWrapper.of(player).getMainSlots());
		InventoryHelper.iterate(storageWrapper.getInventoryHandler(), (slot, stack) -> {
			if (stack.isEmpty() || !uniqueStacks.contains(ItemStackKey.of(stack))) {
				return;
			}
			int moved = InventoryHelper.mergeIntoPlayerInventory(player, stack, 0);
			if (moved > 0) {
				storageWrapper.getInventoryHandler().setStackInSlot(slot, stack.getCount() == moved ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved));
			}
		}, () -> false, false);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private static class FilteredStorageItemHandler implements ResourceHandler<ItemResource> {
		protected final boolean matchContents;
		private final Set<ItemStackKey> uniqueStacks;
		private final IStorageWrapper storageWrapper;

		public FilteredStorageItemHandler(IStorageWrapper storageWrapper, boolean matchContents) {
			this.storageWrapper = storageWrapper;
			this.matchContents = matchContents;
			uniqueStacks = getUniqueStacks(storageWrapper.getInventoryHandler());
		}

		protected Set<ItemStackKey> getUniqueStacks(ITrackedContentsItemResourceHandler itemHandler) {
			return itemHandler.getTrackedStacks();
		}

		protected boolean matchesFilter(ItemResource resource) {
			return uniqueStacks.contains(ItemStackKey.of(resource)) || storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).matchesFilter(resource.getItem(), resource.getComponents());
		}

		@Override
		public int size() {
			return storageWrapper.getInventoryHandler().size();
		}

		@Override
		public ItemResource getResource(int i) {
			return storageWrapper.getInventoryHandler().getResource(i);
		}

		@Override
		public long getAmountAsLong(int i) {
			return storageWrapper.getInventoryHandler().getAmountAsLong(i);
		}

		@Override
		public int insert(ItemResource resource, int amount, TransactionContext transaction) {
			if (!matchContents || matchesFilter(resource)) {
				return storageWrapper.getInventoryHandler().insert(resource, amount, transaction);
			} else {
				return 0;
			}
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (!matchContents || matchesFilter(resource)) {
				return storageWrapper.getInventoryHandler().insert(index, resource, amount, transaction);
			} else {
				return 0;
			}
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			return storageWrapper.getInventoryHandler().extract(index, resource, amount, transaction);
		}

		@Override
		public long getCapacityAsLong(int i, ItemResource resource) {
			return storageWrapper.getInventoryHandler().getOverflowAwareCapacity(i, resource);
		}

		@Override
		public boolean isValid(int i, ItemResource resource) {
			return storageWrapper.getInventoryHandler().isValid(i, resource);
		}
	}
}
