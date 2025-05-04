package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.InputIngredient;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.transfer.RecipeFinder;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingContainerRecipeTransferHandlerServer;

import java.util.ArrayList;
import java.util.List;

public record TransferRecipePayload(ResourceLocation recipeId, ResourceLocation recipeTypeId, CompoundTag tag, List<Integer> inputSlots, List<Integer> inventorySlots, boolean maxTransfer) implements CustomPacketPayload {
	public static final Type<TransferRecipePayload> TYPE = new Type<>(SophisticatedCore.getRL("rei_move_items"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TransferRecipePayload> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC,
			TransferRecipePayload::recipeId,
			ResourceLocation.STREAM_CODEC,
			TransferRecipePayload::recipeTypeId,
			ByteBufCodecs.COMPOUND_TAG,
			TransferRecipePayload::tag,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list()),
			TransferRecipePayload::inputSlots,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list()),
			TransferRecipePayload::inventorySlots,
			ByteBufCodecs.BOOL,
			TransferRecipePayload::maxTransfer,
			TransferRecipePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(TransferRecipePayload payload, IPayloadContext context) {
		RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.get(payload.recipeTypeId);
		if (recipeType == null) {
			return;
		}

		Player player = context.player();
		AbstractContainerMenu container = player.containerMenu;
		List<InputIngredient<ItemStack>> inputs = readInputs(payload.tag.getList("Inputs", Tag.TAG_COMPOUND));

		RecipeFinder recipeFinder = new RecipeFinder();
		for (int slotId : payload.inventorySlots) {
			SophisticatedSlotAccessor slot = (SophisticatedSlotAccessor) SophisticatedSlotAccessor.fromSlot(container.getSlot(slotId));
			recipeFinder.addNormalItem(slot.getItemStack());
		}

		NonNullList<Ingredient> ingredients = NonNullList.create();
		for (InputIngredient<ItemStack> itemStacks : inputs) {
			ingredients.add(CollectionUtils.toIngredient(itemStacks.get()));
		}

		IntList recipeItemIds = new IntArrayList();
		if (recipeFinder.findRecipe(ingredients, recipeItemIds)) {
			CraftingContainerRecipeTransferHandlerServer.setItemsWithStacks(
					player,
					payload.recipeId,
					recipeType,
					recipeItemIds.intStream().mapToObj(RecipeFinder::getStackFromId).toList(),
					payload.inputSlots,
					payload.inventorySlots,
					payload.maxTransfer
			);
		}
	}

	private static List<InputIngredient<ItemStack>> readInputs(ListTag tag) {
		List<InputIngredient<ItemStack>> inputs = new ArrayList<>();
		for (Tag t : tag) {
			CompoundTag compoundTag = (CompoundTag) t;
			InputIngredient<EntryStack<?>> stacks = InputIngredient.of(compoundTag.getInt("Index"), EntryIngredient.read(compoundTag.getList("Ingredient", Tag.TAG_COMPOUND)));
			inputs.add(InputIngredient.withType(stacks, VanillaEntryTypes.ITEM));
		}
		return inputs;
	}
}