package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingContainerRecipeTransferHandlerServer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record JeiTransferRecipeMessage(ResourceLocation recipeId, ResourceLocation recipeTypeId,
									   Map<Integer, Integer> matchingItems,
									   List<Integer> craftingSlotIndexes, List<Integer> inventorySlotIndexes,
									   boolean maxTransfer) {

	public static void encode(JeiTransferRecipeMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeResourceLocation(msg.recipeId);
		packetBuffer.writeResourceLocation(msg.recipeTypeId);
		packetBuffer.writeMap(msg.matchingItems, FriendlyByteBuf::writeInt, FriendlyByteBuf::writeInt);
		packetBuffer.writeCollection(msg.craftingSlotIndexes, FriendlyByteBuf::writeInt);
		packetBuffer.writeCollection(msg.inventorySlotIndexes, FriendlyByteBuf::writeInt);
		packetBuffer.writeBoolean(msg.maxTransfer);
	}

	public static JeiTransferRecipeMessage decode(FriendlyByteBuf packetBuffer) {
		return new JeiTransferRecipeMessage(packetBuffer.readResourceLocation(), packetBuffer.readResourceLocation(), packetBuffer.readMap(HashMap::new, FriendlyByteBuf::readInt, FriendlyByteBuf::readInt), packetBuffer.readList(FriendlyByteBuf::readInt), packetBuffer.readList(FriendlyByteBuf::readInt), packetBuffer.readBoolean());
	}

	static void onMessage(JeiTransferRecipeMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg, context.getSender()));
		context.setPacketHandled(true);
	}

	private static void handleMessage(JeiTransferRecipeMessage msg, @Nullable ServerPlayer sender) {
		if (sender == null) {
			return;
		}

		RecipeType<?> recipeType = ForgeRegistries.RECIPE_TYPES.getValue(msg.recipeTypeId);
		if (recipeType == null) {
			return;
		}
		CraftingContainerRecipeTransferHandlerServer.setItemsWithSlotIDMap(sender, msg.recipeId, recipeType, msg.matchingItems, msg.craftingSlotIndexes, msg.inventorySlotIndexes, msg.maxTransfer);
	}
}
