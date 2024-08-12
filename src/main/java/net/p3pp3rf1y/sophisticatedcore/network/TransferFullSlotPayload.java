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
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public record TransferFullSlotPayload(int slotId) implements CustomPacketPayload {
	public static final Type<TransferFullSlotPayload> TYPE = new Type<>(SophisticatedCore.getRL("transfer_full_slot"));
	public static final StreamCodec<ByteBuf, TransferFullSlotPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			TransferFullSlotPayload::slotId,
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
		Slot slot = storageContainer.getSlot(payload.slotId);
		ItemStack transferResult;
		do {
			transferResult = storageContainer.quickMoveStack(player, payload.slotId);
		} while (!transferResult.isEmpty() && ItemStack.isSameItem(slot.getItem(), transferResult));
	}
}
