package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingContainerRecipeTransferHandlerServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record JeiTransferRecipePayload(ResourceKey<Recipe<?>> recipeId, ResourceLocation recipeTypeId, Map<Integer, Integer> matchingItems,
		List<Integer> craftingSlotIndexes, List<Integer> inventorySlotIndexes, boolean maxTransfer) implements CustomPacketPayload {
	private static final int MAX_CRAFTING_SLOTS = 9;
	private static final int MAX_INVENTORY_SLOTS = 512;
	public static final Type<JeiTransferRecipePayload> TYPE = new Type<>(SophisticatedCore.getRL("jei_transfer_recipe"));
	public static final StreamCodec<ByteBuf, JeiTransferRecipePayload> STREAM_CODEC = StreamCodec.composite(ResourceKey.streamCodec(Registries.RECIPE),
			JeiTransferRecipePayload::recipeId, ResourceLocation.STREAM_CODEC, JeiTransferRecipePayload::recipeTypeId,
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.INT, ByteBufCodecs.INT, MAX_CRAFTING_SLOTS), JeiTransferRecipePayload::matchingItems,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list(MAX_CRAFTING_SLOTS)), JeiTransferRecipePayload::craftingSlotIndexes,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list(MAX_INVENTORY_SLOTS)), JeiTransferRecipePayload::inventorySlotIndexes, ByteBufCodecs.BOOL,
			JeiTransferRecipePayload::maxTransfer, JeiTransferRecipePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(JeiTransferRecipePayload payload, IPayloadContext context) {
		RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.getValue(payload.recipeTypeId);
		if (recipeType == null) {
			return;
		}

		CraftingContainerRecipeTransferHandlerServer.setItemsWithSlotIDMap(context.player(), payload.recipeId, recipeType, payload.matchingItems,
				payload.craftingSlotIndexes, payload.inventorySlotIndexes, payload.maxTransfer);
	}
}
