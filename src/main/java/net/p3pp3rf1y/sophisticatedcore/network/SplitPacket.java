package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.FriendlyByteBuf;

public record SplitPacket(byte[] payload) {
	public static void encode(SplitPacket msg, FriendlyByteBuf buf) {
		buf.writeByteArray(msg.payload);
	}

	public static SplitPacket decode(FriendlyByteBuf buf) {
		return new SplitPacket(buf.readByteArray());
	}
}
