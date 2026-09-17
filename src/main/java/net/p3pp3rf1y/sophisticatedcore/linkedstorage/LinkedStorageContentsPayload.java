package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.render.ClientStorageContentsTooltipBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;

import java.util.UUID;

public record LinkedStorageContentsPayload(UUID groupId, long revision, ContainerContents contents, Component groupName, int inventorySlots, int upgradeSlots,
		int columnsTaken) implements CustomPacketPayload {
	public static final Type<LinkedStorageContentsPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("linked_storage_contents"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LinkedStorageContentsPayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> {
		UUIDUtil.STREAM_CODEC.encode(buffer, payload.groupId);
		ByteBufCodecs.VAR_LONG.encode(buffer, payload.revision);
		ContainerContents.STREAM_CODEC.encode(buffer, payload.contents);
		ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buffer, payload.groupName);
		ByteBufCodecs.VAR_INT.encode(buffer, payload.inventorySlots);
		ByteBufCodecs.VAR_INT.encode(buffer, payload.upgradeSlots);
		ByteBufCodecs.VAR_INT.encode(buffer, payload.columnsTaken);
	}, buffer -> fromStream(UUIDUtil.STREAM_CODEC.decode(buffer), ByteBufCodecs.VAR_LONG.decode(buffer), ContainerContents.STREAM_CODEC.decode(buffer),
			ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
			ByteBufCodecs.VAR_INT.decode(buffer)));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private static LinkedStorageContentsPayload fromStream(UUID groupId, long revision, ContainerContents contents, Component groupName, int inventorySlots,
			int upgradeSlots, int columnsTaken) {
		if (inventorySlots < 0 || upgradeSlots < 0 || columnsTaken < 0) {
			throw new IllegalArgumentException("Linked storage snapshot dimensions must be non-negative");
		}
		return new LinkedStorageContentsPayload(groupId, revision, contents, groupName, inventorySlots, upgradeSlots, columnsTaken);
	}

	public static LinkedStorageContentsPayload createSnapshot(ServerLevel level, UUID groupId) {
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		LinkedStorageSnapshotProfile profile = manager.resolveVirtualHost(groupId).flatMap(ILinkedStorageVirtualHost::getLinkedStorageSnapshotProfile)
				.orElseThrow(() -> new IllegalStateException("Linked storage group " + groupId + " does not provide a snapshot profile"));
		ContainerContents contents = manager.resolveContents(groupId).orElseThrow(() -> new IllegalStateException("Unknown linked storage group " + groupId))
				.getContents(groupId).copy();
		return new LinkedStorageContentsPayload(groupId, manager.getRevision(groupId), contents, profile.groupName(), profile.inventorySlots(),
				profile.upgradeSlots(), profile.columnsTaken());
	}

	public static void handlePayload(LinkedStorageContentsPayload payload, IPayloadContext context) {
		ClientLinkedStorageContents.updateContents(payload.groupId, payload.revision, payload.contents, payload.groupName, payload.inventorySlots,
				payload.upgradeSlots, payload.columnsTaken);
		ClientStorageContentsTooltipBase.refreshContents();
	}
}
