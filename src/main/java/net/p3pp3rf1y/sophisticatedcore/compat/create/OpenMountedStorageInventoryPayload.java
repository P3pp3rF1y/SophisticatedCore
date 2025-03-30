package net.p3pp3rf1y.sophisticatedcore.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

public record OpenMountedStorageInventoryPayload(int contraptionEntityId, BlockPos localPos) implements CustomPacketPayload {
	public static final Type<OpenMountedStorageInventoryPayload> TYPE = new Type<>(SophisticatedCore.getRL("open_mounted_storage_inventory"));
	public static final StreamCodec<ByteBuf, OpenMountedStorageInventoryPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			OpenMountedStorageInventoryPayload::contraptionEntityId,
			BlockPos.STREAM_CODEC,
			OpenMountedStorageInventoryPayload::localPos,
			OpenMountedStorageInventoryPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(OpenMountedStorageInventoryPayload payload, IPayloadContext context) {
		Player player = context.player();
		Entity entity = player.level().getEntity(payload.contraptionEntityId());
		if (entity instanceof AbstractContraptionEntity contraptionEntity) {
			MountedStorageBase storage = ContraptionHelper.getMountedStorage(contraptionEntity, payload.localPos());
			if (storage != null && player instanceof ServerPlayer serverPlayer) {
				storage.openMenu(serverPlayer, contraptionEntity.getId(), payload.localPos());
			}
		}
	}
}
