package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingContainerRecipeTransferHandlerServer;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingContainerRecipeTransferHandlerServer.SlotTransfer;

import java.util.List;

public record JeiTransferRecipePayload(ResourceLocation recipeId, ResourceLocation recipeTypeId, List<SlotTransfer> slotTransfers,
		List<Integer> craftingSlotIndexes, List<Integer> inventorySlotIndexes, boolean maxTransfer) implements CustomPacketPayload {
	private static final int MAX_CRAFTING_SLOTS = 9;
	private static final int MAX_INVENTORY_SLOTS = 512;
	public static final Type<JeiTransferRecipePayload> TYPE = new Type<>(SophisticatedCore.getRL("jei_transfer_recipe"));
	public static final StreamCodec<ByteBuf, JeiTransferRecipePayload> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC,
			JeiTransferRecipePayload::recipeId, ResourceLocation.STREAM_CODEC, JeiTransferRecipePayload::recipeTypeId,
			SlotTransfer.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_CRAFTING_SLOTS)), JeiTransferRecipePayload::slotTransfers,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list(MAX_CRAFTING_SLOTS)), JeiTransferRecipePayload::craftingSlotIndexes,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list(MAX_INVENTORY_SLOTS)), JeiTransferRecipePayload::inventorySlotIndexes, ByteBufCodecs.BOOL,
			JeiTransferRecipePayload::maxTransfer, JeiTransferRecipePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(JeiTransferRecipePayload payload, IPayloadContext context) {
		RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.get(payload.recipeTypeId);
		if (recipeType == null) {
			return;
		}

		CraftingContainerRecipeTransferHandlerServer.setItemsWithSlotTransfers(context.player(), payload.recipeId, recipeType, payload.slotTransfers,
				payload.craftingSlotIndexes, payload.inventorySlotIndexes, payload.maxTransfer);
	}
}
