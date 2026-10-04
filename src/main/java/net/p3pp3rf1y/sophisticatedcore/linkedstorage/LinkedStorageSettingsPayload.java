package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.UUID;

public record LinkedStorageSettingsPayload(UUID groupId, CompoundTag settings) implements CustomPacketPayload {
	public static final Type<LinkedStorageSettingsPayload> TYPE = new Type<>(SophisticatedCore.getRL("linked_storage_settings"));
	public static final StreamCodec<ByteBuf, LinkedStorageSettingsPayload> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC,
			LinkedStorageSettingsPayload::groupId, ByteBufCodecs.COMPOUND_TAG, LinkedStorageSettingsPayload::settings, LinkedStorageSettingsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(LinkedStorageSettingsPayload payload, IPayloadContext context) {
		ClientLinkedStorageContents.updateSettings(payload.groupId, payload.settings);
	}
}
