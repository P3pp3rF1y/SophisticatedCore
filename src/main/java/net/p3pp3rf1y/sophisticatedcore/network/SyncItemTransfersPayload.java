
package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.render.ItemFlightAnimator;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import java.util.HashMap;
import java.util.Map;

public record SyncItemTransfersPayload(Map<Vec3, ItemStack> itemsTransferred, Vec3 playerPos, boolean fromPlayer) implements CustomPacketPayload {
	public static final Type<SyncItemTransfersPayload> TYPE = new Type<>(SophisticatedCore.getRL("sync_item_transfers"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncItemTransfersPayload> STREAM_CODEC = StreamCodec.composite(
			StreamCodecHelper.ofMap(StreamCodecHelper.VEC3, ItemStack.STREAM_CODEC, HashMap::new),
			SyncItemTransfersPayload::itemsTransferred,
			StreamCodecHelper.VEC3,
			SyncItemTransfersPayload::playerPos,
			ByteBufCodecs.BOOL,
			SyncItemTransfersPayload::fromPlayer,
			SyncItemTransfersPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncItemTransfersPayload payload, IPayloadContext context) {
		payload.itemsTransferred().forEach((pos, stack) -> {
			Player player = context.player();
			Vec3 from = payload.fromPlayer() ? payload.playerPos : pos;
			Vec3 to = payload.fromPlayer() ? pos : payload.playerPos;
			Level level = player.level();
			ItemFlightAnimator.startFlight(stack, from, to, level.getGameTime(), 10, level.getRandom());
			level.playSound(player, to.x(), to.y(), to.z(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, RandHelper.getRandomMinusOneToOne(level.random) * 1.4F + 2.0F);
		});
	}
}
