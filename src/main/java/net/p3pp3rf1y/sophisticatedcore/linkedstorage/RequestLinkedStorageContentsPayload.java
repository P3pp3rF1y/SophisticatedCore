package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.UUID;

public record RequestLinkedStorageContentsPayload(UUID groupId, long knownRevision) implements CustomPacketPayload {
	public static final Type<RequestLinkedStorageContentsPayload> TYPE = new Type<>(SophisticatedCore.getRL("request_linked_storage_contents"));
	public static final StreamCodec<ByteBuf, RequestLinkedStorageContentsPayload> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC,
			RequestLinkedStorageContentsPayload::groupId, ByteBufCodecs.VAR_LONG, RequestLinkedStorageContentsPayload::knownRevision,
			RequestLinkedStorageContentsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RequestLinkedStorageContentsPayload payload, IPayloadContext context) {
		if (!(context.player() instanceof ServerPlayer player)) {
			return;
		}

		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(player.serverLevel()).manager();
		if (!LinkedStorageEndpointAccessProviders.hasGroupEndpoint(player, manager, payload.groupId)) {
			return;
		}
		if (manager.getRevision(payload.groupId) != payload.knownRevision) {
			PacketDistributor.sendToPlayer(player, LinkedStorageContentsPayload.createSnapshot(player.serverLevel(), payload.groupId));
		}
	}
}
