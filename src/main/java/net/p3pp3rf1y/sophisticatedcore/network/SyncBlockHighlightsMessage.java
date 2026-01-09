
package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderer;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record SyncBlockHighlightsMessage(Map<Integer, List<BlockPos>> highlightPositions) {
	public static void encode(SyncBlockHighlightsMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeMap(msg.highlightPositions, FriendlyByteBuf::writeInt, (buf, list) -> buf.writeCollection(list, FriendlyByteBuf::writeBlockPos));
	}

	public static SyncBlockHighlightsMessage decode(FriendlyByteBuf packetBuffer) {
		return new SyncBlockHighlightsMessage(packetBuffer.readMap(FriendlyByteBuf::readInt, buf -> buf.readList(FriendlyByteBuf::readBlockPos)));
	}

	static void onMessage(SyncBlockHighlightsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	public static void handleMessage(SyncBlockHighlightsMessage payload) {
		BlockHighlightRenderer.addHighlightedPositions(payload.highlightPositions());
	}
}
