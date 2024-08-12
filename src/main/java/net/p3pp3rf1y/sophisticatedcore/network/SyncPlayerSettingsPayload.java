package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsManager;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import javax.annotation.Nullable;

public record SyncPlayerSettingsPayload(String playerTagName,
										@Nullable CompoundTag settingsNbt) implements CustomPacketPayload {
	public static final Type<SyncPlayerSettingsPayload> TYPE = new Type<>(SophisticatedCore.getRL("sync_player_settings"));
	public static final StreamCodec<ByteBuf, SyncPlayerSettingsPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			SyncPlayerSettingsPayload::playerTagName,
			StreamCodecHelper.ofNullable(ByteBufCodecs.COMPOUND_TAG),
			SyncPlayerSettingsPayload::settingsNbt,
			SyncPlayerSettingsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncPlayerSettingsPayload payload, IPayloadContext context) {
		if (payload.settingsNbt == null) {
			return;
		}
		SettingsManager.setPlayerSettingsTag(context.player(), payload.playerTagName, payload.settingsNbt);
	}
}
