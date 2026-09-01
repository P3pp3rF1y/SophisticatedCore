package net.p3pp3rf1y.sophisticatedcore.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkPendingCraftData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerItem;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerStackState;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackLifecycle;

import java.util.function.IntFunction;

public class EnderLinkerClearRecipe extends CustomRecipe {
	public static final EnderLinkerClearRecipe INSTANCE = new EnderLinkerClearRecipe(CraftingBookCategory.MISC);
	public static final MapCodec<EnderLinkerClearRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
	public static final StreamCodec<RegistryFriendlyByteBuf, EnderLinkerClearRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
	public static final RecipeSerializer<EnderLinkerClearRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	public EnderLinkerClearRecipe(CraftingBookCategory category) {
		super();
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return !getLinker(input.size(), input::getItem).isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		ItemStack linker = getLinker(input.size(), input::getItem);
		if (linker.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack result = linker.copyWithCount(1);
		LinkedStorageStackLifecycle.clear(result);
		return result;
	}

	public static void clearPendingCraftClaim(ServerLevel level, Container inventory) {
		ItemStack linker = getLinker(inventory.getContainerSize(), inventory::getItem);
		EnderLinkPendingCraftData pendingCraft = linker.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		if (pendingCraft == null) {
			return;
		}
		if (pendingCraft.claimId() == null || !LinkedStorageGroupsSavedData.get(level).manager().consumeActivePendingCraftClaim(pendingCraft.claimId())) {
			throw new IllegalStateException("Could not clear pending Ender Linker craft claim");
		}
	}

	@Override
	public RecipeSerializer<EnderLinkerClearRecipe> getSerializer() {
		return SERIALIZER;
	}

	private static ItemStack getLinker(int size, IntFunction<ItemStack> getItem) {
		ItemStack linker = ItemStack.EMPTY;
		for (int slot = 0; slot < size; slot++) {
			ItemStack stack = getItem.apply(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (!linker.isEmpty() || !(stack.getItem() instanceof EnderLinkerItem)
					|| LinkedStorageStackLifecycle.classifyLinker(stack) == EnderLinkerStackState.UNLINKED) {
				return ItemStack.EMPTY;
			}
			linker = stack;
		}
		return linker;
	}
}
