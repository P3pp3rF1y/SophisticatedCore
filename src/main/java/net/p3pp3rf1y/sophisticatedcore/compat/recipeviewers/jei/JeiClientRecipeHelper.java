package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.library.util.RecipeUtil;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public class JeiClientRecipeHelper {
	public static CraftingRecipe copyShapedRecipe(ShapedRecipe recipe) {
		return new ShapedRecipe(recipe.getId(), "", recipe.category(), recipe.getRecipeWidth(), recipe.getRecipeHeight(), recipe.getIngredients(),
				RecipeUtil.getResultItem(recipe));
	}

	public static CraftingRecipe copyShapelessRecipe(ShapelessRecipe recipe) {
		return new ShapelessRecipe(recipe.getId(), "", recipe.category(), RecipeUtil.getResultItem(recipe), recipe.getIngredients());
	}
}
