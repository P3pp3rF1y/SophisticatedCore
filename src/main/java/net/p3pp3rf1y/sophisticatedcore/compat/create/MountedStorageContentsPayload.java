package net.p3pp3rf1y.sophisticatedcore.compat.create;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.UUID;

public record MountedStorageContentsPayload(UUID storageUuid, CompoundTag contents) implements CustomPacketPayload {
	public static final Type<MountedStorageContentsPayload> TYPE = new Type<>(SophisticatedCore.getRL("mounted_storage_contents"));
	public static final StreamCodec<ByteBuf, MountedStorageContentsPayload> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC,
			MountedStorageContentsPayload::storageUuid, ByteBufCodecs.COMPOUND_TAG, MountedStorageContentsPayload::contents,
			MountedStorageContentsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(MountedStorageContentsPayload payload, IPayloadContext context) {
		MountedStorageData.get().setContentsClient(payload.storageUuid, payload.contents);
	}
}
