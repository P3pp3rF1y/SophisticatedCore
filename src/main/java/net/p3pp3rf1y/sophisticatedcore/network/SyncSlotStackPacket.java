package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public class SyncSlotStackPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = new ResourceLocation(SophisticatedCore.MOD_ID, "sync_slot_stack");
	private final int windowId;
	private final int stateId;
	private final int slotNumber;
	private final ItemStack stack;

	public SyncSlotStackPacket(int windowId, int stateId, int slotNumber, ItemStack stack) {
		this.windowId = windowId;
		this.stateId = stateId;
		this.slotNumber = slotNumber;
		this.stack = stack;
	}

	public SyncSlotStackPacket(FriendlyByteBuf buffer) {
		this(buffer.readUnsignedByte(), buffer.readVarInt(), buffer.readShort(), PacketHelper.readOversizedItemStack(buffer));
	}

	public void handle(PlayPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase || player.containerMenu instanceof SettingsContainerMenu) || player.containerMenu.containerId != windowId) {
			return;
		}
		player.containerMenu.setItem(slotNumber, stateId, stack);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeByte(windowId);
		buffer.writeVarInt(stateId);
		buffer.writeShort(slotNumber);
		PacketHelper.writeOversizedItemStack(stack, buffer);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
