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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingContainerRecipeTransferHandlerServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ReiTransferRecipeMessage(ResourceLocation recipeId, ResourceLocation recipeTypeId, CompoundTag tag,
									   List<Integer> inputSlots, List<Integer> inventorySlots, boolean maxTransfer) {
	public static void encode(ReiTransferRecipeMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeResourceLocation(msg.recipeId);
		packetBuffer.writeResourceLocation(msg.recipeTypeId);
		packetBuffer.writeNbt(msg.tag);
		packetBuffer.writeCollection(msg.inputSlots, FriendlyByteBuf::writeVarInt);
		packetBuffer.writeCollection(msg.inventorySlots, FriendlyByteBuf::writeVarInt);
		packetBuffer.writeBoolean(msg.maxTransfer);
	}

	public static ReiTransferRecipeMessage decode(FriendlyByteBuf packetBuffer) {
		return new ReiTransferRecipeMessage(
				packetBuffer.readResourceLocation(),
				packetBuffer.readResourceLocation(),
				packetBuffer.readNbt(),
				packetBuffer.readList(FriendlyByteBuf::readVarInt),
				packetBuffer.readList(FriendlyByteBuf::readVarInt),
				packetBuffer.readBoolean()
		);
	}

	static void onMessage(ReiTransferRecipeMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(msg, context.getSender()));
		context.setPacketHandled(true);
	}

	public static void handleMessage(ReiTransferRecipeMessage payload, @Nullable ServerPlayer player) {
		RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.get(payload.recipeTypeId);
		if (recipeType == null) {
			return;
		}

		AbstractContainerMenu container = player.containerMenu;
		List<InputIngredient<ItemStack>> inputs = readInputs(payload.tag.getList("Inputs", Tag.TAG_COMPOUND));

		RecipeFinder recipeFinder = new RecipeFinder();
		for (int slotId : payload.inventorySlots) {
			ReiSlotAccessor slot = (ReiSlotAccessor) ReiSlotAccessor.fromSlot(container.getSlot(slotId));
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