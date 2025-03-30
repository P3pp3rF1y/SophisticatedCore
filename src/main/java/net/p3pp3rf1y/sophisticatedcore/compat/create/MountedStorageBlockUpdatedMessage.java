package net.p3pp3rf1y.sophisticatedcore.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MountedStorageBlockUpdatedMessage(int contraptionEntityId) {
	public static void encode(MountedStorageBlockUpdatedMessage msg, FriendlyByteBuf buffer) {
		buffer.writeInt(msg.contraptionEntityId);
	}

	public static MountedStorageBlockUpdatedMessage decode(FriendlyByteBuf buffer) {
		return new MountedStorageBlockUpdatedMessage(buffer.readInt());
	}

	static void onMessage(MountedStorageBlockUpdatedMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(MountedStorageBlockUpdatedMessage msg) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		Entity entity = player.level().getEntity(msg.contraptionEntityId());
		if (entity instanceof AbstractContraptionEntity contraptionEntity) {
			contraptionEntity.getContraption().deferInvalidate = true;
		}
	}
}
