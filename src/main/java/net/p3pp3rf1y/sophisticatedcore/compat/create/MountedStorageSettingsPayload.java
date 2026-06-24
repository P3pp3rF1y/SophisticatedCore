package net.p3pp3rf1y.sophisticatedcore.compat.create;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;

import java.util.UUID;

public record MountedStorageSettingsPayload(UUID storageUuid, ContainerContents.SettingsData settings) implements CustomPacketPayload {
	public static final Type<MountedStorageSettingsPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("mounted_storage_settings"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MountedStorageSettingsPayload> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC,
			MountedStorageSettingsPayload::storageUuid, ContainerContents.SettingsData.STREAM_CODEC, MountedStorageSettingsPayload::settings,
			MountedStorageSettingsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(MountedStorageSettingsPayload payload, IPayloadContext context) {
		MountedStorageData.get().getContents(payload.storageUuid).settings().reloadFrom(payload.settings);
	}
}
