package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;

public record SetMemorySlotPayload(ItemStack stack, int slotNumber) implements CustomPacketPayload {
	public static final Type<SetMemorySlotPayload> TYPE = new Type<>(SophisticatedCore.getRL("set_memory_slot"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SetMemorySlotPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC,
			SetMemorySlotPayload::stack,
			ByteBufCodecs.INT,
			SetMemorySlotPayload::slotNumber,
			SetMemorySlotPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SetMemorySlotPayload payload, IPayloadContext context) {
		if (!(context.player().containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu)) {
			return;
		}
		IStorageWrapper storageWrapper = settingsContainerMenu.getStorageWrapper();
		storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).setFilter(payload.slotNumber, payload.stack);
		storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemChanged(payload.slotNumber);
		storageWrapper.getInventoryHandler().onSlotFilterChanged(payload.slotNumber);
		settingsContainerMenu.sendAdditionalSlotInfo();
	}
}
