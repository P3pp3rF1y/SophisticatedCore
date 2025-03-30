package net.p3pp3rf1y.sophisticatedcore.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

public record MountedStorageBlockUpdatedPayload(int contraptionEntityId) implements CustomPacketPayload {
	public static final Type<MountedStorageBlockUpdatedPayload> TYPE = new Type<>(SophisticatedCore.getRL("mounted_storage_block_update"));
	public static final StreamCodec<ByteBuf, MountedStorageBlockUpdatedPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			MountedStorageBlockUpdatedPayload::contraptionEntityId,
			MountedStorageBlockUpdatedPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(MountedStorageBlockUpdatedPayload payload, IPayloadContext context) {
		Player player = context.player();
		Entity entity = player.level().getEntity(payload.contraptionEntityId());
		if (entity instanceof AbstractContraptionEntity contraptionEntity) {
			contraptionEntity.getContraption().deferInvalidate = true;
		}
	}
}
