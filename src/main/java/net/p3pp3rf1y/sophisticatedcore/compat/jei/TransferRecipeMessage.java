package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransferRecipeMessage implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "transfer_recipe");
	private final Map<Integer, Integer> matchingItems;
	private final List<Integer> craftingSlotIndexes;
	private final List<Integer> inventorySlotIndexes;
	private final boolean maxTransfer;
	private final ResourceLocation recipeId;

	public TransferRecipeMessage(ResourceLocation recipeId, Map<Integer, Integer> matchingItems, List<Integer> craftingSlotIndexes, List<Integer> inventorySlotIndexes, boolean maxTransfer) {
		this.recipeId = recipeId;
		this.matchingItems = matchingItems;
		this.craftingSlotIndexes = craftingSlotIndexes;
		this.inventorySlotIndexes = inventorySlotIndexes;
		this.maxTransfer = maxTransfer;
	}

	public TransferRecipeMessage(FriendlyByteBuf buffer) {
		this(buffer.readResourceLocation(), buffer.readMap(FriendlyByteBuf::readInt, FriendlyByteBuf::readInt),
				buffer.readCollection(ArrayList::new, FriendlyByteBuf::readInt), buffer.readCollection(ArrayList::new, FriendlyByteBuf::readInt),
				buffer.readBoolean());
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		CraftingContainerRecipeTransferHandlerServer.setItems(player, recipeId, matchingItems, craftingSlotIndexes, inventorySlotIndexes, maxTransfer);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeResourceLocation(recipeId);
		buffer.writeMap(matchingItems, FriendlyByteBuf::writeInt, FriendlyByteBuf::writeInt);
		buffer.writeCollection(craftingSlotIndexes, FriendlyByteBuf::writeInt);
		buffer.writeCollection(inventorySlotIndexes, FriendlyByteBuf::writeInt);
		buffer.writeBoolean(maxTransfer);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
