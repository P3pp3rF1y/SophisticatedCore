package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.UUID;

public record PlayDiscPayload(boolean blockStorage, UUID storageUuid, Holder<JukeboxSong> song, int entityId, BlockPos pos) implements CustomPacketPayload {
	public static final Type<PlayDiscPayload> TYPE = new Type<>(SophisticatedCore.getRL("play_disc"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PlayDiscPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			PlayDiscPayload::blockStorage,
			UUIDUtil.STREAM_CODEC,
			PlayDiscPayload::storageUuid,
			JukeboxSong.STREAM_CODEC,
			PlayDiscPayload::song,
			ByteBufCodecs.INT,
			PlayDiscPayload::entityId,
			BlockPos.STREAM_CODEC,
			PlayDiscPayload::pos,
			PlayDiscPayload::new);

	public PlayDiscPayload(UUID storageUuid, Holder<JukeboxSong> song, BlockPos pos) {
		this(true, storageUuid, song, 0, pos);
	}

	public PlayDiscPayload(UUID storageUuid, Holder<JukeboxSong> song, int entityId) {
		this(false, storageUuid, song, entityId, BlockPos.ZERO);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(PlayDiscPayload payload, IPayloadContext context) {
		SoundEvent soundEvent = payload.song().value().soundEvent().value();
		if (payload.blockStorage) {
			StorageSoundHandler.playStorageSound(soundEvent, payload.storageUuid, payload.pos);
		} else {
			StorageSoundHandler.playStorageSound(soundEvent, payload.storageUuid, payload.entityId);
		}
	}
}
