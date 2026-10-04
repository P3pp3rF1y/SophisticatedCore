package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;

import java.util.UUID;
import java.util.function.Supplier;

public record LinkedStorageSettingsMessage(UUID groupId, CompoundTag settings) {
	public static void encode(LinkedStorageSettingsMessage message, FriendlyByteBuf buffer) {
		buffer.writeUUID(message.groupId);
		buffer.writeNbt(message.settings);
	}

	public static LinkedStorageSettingsMessage decode(FriendlyByteBuf buffer) {
		return new LinkedStorageSettingsMessage(buffer.readUUID(), buffer.readNbt());
	}

	public static void onMessage(LinkedStorageSettingsMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> ClientLinkedStorageContents.updateSettings(message.groupId, message.settings));
		context.setPacketHandled(true);
	}
}
