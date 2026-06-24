package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.InputIngredient;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.transfer.ItemRecipeFinder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingContainerRecipeTransferHandlerServer;

import java.util.ArrayList;
import java.util.List;

public record ReiTransferRecipePayload(ResourceLocation recipeId, ResourceLocation recipeTypeId, CompoundTag tag, List<Integer> inputSlots,
		List<Integer> inventorySlots, boolean maxTransfer) implements CustomPacketPayload {
	public static final Type<ReiTransferRecipePayload> TYPE = new Type<>(SophisticatedCore.getRL("rei_move_items"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ReiTransferRecipePayload> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC,
			ReiTransferRecipePayload::recipeId, ResourceLocation.STREAM_CODEC, ReiTransferRecipePayload::recipeTypeId, ByteBufCodecs.COMPOUND_TAG,
			ReiTransferRecipePayload::tag, ByteBufCodecs.INT.apply(ByteBufCodecs.list()), ReiTransferRecipePayload::inputSlots,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list()), ReiTransferRecipePayload::inventorySlots, ByteBufCodecs.BOOL, ReiTransferRecipePayload::maxTransfer,
			ReiTransferRecipePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(ReiTransferRecipePayload payload, IPayloadContext context) {
		RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.getValue(payload.recipeTypeId);
		if (recipeType == null) {
			return;
		}

		Player player = context.player();
		AbstractContainerMenu container = player.containerMenu;
		List<InputIngredient<ItemStack>> inputs = readInputs(payload.tag.getListOrEmpty("Inputs"));

		ItemRecipeFinder recipeFinder = new ItemRecipeFinder();
		for (int slotId : payload.inventorySlots) {
			ReiSlotAccessor slot = (ReiSlotAccessor) ReiSlotAccessor.fromSlot(container.getSlot(slotId));
			recipeFinder.addNormalItem(slot.getItemStack());
		}

		List<List<ItemStack>> ingredients = new ArrayList<>();
		for (InputIngredient<ItemStack> itemStacks : inputs) {
			ingredients.add(itemStacks.get());
		}

		List<ItemStack> stacks = new ArrayList<>();
		if (recipeFinder.findRecipe(ingredients, 1, stacks::add)) {
			CraftingContainerRecipeTransferHandlerServer.setItemsWithStacks(player, ResourceKey.create(Registries.RECIPE, payload.recipeId), recipeType, stacks,
					payload.inputSlots, payload.inventorySlots, payload.maxTransfer);
		}
	}

	private static List<InputIngredient<ItemStack>> readInputs(ListTag tag) {
		List<InputIngredient<ItemStack>> inputs = new ArrayList<>();
		for (Tag t : tag) {
			CompoundTag compoundTag = (CompoundTag) t;
			InputIngredient<EntryStack<?>> stacks = InputIngredient.of(compoundTag.getIntOr("Index", 0),
					EntryIngredient.codec().parse(NbtOps.INSTANCE, compoundTag.getListOrEmpty("Ingredient")).getOrThrow());
			inputs.add(InputIngredient.withType(stacks, VanillaEntryTypes.ITEM));
		}
		return inputs;
	}
}
