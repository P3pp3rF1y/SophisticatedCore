
package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderer;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SyncBlockHighlightsPayload(Map<Integer, List<List<BlockPos>>> highlightPositions, int durationTicks) implements CustomPacketPayload {
	public static final Type<SyncBlockHighlightsPayload> TYPE = new Type<>(SophisticatedCore.getRL("sync_block_highlights"));
	public static final StreamCodec<ByteBuf, SyncBlockHighlightsPayload> STREAM_CODEC = StreamCodec.composite(
			StreamCodecHelper.ofMap(ByteBufCodecs.INT, BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()), HashMap::new),
			SyncBlockHighlightsPayload::highlightPositions, ByteBufCodecs.INT, SyncBlockHighlightsPayload::durationTicks, SyncBlockHighlightsPayload::new);

	public SyncBlockHighlightsPayload(Map<Integer, List<List<BlockPos>>> highlightPositions) {
		this(highlightPositions, BlockHighlightRenderer.HIGHLIGHT_DURATION);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncBlockHighlightsPayload payload, IPayloadContext context) {
		BlockHighlightRenderer.addHighlightedPositions(payload.highlightPositions(), payload.durationTicks());
	}
}
