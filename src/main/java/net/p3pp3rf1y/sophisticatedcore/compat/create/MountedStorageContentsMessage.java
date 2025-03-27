package net.p3pp3rf1y.sophisticatedcore.compat.create;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record MountedStorageContentsMessage(UUID storageUuid, CompoundTag contents) {
	public static void encode(MountedStorageContentsMessage msg, FriendlyByteBuf buffer) {
		buffer.writeUUID(msg.storageUuid);
		buffer.writeNbt(msg.contents);
	}

	public static MountedStorageContentsMessage decode(FriendlyByteBuf buffer) {
		return new MountedStorageContentsMessage(buffer.readUUID(), buffer.readNbt());
	}

	static void onMessage(MountedStorageContentsMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(MountedStorageContentsMessage msg) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || msg.contents == null) {
			return;
		}

		MountedStorageData.get(msg.storageUuid).setContents(msg.storageUuid, msg.contents);
	}
}
