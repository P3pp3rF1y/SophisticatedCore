package net.p3pp3rf1y.sophisticatedcore.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import org.jspecify.annotations.Nullable;

public record MountedStorageUpdatePayload(int contraptionEntityId, BlockPos localPos, ItemStack storageStack, boolean refreshBlockRender) implements CustomPacketPayload {
	public static final Type<MountedStorageUpdatePayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("mounted_storage_update"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MountedStorageUpdatePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			MountedStorageUpdatePayload::contraptionEntityId,
			BlockPos.STREAM_CODEC,
			MountedStorageUpdatePayload::localPos,
			ItemStack.STREAM_CODEC,
			MountedStorageUpdatePayload::storageStack,
			ByteBufCodecs.BOOL,
			MountedStorageUpdatePayload::refreshBlockRender,
			MountedStorageUpdatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(MountedStorageUpdatePayload payload, IPayloadContext context) {
		Player player = context.player();
		Entity entity = player.level().getEntity(payload.contraptionEntityId());
		if (entity instanceof AbstractContraptionEntity contraptionEntity) {
			@Nullable MountedStorageBase mountedStorage = ContraptionHelper.getMountedStorage(contraptionEntity, payload.localPos());
			if (mountedStorage == null) {
				return;
			}
			mountedStorage.updateWithSyncedStorageStack(payload.storageStack(), payload.refreshBlockRender());
		}
	}
}
