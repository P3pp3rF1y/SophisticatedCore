package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;

import java.util.UUID;

public record LinkedStorageSettingsPayload(UUID groupId, ContainerContents.SettingsData settings) implements CustomPacketPayload {
	public static final Type<LinkedStorageSettingsPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("linked_storage_settings"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LinkedStorageSettingsPayload> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC,
			LinkedStorageSettingsPayload::groupId, ContainerContents.SettingsData.STREAM_CODEC, LinkedStorageSettingsPayload::settings,
			LinkedStorageSettingsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(LinkedStorageSettingsPayload payload, IPayloadContext context) {
		ClientLinkedStorageContents.updateSettings(payload.groupId, payload.settings);
	}
}
