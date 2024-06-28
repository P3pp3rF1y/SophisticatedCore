package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public class TransferFullSlotPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "transfer_full_slot");
	private final int slotId;

	public TransferFullSlotPacket(int slotId) {
		this.slotId = slotId;
	}

	public TransferFullSlotPacket(FriendlyByteBuf buffer) {
		this(buffer.readInt());
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> storageContainer)) {
			return;
		}
		Slot slot = storageContainer.getSlot(slotId);
		ItemStack transferResult;
		do {
			transferResult = storageContainer.quickMoveStack(player, slotId);
		} while (!transferResult.isEmpty() && ItemStack.isSameItem(slot.getItem(), transferResult));
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(slotId);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
