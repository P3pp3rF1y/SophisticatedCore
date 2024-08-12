package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public record SetGhostSlotPayload(ItemStack stack, int slotNumber) implements CustomPacketPayload {
	public static final Type<SetGhostSlotPayload> TYPE = new Type<>(SophisticatedCore.getRL("set_ghost_slot"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SetGhostSlotPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC,
			SetGhostSlotPayload::stack,
			ByteBufCodecs.INT,
			SetGhostSlotPayload::slotNumber,
			SetGhostSlotPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SetGhostSlotPayload payload, IPayloadContext context) {
		if (!(context.player().containerMenu instanceof StorageContainerMenuBase<?>)) {
			return;
		}
		context.player().containerMenu.getSlot(payload.slotNumber).set(payload.stack);
	}
}
