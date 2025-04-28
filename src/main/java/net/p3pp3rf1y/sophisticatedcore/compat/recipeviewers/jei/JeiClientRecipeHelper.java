package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.library.util.RecipeUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public class JeiClientRecipeHelper {
	public static RecipeHolder<CraftingRecipe> copyShapedRecipeWithRecipeHolder(ResourceLocation id, ShapedRecipe recipe) {
		return new RecipeHolder<>(id, new ShapedRecipe("", recipe.category(), recipe.pattern, RecipeUtil.getResultItem(recipe)));
	}

	public static RecipeHolder<CraftingRecipe> copyShapelessRecipeWithRecipeHolder(ResourceLocation id, ShapelessRecipe recipe) {
		return new RecipeHolder<>(id, new ShapelessRecipe("", recipe.category(), RecipeUtil.getResultItem(recipe), recipe.getIngredients()));
	}
}
