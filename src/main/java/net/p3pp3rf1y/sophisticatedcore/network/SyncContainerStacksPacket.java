package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.ArrayList;
import java.util.List;

public class SyncContainerStacksPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = new ResourceLocation(SophisticatedCore.MOD_ID, "sync_container_stacks");
	private final int windowId;
	private final int stateId;
	private final List<ItemStack> itemStacks;
	private final ItemStack carriedStack;

	public SyncContainerStacksPacket(int windowId, int stateId, List<ItemStack> itemStacks, ItemStack carriedStack) {
		this.windowId = windowId;
		this.stateId = stateId;
		this.itemStacks = itemStacks;
		this.carriedStack = carriedStack;
	}

	public SyncContainerStacksPacket(FriendlyByteBuf buffer) {
		this(buffer.readUnsignedByte(), buffer.readVarInt(), buffer.readCollection(ArrayList::new, PacketHelper::readOversizedItemStack), buffer.readItem());
	}

	public void handle(PlayPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase) || player.containerMenu.containerId != windowId) {
			return;
		}
		player.containerMenu.initializeContents(stateId, itemStacks, carriedStack);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeByte(windowId);
		buffer.writeVarInt(stateId);
		buffer.writeCollection(itemStacks, (buf, itemStack) -> PacketHelper.writeOversizedItemStack(itemStack, buf));
		buffer.writeItem(carriedStack);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
