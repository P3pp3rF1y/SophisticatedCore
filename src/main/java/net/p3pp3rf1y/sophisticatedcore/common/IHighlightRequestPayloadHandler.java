package net.p3pp3rf1y.sophisticatedcore.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;

public interface IHighlightRequestPayloadHandler<T> {
	ResourceLocation id();

	StreamCodec<ByteBuf, T> requestCodec();

	HighlightResult compute(ServerPlayer player,
							ItemStackKey stackKey,
							T clientData);

	record HighlightResult(int stackCounts, int itemCounts) {
	}
}
