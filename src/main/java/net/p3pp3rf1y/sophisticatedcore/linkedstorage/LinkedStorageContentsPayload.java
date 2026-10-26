package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
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
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;

import java.util.UUID;

public record LinkedStorageContentsPayload(UUID groupId, long revision, ContainerContents contents, Component groupName, int inventorySlots, int upgradeSlots,
		int columnsTaken, SortBy sortBy, CompoundTag hostDescriptor) implements CustomPacketPayload {
	public LinkedStorageContentsPayload(UUID groupId, long revision, ContainerContents contents, Component groupName, int inventorySlots, int upgradeSlots,
			int columnsTaken) {
		this(groupId, revision, contents, groupName, inventorySlots, upgradeSlots, columnsTaken, SortBy.NAME, new CompoundTag());
	}
	public static final Type<LinkedStorageContentsPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("linked_storage_contents"));
	private static final StreamCodec<RegistryFriendlyByteBuf, SnapshotSize> SNAPSHOT_SIZE_STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,
			SnapshotSize::inventorySlots, ByteBufCodecs.VAR_INT, SnapshotSize::upgradeSlots, ByteBufCodecs.VAR_INT, SnapshotSize::columnsTaken,
			SortBy.STREAM_CODEC, SnapshotSize::sortBy, ByteBufCodecs.COMPOUND_TAG, SnapshotSize::hostDescriptor, SnapshotSize::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, LinkedStorageContentsPayload> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC,
			LinkedStorageContentsPayload::groupId, ByteBufCodecs.VAR_LONG, LinkedStorageContentsPayload::revision, ContainerContents.STREAM_CODEC,
			LinkedStorageContentsPayload::contents, ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC, LinkedStorageContentsPayload::groupName,
			SNAPSHOT_SIZE_STREAM_CODEC, LinkedStorageContentsPayload::snapshotSize, LinkedStorageContentsPayload::fromStream);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private SnapshotSize snapshotSize() {
		return new SnapshotSize(inventorySlots, upgradeSlots, columnsTaken, sortBy, hostDescriptor);
	}

	private static LinkedStorageContentsPayload fromStream(UUID groupId, long revision, ContainerContents contents, Component groupName,
			SnapshotSize snapshotSize) {
		return new LinkedStorageContentsPayload(groupId, revision, contents, groupName, snapshotSize.inventorySlots(), snapshotSize.upgradeSlots(),
				snapshotSize.columnsTaken(), snapshotSize.sortBy(), snapshotSize.hostDescriptor());
	}

	public static LinkedStorageContentsPayload createSnapshot(ServerLevel level, UUID groupId) {
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		LinkedStorageSnapshotProfile profile = manager.resolveVirtualHost(groupId).flatMap(ILinkedStorageVirtualHost::getLinkedStorageSnapshotProfile)
				.orElseThrow(() -> new IllegalStateException("Linked storage group " + groupId + " does not provide a snapshot profile"));
		ILinkedStorageContents contents = manager.resolveContents(groupId)
				.orElseThrow(() -> new IllegalStateException("Unknown linked storage group " + groupId));
		LinkedStorageHostDescriptor descriptor = manager.getHostDescriptor(groupId).orElseThrow();
		CompoundTag hostDescriptor = new CompoundTag();
		hostDescriptor.putString(LinkedStorageHostDescriptor.FACTORY_ID_TAG, descriptor.factoryId().toString());
		hostDescriptor.put(LinkedStorageHostDescriptor.VIRTUAL_CARRIER_TAG,
				manager.resolveVirtualHost(groupId).flatMap(ILinkedStorageVirtualHost::getVirtualCarrierSnapshot).orElseGet(descriptor::virtualCarrier));
		return new LinkedStorageContentsPayload(groupId, manager.getRevision(groupId), contents.contents().copy(), profile.groupName(),
				profile.inventorySlots(), profile.upgradeSlots(), profile.columnsTaken(), contents.getSortBy(), hostDescriptor);
	}

	public static void handlePayload(LinkedStorageContentsPayload payload, IPayloadContext context) {
		ClientLinkedStorageContents.updateHostDescriptor(payload.groupId, payload.hostDescriptor);
		ClientLinkedStorageContents.updateContents(payload.groupId, payload.revision, payload.contents, payload.groupName, payload.inventorySlots,
				payload.upgradeSlots, payload.columnsTaken, payload.sortBy);
		if (context.player().containerMenu instanceof StorageContainerMenuBase<?> menu) {
			menu.detectSettingsChangeAndReload();
		}
		ClientStorageContentsTooltipBase.refreshContents();
	}

	private record SnapshotSize(int inventorySlots, int upgradeSlots, int columnsTaken, SortBy sortBy, CompoundTag hostDescriptor) {
	}
}
