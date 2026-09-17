package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointAccessProviders;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;

import java.util.UUID;
import java.util.function.Supplier;

public record RequestLinkedStorageContentsMessage(UUID groupId, long knownRevision) {
	public static void encode(RequestLinkedStorageContentsMessage message, FriendlyByteBuf buffer) {
		buffer.writeUUID(message.groupId);
		buffer.writeVarLong(message.knownRevision);
	}

	public static RequestLinkedStorageContentsMessage decode(FriendlyByteBuf buffer) {
		return new RequestLinkedStorageContentsMessage(buffer.readUUID(), buffer.readVarLong());
	}

	public static void onMessage(RequestLinkedStorageContentsMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) {
				return;
			}

			LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(player.serverLevel()).manager();
			if (LinkedStorageEndpointAccessProviders.hasGroupEndpoint(player, manager, message.groupId)
					&& manager.getRevision(message.groupId) != message.knownRevision) {
				PacketHandler.INSTANCE.sendToClient(player, LinkedStorageContentsMessage.createSnapshot(player.serverLevel(), message.groupId));
			}
		});
		context.setPacketHandled(true);
	}
}
