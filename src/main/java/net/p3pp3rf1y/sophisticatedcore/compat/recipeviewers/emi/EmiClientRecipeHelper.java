package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;

public class EmiClientRecipeHelper {
	public static EmiCraftingRecipe wrapSyntheticShapedRecipe(CraftingRecipe recipe) {
		return new EmiCraftingRecipe(recipe.getIngredients().stream().map(EmiIngredient::of).toList(), EmiStack.of(ClientRecipeHelper.getResultItem(recipe)),
				recipe.getId().withPath(path -> path.startsWith("/") ? path : "/" + path), false);
	}

	public static EmiCraftingRecipe wrapSyntheticShapelessRecipe(CraftingRecipe recipe) {
		return new EmiCraftingRecipe(recipe.getIngredients().stream().map(EmiIngredient::of).toList(), EmiStack.of(ClientRecipeHelper.getResultItem(recipe)),
				recipe.getId().withPath(path -> path.startsWith("/") ? path : "/" + path), true);
	}
}
