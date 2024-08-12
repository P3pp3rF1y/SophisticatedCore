package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public record SyncSlotStackPayload(int windowId, int stateId, int slotNumber,
								   ItemStack stack) implements CustomPacketPayload {
	public static final Type<SyncSlotStackPayload> TYPE = new Type<>(SophisticatedCore.getRL("sync_slot_stack"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncSlotStackPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			SyncSlotStackPayload::windowId,
			ByteBufCodecs.INT,
			SyncSlotStackPayload::stateId,
			ByteBufCodecs.INT,
			SyncSlotStackPayload::slotNumber,
			ItemStack.OPTIONAL_STREAM_CODEC,
			SyncSlotStackPayload::stack,
			SyncSlotStackPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncSlotStackPayload payload, IPayloadContext context) {
		Player player = context.player();
		if (!(player.containerMenu instanceof StorageContainerMenuBase || player.containerMenu instanceof SettingsContainerMenu) || player.containerMenu.containerId != payload.windowId) {
			return;
		}
		player.containerMenu.setItem(payload.slotNumber, payload.stateId, payload.stack);
	}
}
