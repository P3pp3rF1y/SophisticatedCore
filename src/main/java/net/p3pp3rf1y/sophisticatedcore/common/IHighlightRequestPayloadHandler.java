package net.p3pp3rf1y.sophisticatedcore.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;

public interface IHighlightRequestPayloadHandler<T> {
	ResourceLocation id();

	HighlightResult compute(ServerPlayer player,
							ItemStackKey stackKey,
							T clientData);

	T decode(FriendlyByteBuf packetBuffer);

	void encode(FriendlyByteBuf packetBuffer, T value);

	record HighlightResult(int stackCounts, int itemCounts) {
	}
}
