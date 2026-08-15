package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingContainerRecipeTransferHandlerServer;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import java.util.List;

public record EmiTransferRecipePayload(ResourceKey<Recipe<?>> recipeId, Identifier recipeTypeId, int action, List<Integer> slots, List<Integer> crafting, int output, List<ItemStack> stacks, boolean maxTransfer) implements CustomPacketPayload {
	public static final Type<EmiTransferRecipePayload> TYPE = new Type<>(SophisticatedCore.getIdentifier("emi_transfer_recipe"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EmiTransferRecipePayload> STREAM_CODEC = StreamCodecHelper.composite(
			ResourceKey.streamCodec(Registries.RECIPE),
			EmiTransferRecipePayload::recipeId,
			Identifier.STREAM_CODEC,
			EmiTransferRecipePayload::recipeTypeId,
			ByteBufCodecs.INT,
			EmiTransferRecipePayload::action,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list(512)),
			EmiTransferRecipePayload::slots,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list(9)),
			EmiTransferRecipePayload::crafting,
			ByteBufCodecs.INT,
			EmiTransferRecipePayload::output,
			ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(9)),
			EmiTransferRecipePayload::stacks,
			ByteBufCodecs.BOOL,
			EmiTransferRecipePayload::maxTransfer,
			EmiTransferRecipePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(EmiTransferRecipePayload payload, IPayloadContext context) {
		RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.getValue(payload.recipeTypeId);
		if (recipeType == null) {
			return;
		}

		Player player = context.player();
		CraftingContainerRecipeTransferHandlerServer.setItemsWithStacks(player, payload.recipeId, recipeType, payload.stacks,
				payload.crafting, payload.slots, payload.maxTransfer);

		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> container)) {
			return;
		}

		Slot output = null;
		if (payload.output >= 0 && payload.output < container.getTotalSlotsNumber()) {
			output = container.getSlot(payload.output);
		}

		if (output != null) {
			if (payload.action == 1) {
				container.clicked(output.index, 0, ContainerInput.PICKUP, player);
			} else if (payload.action == 2) {
				container.clicked(output.index, 0, ContainerInput.QUICK_MOVE, player);
			}
		}
	}
}
