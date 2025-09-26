package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record RequestItemHighlightsMessage(ItemStack stack,
										   List<BlockPos> storagePositions) {
	public static void encode(RequestItemHighlightsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeItemStack(msg.stack(), false);
		packetBuffer.writeCollection(msg.storagePositions(), FriendlyByteBuf::writeBlockPos);
	}

	public static RequestItemHighlightsMessage decode(FriendlyByteBuf packetBuffer) {
		return new RequestItemHighlightsMessage(packetBuffer.readItem(), packetBuffer.readList(FriendlyByteBuf::readBlockPos));
	}

	static void onMessage(RequestItemHighlightsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(@Nullable ServerPlayer player, RequestItemHighlightsMessage msg) {
		if (player == null) {
			return;
		}
		ItemStackKey stackKey = ItemStackKey.of(msg.stack());
		Level level = player.level();

		List<BlockPos> stackPositions = new ArrayList<>();
		List<BlockPos> itemPositions = new ArrayList<>();
		msg.storagePositions().forEach(pos -> {
			WorldHelper.getLoadedBlockEntity(level, pos, IControllableStorage.class).ifPresent(storage -> {
				ISlotTracker slotTracker = storage.getStorageWrapper().getInventoryHandler().getSlotTracker();
				if (slotTracker.getPartialStacks().contains(stackKey) || slotTracker.getFullStacks().contains(stackKey)) {
					stackPositions.add(pos);
				} else if (slotTracker.getItems().contains(msg.stack().getItem())) {
					itemPositions.add(pos);
				}
			});
		});
		PacketHandler.INSTANCE.sendToClient(player, new SyncItemHighlightsMessage(stackPositions, itemPositions, List.of()));
	}
}
