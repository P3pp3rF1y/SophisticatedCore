package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;

public class SetMemorySlotMessage implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "set_memory_slot");
	private final ItemStack stack;
	private final int slotNumber;

	public SetMemorySlotMessage(ItemStack stack, int slotNumber) {
		this.stack = stack;
		this.slotNumber = slotNumber;
	}

	public SetMemorySlotMessage(FriendlyByteBuf buffer) {
		this(buffer.readItem(), buffer.readShort());
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (!(player.containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu)) {
			return;
		}
		IStorageWrapper storageWrapper = settingsContainerMenu.getStorageWrapper();
		storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).setFilter(slotNumber, stack);
		storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemChanged(slotNumber);
		storageWrapper.getInventoryHandler().onSlotFilterChanged(slotNumber);
		settingsContainerMenu.sendAdditionalSlotInfo();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeItem(stack);
		buffer.writeShort(slotNumber);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
