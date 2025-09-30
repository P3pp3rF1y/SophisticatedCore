
package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.client.render.ItemFlightAnimator;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;

import java.util.Map;
import java.util.function.Supplier;

public record SyncItemTransfersMessage(Map<Vec3, ItemStack> itemsTransferred, boolean fromPlayer) {
	public static void encode(SyncItemTransfersMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeMap(msg.itemsTransferred(), (buf, vec) -> {
			buf.writeDouble(vec.x());
			buf.writeDouble(vec.y());
			buf.writeDouble(vec.z());
		}, FriendlyByteBuf::writeItem);
		packetBuffer.writeBoolean(msg.fromPlayer);
	}

	public static SyncItemTransfersMessage decode(FriendlyByteBuf packetBuffer) {
		return new SyncItemTransfersMessage(
				packetBuffer.readMap(buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), FriendlyByteBuf::readItem),
				packetBuffer.readBoolean()
		);
	}

	static void onMessage(SyncItemTransfersMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg, context));
		context.setPacketHandled(true);
	}

	@OnlyIn(Dist.CLIENT)
	public static void handleMessage(SyncItemTransfersMessage msg, NetworkEvent.Context context) {
		msg.itemsTransferred().forEach((pos, stack) -> {
			LocalPlayer player = Minecraft.getInstance().player;
			Vec3 playerPos = player.getEyePosition().add(0, -0.1, 0);
			Vec3 from = msg.fromPlayer() ? playerPos : pos;
			Vec3 to = msg.fromPlayer() ? pos : playerPos;
			Level level = player.level();
			ItemFlightAnimator.startFlight(stack, from, to, level.getGameTime(), 10, level.getRandom());
			level.playSound(player, to.x(), to.y(), to.z(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, RandHelper.getRandomMinusOneToOne(level.random) * 1.4F + 2.0F);
		});
	}
}
