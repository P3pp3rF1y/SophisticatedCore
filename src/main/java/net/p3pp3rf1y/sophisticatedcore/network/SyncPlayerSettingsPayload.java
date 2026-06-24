package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.settings.main.PlayerMainSettingsSavedData;

public record SyncPlayerSettingsPayload(String name, MainSettingsCategoryData data) implements CustomPacketPayload {
	public static final Type<SyncPlayerSettingsPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("sync_player_settings"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerSettingsPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8,
			SyncPlayerSettingsPayload::name, MainSettingsCategoryData.STREAM_CODEC, SyncPlayerSettingsPayload::data, SyncPlayerSettingsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncPlayerSettingsPayload payload, IPayloadContext context) {
		PlayerMainSettingsSavedData.get().put(context.player().getUUID(), payload.name, payload.data);
	}
}
