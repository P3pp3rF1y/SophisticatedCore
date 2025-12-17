package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.UUID;

public record SoundFinishedNotificationPayload(UUID storageUuid) implements CustomPacketPayload {
	public static final Type<SoundFinishedNotificationPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("sound_finished_notification"));
	public static final StreamCodec<ByteBuf, SoundFinishedNotificationPayload> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC,
			SoundFinishedNotificationPayload::storageUuid,
			SoundFinishedNotificationPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SoundFinishedNotificationPayload payload, IPayloadContext context) {
		ServerStorageSoundHandler.onSoundFinished(context.player().level(), payload.storageUuid);
	}
}
