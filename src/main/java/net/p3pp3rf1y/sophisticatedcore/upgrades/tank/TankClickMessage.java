package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

import javax.annotation.Nullable;

import java.util.function.Supplier;

public class TankClickMessage {
	private final int upgradeSlot;

	public TankClickMessage(int upgradeSlot) {
		this.upgradeSlot = upgradeSlot;
	}

	public static void encode(TankClickMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeInt(msg.upgradeSlot);
	}

	public static TankClickMessage decode(FriendlyByteBuf packetBuffer) {
		return new TankClickMessage(packetBuffer.readInt());
	}

	public static void onMessage(TankClickMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(@Nullable ServerPlayer sender, TankClickMessage msg) {
		if (sender == null || !(sender.containerMenu instanceof StorageContainerMenuBase)) {
			return;
		}
		AbstractContainerMenu containerMenu = sender.containerMenu;
		UpgradeContainerBase<?, ?> upgradeContainer = ((StorageContainerMenuBase<?>) containerMenu).getUpgradeContainers().get(msg.upgradeSlot);
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
			sender.connection.send(new ClientboundContainerSetSlotPacket(-1, containerMenu.incrementStateId(), -1, containerMenu.getCarried()));
		});
	}

}
