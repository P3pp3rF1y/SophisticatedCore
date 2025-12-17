
package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.render.ItemInStorageHighlightRenderer;

import java.util.List;

public record SyncItemHighlightsPayload(List<BlockPos> stackPositions, List<BlockPos> itemPositions, List<BlockPos> emptyTargetPositions) implements CustomPacketPayload {
	public static final Type<SyncItemHighlightsPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("sync_item_highlights"));
	public static final StreamCodec<ByteBuf, SyncItemHighlightsPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
			SyncItemHighlightsPayload::stackPositions,
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
			SyncItemHighlightsPayload::itemPositions,
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
			SyncItemHighlightsPayload::emptyTargetPositions,
			SyncItemHighlightsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncItemHighlightsPayload payload, IPayloadContext context) {
		ItemInStorageHighlightRenderer.setHighlightedPositions(payload.stackPositions(), payload.itemPositions, payload.emptyTargetPositions);
	}
}
