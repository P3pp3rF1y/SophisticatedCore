package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.UUID;

public record SoundStopNotificationPayload(UUID storageUuid) implements CustomPacketPayload {
	public static final Type<SoundStopNotificationPayload> TYPE = new Type<>(SophisticatedCore.getRL("sound_stop_notification"));
	public static final StreamCodec<ByteBuf, SoundStopNotificationPayload> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC,
			SoundStopNotificationPayload::storageUuid,
			SoundStopNotificationPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SoundStopNotificationPayload payload, IPayloadContext context) {
		ServerStorageSoundHandler.onSoundStopped(context.player().level(), payload.storageUuid);
	}
}
