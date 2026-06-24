package net.p3pp3rf1y.sophisticatedcore.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.network.ISplittableMessage;

import javax.annotation.Nullable;

import java.util.function.Supplier;

public record MountedStorageUpdateMessage(int contraptionEntityId, BlockPos localPos, ItemStack storageStack,
		boolean refreshBlockRender) implements ISplittableMessage {
	public static void encode(MountedStorageUpdateMessage msg, FriendlyByteBuf buffer) {
		buffer.writeInt(msg.contraptionEntityId);
		buffer.writeBlockPos(msg.localPos);
		buffer.writeItem(msg.storageStack);
		buffer.writeBoolean(msg.refreshBlockRender);
	}

	public static MountedStorageUpdateMessage decode(FriendlyByteBuf buffer) {
		return new MountedStorageUpdateMessage(buffer.readInt(), buffer.readBlockPos(), buffer.readItem(), buffer.readBoolean());
	}

	static void onMessage(MountedStorageUpdateMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(MountedStorageUpdateMessage msg) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		Entity entity = player.level().getEntity(msg.contraptionEntityId());
		if (entity instanceof AbstractContraptionEntity contraptionEntity) {
			@Nullable
			MountedStorageBase mountedStorage = ContraptionHelper.getMountedStorage(contraptionEntity, msg.localPos());
			if (mountedStorage == null) {
				return;
			}
			mountedStorage.updateWithSyncedStorageStack(msg.storageStack(), msg.refreshBlockRender());
		}
	}
}
