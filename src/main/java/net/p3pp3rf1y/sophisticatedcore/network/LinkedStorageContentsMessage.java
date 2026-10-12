package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.client.render.ClientStorageContentsTooltipBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageVirtualHost;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageSnapshotProfile;

import java.util.UUID;
import java.util.function.Supplier;

public record LinkedStorageContentsMessage(UUID groupId, long revision, CompoundTag contents, Component groupName, int inventorySlots, int upgradeSlots,
		int columnsTaken, CompoundTag hostDescriptor) {
	public LinkedStorageContentsMessage(UUID groupId, long revision, CompoundTag contents, Component groupName, int inventorySlots, int upgradeSlots,
			int columnsTaken) {
		this(groupId, revision, contents, groupName, inventorySlots, upgradeSlots, columnsTaken, new CompoundTag());
	}

	public static void encode(LinkedStorageContentsMessage message, FriendlyByteBuf buffer) {
		buffer.writeUUID(message.groupId);
		buffer.writeVarLong(message.revision);
		buffer.writeNbt(message.contents);
		buffer.writeComponent(message.groupName);
		buffer.writeVarInt(message.inventorySlots);
		buffer.writeVarInt(message.upgradeSlots);
		buffer.writeVarInt(message.columnsTaken);
		buffer.writeNbt(message.hostDescriptor);
	}

	public static LinkedStorageContentsMessage decode(FriendlyByteBuf buffer) {
		return new LinkedStorageContentsMessage(buffer.readUUID(), buffer.readVarLong(), buffer.readNbt(), buffer.readComponent(), buffer.readVarInt(),
				buffer.readVarInt(), buffer.readVarInt(), buffer.readNbt());
	}

	public static LinkedStorageContentsMessage createSnapshot(ServerLevel level, UUID groupId) {
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		LinkedStorageSnapshotProfile profile = manager.resolveVirtualHost(groupId).flatMap(ILinkedStorageVirtualHost::getLinkedStorageSnapshotProfile)
				.orElseThrow(() -> new IllegalStateException("Linked storage group " + groupId + " does not provide a snapshot profile"));
		CompoundTag contents = manager.resolveContents(groupId).orElseThrow(() -> new IllegalStateException("Unknown linked storage group " + groupId))
				.getContents().copy();
		LinkedStorageHostDescriptor descriptor = manager.getHostDescriptor(groupId).orElseThrow();
		CompoundTag hostDescriptor = new CompoundTag();
		hostDescriptor.putString(LinkedStorageHostDescriptor.FACTORY_ID_TAG, descriptor.factoryId().toString());
		hostDescriptor.put(LinkedStorageHostDescriptor.VIRTUAL_CARRIER_TAG,
				manager.resolveVirtualHost(groupId).flatMap(ILinkedStorageVirtualHost::getVirtualCarrierSnapshot).orElseGet(descriptor::virtualCarrier));
		return new LinkedStorageContentsMessage(groupId, manager.getRevision(groupId), contents, profile.groupName(), profile.inventorySlots(),
				profile.upgradeSlots(), profile.columnsTaken(), hostDescriptor);
	}

	public static void onMessage(LinkedStorageContentsMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ClientLinkedStorageContents.updateHostDescriptor(message.groupId, message.hostDescriptor);
			ClientLinkedStorageContents.updateContents(message.groupId, message.revision, message.contents, message.groupName, message.inventorySlots,
					message.upgradeSlots, message.columnsTaken);
			if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof StorageContainerMenuBase<?> menu) {
				menu.detectSettingsChangeAndReload();
			}
			ClientStorageContentsTooltipBase.refreshContents();
		});
		context.setPacketHandled(true);
	}
}
