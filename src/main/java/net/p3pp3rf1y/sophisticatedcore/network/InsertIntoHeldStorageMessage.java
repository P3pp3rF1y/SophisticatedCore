package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.api.IStashStorageItem;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class InsertIntoHeldStorageMessage {
	private final int slotIndex;

	public InsertIntoHeldStorageMessage(int slotIndex) {
		this.slotIndex = slotIndex;
	}

	public static void encode(InsertIntoHeldStorageMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeInt(msg.slotIndex);
	}

	public static InsertIntoHeldStorageMessage decode(FriendlyByteBuf packetBuffer) {
		return new InsertIntoHeldStorageMessage(packetBuffer.readInt());
	}

	static void onMessage(InsertIntoHeldStorageMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(@Nullable ServerPlayer player, InsertIntoHeldStorageMessage msg) {
		if (player == null) {
			return;
		}

		AbstractContainerMenu containerMenu = player.containerMenu;
		ItemStack storageStack = containerMenu.getCarried();
		if (storageStack.getItem() instanceof IStashStorageItem stashStorageItem) {
			Slot slot = containerMenu.getSlot(msg.slotIndex);
			ItemStack stackToStash = slot.getItem();
			ItemStack stashResult = stashStorageItem.stash(storageStack, stackToStash);
			slot.set(stashResult);
			slot.onTake(player, stashResult);
		}
	}
}
