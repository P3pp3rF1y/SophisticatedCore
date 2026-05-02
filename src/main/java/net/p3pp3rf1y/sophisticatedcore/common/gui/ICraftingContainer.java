package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

public interface ICraftingContainer {
	List<Slot> getRecipeSlots();

	int getCraftingGridWidth();

	int getCraftingGridHeight();

	Container getCraftMatrix();

	void setRecipeUsed(ResourceKey<Recipe<?>> recipeId);

	RecipeType<?> getRecipeType();

	boolean shouldRefillCraftingGrid();
}
