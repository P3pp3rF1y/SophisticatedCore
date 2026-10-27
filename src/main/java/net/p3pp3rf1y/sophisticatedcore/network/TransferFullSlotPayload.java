package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IFilterSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public record TransferFullSlotPayload(int slotId) implements CustomPacketPayload {
	public static final Type<TransferFullSlotPayload> TYPE = new Type<>(SophisticatedCore.getRL("transfer_full_slot"));
	public static final StreamCodec<ByteBuf, TransferFullSlotPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, TransferFullSlotPayload::slotId,
			TransferFullSlotPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(TransferFullSlotPayload payload, IPayloadContext context) {
		Player player = context.player();
		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> storageContainer)) {
			return;
		}
		int slotId = payload.slotId();
		if (slotId < 0 || slotId >= storageContainer.getTotalSlotsNumber()) {
			return;
		}
		Slot slot = storageContainer.getSlot(slotId);
		// The slot id comes from the client, so the source has to be validated here instead of relying on the
		// GUI only ever sending real storage slots. Ghost filter samples must not be extractable; other upgrade
		// settings slots (crafting ingredients/results, memory slots, ...) are not restricted.
		if (slot instanceof IFilterSlot || !slot.isActive() || !slot.mayPickup(player)) {
			return;
		}
		ItemStack transferResult;
		do {
			transferResult = storageContainer.quickMoveStack(player, slotId);
		} while (!transferResult.isEmpty() && ItemStack.isSameItem(slot.getItem(), transferResult));
	}
}
