package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;

public class CoreRecipeViewerDisplays {
	private CoreRecipeViewerDisplays() {
	}

	public static void register(IRecipeViewerDisplayCatalog catalog) {
		ClientRecipeHelper.transformAllRecipesOfTypeWithIds(RecipeType.CRAFTING, UpgradeNextTierRecipe.class,
				(id, recipe) -> new ShapedRecipe(id, recipe.getGroup(), CraftingBookCategory.MISC, recipe.getRecipeWidth(), recipe.getRecipeHeight(),
						recipe.getIngredients(), ClientRecipeHelper.getResultItem(recipe)))
				.forEach(catalog::addCraftingRecipe);
	}
}
