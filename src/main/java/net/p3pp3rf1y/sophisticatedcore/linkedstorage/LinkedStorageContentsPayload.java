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
	private static final StreamCodec<RegistryFriendlyByteBuf, SnapshotSize> SNAPSHOT_SIZE_STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,
			SnapshotSize::inventorySlots, ByteBufCodecs.VAR_INT, SnapshotSize::upgradeSlots, ByteBufCodecs.VAR_INT, SnapshotSize::columnsTaken,
			SnapshotSize::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, LinkedStorageContentsPayload> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC,
			LinkedStorageContentsPayload::groupId, ByteBufCodecs.VAR_LONG, LinkedStorageContentsPayload::revision, ContainerContents.STREAM_CODEC,
			LinkedStorageContentsPayload::contents, ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC, LinkedStorageContentsPayload::groupName,
			SNAPSHOT_SIZE_STREAM_CODEC, LinkedStorageContentsPayload::snapshotSize, LinkedStorageContentsPayload::fromStream);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private SnapshotSize snapshotSize() {
		return new SnapshotSize(inventorySlots, upgradeSlots, columnsTaken);
	}

	private static LinkedStorageContentsPayload fromStream(UUID groupId, long revision, ContainerContents contents, Component groupName,
			SnapshotSize snapshotSize) {
		return new LinkedStorageContentsPayload(groupId, revision, contents, groupName, snapshotSize.inventorySlots(), snapshotSize.upgradeSlots(),
				snapshotSize.columnsTaken());
	}

	public static LinkedStorageContentsPayload createSnapshot(ServerLevel level, UUID groupId) {
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		LinkedStorageSnapshotProfile profile = manager.resolveVirtualHost(groupId).flatMap(ILinkedStorageVirtualHost::getLinkedStorageSnapshotProfile)
				.orElseThrow(() -> new IllegalStateException("Linked storage group " + groupId + " does not provide a snapshot profile"));
		ContainerContents contents = manager.resolveContents(groupId).orElseThrow(() -> new IllegalStateException("Unknown linked storage group " + groupId))
				.contents().copy();
		return new LinkedStorageContentsPayload(groupId, manager.getRevision(groupId), contents, profile.groupName(), profile.inventorySlots(),
				profile.upgradeSlots(), profile.columnsTaken());
	}

	public static void handlePayload(LinkedStorageContentsPayload payload, IPayloadContext context) {
		ClientLinkedStorageContents.updateContents(payload.groupId, payload.revision, payload.contents, payload.groupName, payload.inventorySlots,
				payload.upgradeSlots, payload.columnsTaken);
		ClientStorageContentsTooltipBase.refreshContents();
	}

	private record SnapshotSize(int inventorySlots, int upgradeSlots, int columnsTaken) {
	}
}
