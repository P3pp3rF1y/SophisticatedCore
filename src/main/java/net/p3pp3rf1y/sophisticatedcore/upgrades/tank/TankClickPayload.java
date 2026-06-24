package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

public record TankClickPayload(int upgradeSlot) implements CustomPacketPayload {
	public static final Type<TankClickPayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("tank_click"));
	public static final StreamCodec<ByteBuf, TankClickPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, TankClickPayload::upgradeSlot,
			TankClickPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(TankClickPayload payload, IPayloadContext context) {
		Player player = context.player();
		if (!(player instanceof ServerPlayer serverPlayer) || !(player.containerMenu instanceof StorageContainerMenuBase<?> storageContainerMenu)) {
			return;
		}
		AbstractContainerMenu containerMenu = player.containerMenu;
		UpgradeContainerBase<?, ?> upgradeContainer = storageContainerMenu.getUpgradeContainers().get(payload.upgradeSlot);
		if (!(upgradeContainer instanceof TankUpgradeContainer tankContainer)) {
			return;
		}
		ItemStack cursorStack = containerMenu.getCarried();
		if (cursorStack.getCount() > 1) {
			return;
		}

		TankUpgradeWrapper tankWrapper = tankContainer.getUpgradeWrapper();
		tankWrapper.interactWithCursorStack(cursorStack, stack -> {
			containerMenu.setCarried(stack);
			serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(-1, containerMenu.incrementStateId(), -1, containerMenu.getCarried()));
		});
	}
}
