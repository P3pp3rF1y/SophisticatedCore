package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;

public class CoreRecipeViewerDisplays {
	private CoreRecipeViewerDisplays() {
	}

	public static void register(IRecipeViewerDisplayCatalog catalog) {
		ClientRecipeHelper.transformAllRecipesOfType(RecipeType.CRAFTING, UpgradeNextTierRecipe.class,
				(id, recipe) -> new RecipeHolder<CraftingRecipe>(ClientRecipeHelper.recipeKey(id), new ShapedRecipe(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""), recipe.getCompose().pattern, ItemStackTemplate.fromNonEmptyStack(ClientRecipeHelper.getResultItem(recipe)))))
				.forEach(catalog::addCraftingRecipe);
	}
}
