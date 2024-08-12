package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record TransferRecipePayload(ResourceLocation recipeId, Map<Integer, Integer> matchingItems,
									List<Integer> craftingSlotIndexes, List<Integer> inventorySlotIndexes,
									boolean maxTransfer) implements CustomPacketPayload {
	public static final Type<TransferRecipePayload> TYPE = new Type<>(SophisticatedCore.getRL("transfer_recipe"));
	public static final StreamCodec<ByteBuf, TransferRecipePayload> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC,
			TransferRecipePayload::recipeId,
			StreamCodecHelper.ofMap(ByteBufCodecs.INT, ByteBufCodecs.INT, HashMap::new),
			TransferRecipePayload::matchingItems,
			StreamCodecHelper.ofCollection(ByteBufCodecs.INT, ArrayList::new),
			TransferRecipePayload::craftingSlotIndexes,
			StreamCodecHelper.ofCollection(ByteBufCodecs.INT, ArrayList::new),
			TransferRecipePayload::inventorySlotIndexes,
			ByteBufCodecs.BOOL,
			TransferRecipePayload::maxTransfer,
			TransferRecipePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(TransferRecipePayload payload, IPayloadContext context) {
		CraftingContainerRecipeTransferHandlerServer.setItems(context.player(), payload.recipeId, payload.matchingItems,
				payload.craftingSlotIndexes, payload.inventorySlotIndexes, payload.maxTransfer);
	}
}
