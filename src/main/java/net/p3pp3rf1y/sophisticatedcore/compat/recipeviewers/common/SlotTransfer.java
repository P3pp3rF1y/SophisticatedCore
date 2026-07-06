package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SlotTransfer(int inventorySlotId, int craftingSlotId, int count) {
	public static final StreamCodec<ByteBuf, SlotTransfer> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, SlotTransfer::inventorySlotId,
			ByteBufCodecs.VAR_INT, SlotTransfer::craftingSlotId, ByteBufCodecs.VAR_INT, SlotTransfer::count, SlotTransfer::new);

	public SlotTransfer {
		if (count < 1) {
			throw new IllegalArgumentException("Slot transfer count must be positive");
		}
	}
}
