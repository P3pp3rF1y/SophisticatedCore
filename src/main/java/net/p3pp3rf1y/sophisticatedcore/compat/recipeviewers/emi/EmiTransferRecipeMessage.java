package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingContainerRecipeTransferHandlerServer;

import javax.annotation.Nullable;

import java.util.List;
import java.util.function.Supplier;

public record EmiTransferRecipeMessage(ResourceLocation recipeId, ResourceLocation recipeTypeId, int action, List<Integer> slots, List<Integer> crafting,
		int output, List<ItemStack> stacks, boolean maxTransfer) {
	public static void encode(EmiTransferRecipeMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeResourceLocation(msg.recipeId);
		packetBuffer.writeResourceLocation(msg.recipeTypeId);
		packetBuffer.writeInt(msg.action);
		packetBuffer.writeCollection(msg.slots, FriendlyByteBuf::writeVarInt);
		packetBuffer.writeCollection(msg.crafting, FriendlyByteBuf::writeVarInt);
		packetBuffer.writeInt(msg.output);
		packetBuffer.writeCollection(msg.stacks, FriendlyByteBuf::writeItem);
		packetBuffer.writeBoolean(msg.maxTransfer);
	}

	public static EmiTransferRecipeMessage decode(FriendlyByteBuf packetBuffer) {
		return new EmiTransferRecipeMessage(packetBuffer.readResourceLocation(), packetBuffer.readResourceLocation(), packetBuffer.readInt(),
				packetBuffer.readList(FriendlyByteBuf::readVarInt), packetBuffer.readList(FriendlyByteBuf::readVarInt), packetBuffer.readInt(),
				packetBuffer.readList(FriendlyByteBuf::readItem), packetBuffer.readBoolean());
	}

	static void onMessage(EmiTransferRecipeMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg, context.getSender()));
		context.setPacketHandled(true);
	}

	public static void handleMessage(EmiTransferRecipeMessage payload, @Nullable ServerPlayer player) {
		RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.get(payload.recipeTypeId);
		if (recipeType == null) {
			return;
		}

		CraftingContainerRecipeTransferHandlerServer.setItemsWithStacks(player, payload.recipeId, recipeType, payload.stacks, payload.crafting, payload.slots,
				payload.maxTransfer);

		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> container)) {
			return;
		}

		Slot output = null;
		if (payload.output >= 0 && payload.output < container.getTotalSlotsNumber()) {
			output = container.getSlot(payload.output);
		}

		if (output != null) {
			if (payload.action == 1) {
				container.clicked(output.index, 0, ClickType.PICKUP, player);
			} else if (payload.action == 2) {
				container.clicked(output.index, 0, ClickType.QUICK_MOVE, player);
			}
		}
	}
}
