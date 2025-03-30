package net.p3pp3rf1y.sophisticatedcore.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public record OpenMountedStorageInventoryMessage(int contraptionEntityId, BlockPos localPos) {
	public static void encode(OpenMountedStorageInventoryMessage msg, FriendlyByteBuf buffer) {
		buffer.writeInt(msg.contraptionEntityId());
		buffer.writeBlockPos(msg.localPos());
	}

	public static OpenMountedStorageInventoryMessage decode(FriendlyByteBuf buffer) {
		return new OpenMountedStorageInventoryMessage(buffer.readInt(), buffer.readBlockPos());
	}

	static void onMessage(OpenMountedStorageInventoryMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	public static void handleMessage(@Nullable ServerPlayer player, OpenMountedStorageInventoryMessage msg) {
		if (player == null) {
			return;
		}

		Entity entity = player.level().getEntity(msg.contraptionEntityId());
		if (entity instanceof AbstractContraptionEntity contraptionEntity) {
			MountedStorageBase storage = ContraptionHelper.getMountedStorage(contraptionEntity, msg.localPos);
			if (storage != null) {
				storage.openMenu(player, contraptionEntity.getId(), msg.localPos());
			}
		}
	}
}
