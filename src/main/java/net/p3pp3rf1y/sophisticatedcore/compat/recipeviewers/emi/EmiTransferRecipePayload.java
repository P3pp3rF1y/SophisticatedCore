package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingContainerRecipeTransferHandlerServer;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import java.util.List;

public record EmiTransferRecipePayload(ResourceLocation recipeId, ResourceLocation recipeTypeId, int action, List<Integer> slots, List<Integer> crafting,
		int output, List<ItemStack> stacks, boolean maxTransfer) implements CustomPacketPayload {
	private static final int MAX_CRAFTING_SLOTS = 9;
	private static final int MAX_INVENTORY_SLOTS = 512;
	public static final Type<EmiTransferRecipePayload> TYPE = new Type<>(SophisticatedCore.getRL("emi_transfer_recipe"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EmiTransferRecipePayload> STREAM_CODEC = StreamCodecHelper.composite(ResourceLocation.STREAM_CODEC,
			EmiTransferRecipePayload::recipeId, ResourceLocation.STREAM_CODEC, EmiTransferRecipePayload::recipeTypeId, ByteBufCodecs.INT,
			EmiTransferRecipePayload::action, ByteBufCodecs.INT.apply(ByteBufCodecs.list(MAX_INVENTORY_SLOTS)), EmiTransferRecipePayload::slots,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list(MAX_CRAFTING_SLOTS)), EmiTransferRecipePayload::crafting, ByteBufCodecs.INT,
			EmiTransferRecipePayload::output, ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_CRAFTING_SLOTS)), EmiTransferRecipePayload::stacks,
			ByteBufCodecs.BOOL, EmiTransferRecipePayload::maxTransfer, EmiTransferRecipePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(EmiTransferRecipePayload payload, IPayloadContext context) {
		RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.get(payload.recipeTypeId);
		if (recipeType == null) {
			return;
		}

		Player player = context.player();
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
