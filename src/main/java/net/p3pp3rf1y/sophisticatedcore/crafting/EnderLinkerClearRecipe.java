package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkPendingCraftData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerItem;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerStackState;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackLifecycle;

import java.util.function.IntFunction;

public class EnderLinkerClearRecipe extends CustomRecipe {
	public EnderLinkerClearRecipe(net.minecraft.resources.ResourceLocation registryName, CraftingBookCategory category) {
		super(registryName, category);
	}

	@Override
	public boolean matches(CraftingContainer input, Level level) {
		return !getLinker(input.getContainerSize(), input::getItem).isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
		ItemStack linker = getLinker(input.getContainerSize(), input::getItem);
		return linker.isEmpty() ? ItemStack.EMPTY : new ItemStack(linker.getItem());
	}

	public static void clearPendingCraftClaim(ServerLevel level, Container inventory) {
		EnderLinkPendingCraftData pendingCraft = LinkedStorageStackData.getPendingCraft(getLinker(inventory.getContainerSize(), inventory::getItem));
		if (pendingCraft != null && pendingCraft.claimId() != null
				&& !LinkedStorageGroupsSavedData.get(level).manager().consumeActivePendingCraftClaim(pendingCraft.claimId())) {
			throw new IllegalStateException("Could not clear pending Ender Linker craft claim");
		}
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width >= 1 && height >= 1;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModRecipes.ENDER_LINKER_CLEAR_SERIALIZER.get();
	}

	private static ItemStack getLinker(int size, IntFunction<ItemStack> getItem) {
		ItemStack linker = ItemStack.EMPTY;
		for (int slot = 0; slot < size; slot++) {
			ItemStack stack = getItem.apply(slot);
			if (!stack.isEmpty()) {
				if (!linker.isEmpty() || !(stack.getItem() instanceof EnderLinkerItem)
						|| LinkedStorageStackLifecycle.classifyLinker(stack) == EnderLinkerStackState.UNLINKED) {
					return ItemStack.EMPTY;
				}
				linker = stack;
			}
		}
		return linker;
	}
}
