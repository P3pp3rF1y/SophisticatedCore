
package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.client.render.ItemInStorageHighlightRenderer;

import java.util.List;
import java.util.function.Supplier;

public record SyncItemHighlightsMessage(List<BlockPos> stackPositions, List<BlockPos> itemPositions) {
	public static void encode(SyncItemHighlightsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeCollection(msg.stackPositions, FriendlyByteBuf::writeBlockPos);
		packetBuffer.writeCollection(msg.itemPositions, FriendlyByteBuf::writeBlockPos);
	}

	public static SyncItemHighlightsMessage decode(FriendlyByteBuf packetBuffer) {
		return new SyncItemHighlightsMessage(packetBuffer.readList(FriendlyByteBuf::readBlockPos), packetBuffer.readList(FriendlyByteBuf::readBlockPos));
	}

	static void onMessage(SyncItemHighlightsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	public static void handleMessage(SyncItemHighlightsMessage msg) {
		ItemInStorageHighlightRenderer.setHighlightedPositions(msg.stackPositions(), msg.itemPositions);
	}
}
