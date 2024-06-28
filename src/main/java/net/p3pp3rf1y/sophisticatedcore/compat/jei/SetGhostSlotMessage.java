package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public class SetGhostSlotMessage implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "set_ghost_slot");
	private final ItemStack stack;
	private final int slotNumber;

	public SetGhostSlotMessage(ItemStack stack, int slotNumber) {
		this.stack = stack;
		this.slotNumber = slotNumber;
	}

	public SetGhostSlotMessage(FriendlyByteBuf buffer) {
		this(buffer.readItem(), buffer.readShort());
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase<?>)) {
			return;
		}
		player.containerMenu.getSlot(slotNumber).set(stack);
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
