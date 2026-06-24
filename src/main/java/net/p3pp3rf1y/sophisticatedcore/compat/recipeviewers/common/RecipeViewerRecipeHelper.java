package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.CustomShapelessRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.IWrapperRecipe;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RecipeViewerRecipeHelper {
	private RecipeViewerRecipeHelper() {
	}

	public static Optional<ShapedRecipe> getShapedRecipe(Recipe<?> recipe) {
		if (recipe instanceof ShapedRecipe shapedRecipe) {
			return Optional.of(shapedRecipe);
		}

		if (recipe instanceof IWrapperRecipe<?> wrapperRecipe && wrapperRecipe.getCompose() instanceof ShapedRecipe shapedRecipe) {
			return Optional.of(shapedRecipe);
		}

		return Optional.empty();
	}

	public static Optional<List<Ingredient>> getShapelessIngredients(Recipe<?> recipe) {
		if (recipe instanceof CustomShapelessRecipe customShapelessRecipe) {
			return Optional.of(customShapelessRecipe.placementInfo().ingredients());
		}

		if (recipe instanceof ShapelessRecipe shapelessRecipe) {
			return Optional.of(shapelessRecipe.ingredients);
		}

		if (recipe instanceof IWrapperRecipe<?> wrapperRecipe && wrapperRecipe.getCompose() instanceof ShapelessRecipe shapelessRecipe) {
			return Optional.of(shapelessRecipe.ingredients);
		}

		return Optional.empty();
	}

	public static Optional<ItemStack> getShapelessResult(Recipe<?> recipe) {
		if (recipe instanceof CustomShapelessRecipe customShapelessRecipe) {
			return Optional.of(customShapelessRecipe.result().create());
		}

		if (recipe instanceof ShapelessRecipe shapelessRecipe) {
			return Optional.of(shapelessRecipe.result.create());
		}

		if (recipe instanceof IWrapperRecipe<?> wrapperRecipe && wrapperRecipe.getCompose() instanceof ShapelessRecipe shapelessRecipe) {
			return Optional.of(shapelessRecipe.result.create());
		}

		return Optional.empty();
	}

	public static Collection<Optional<Ingredient>> getIngredients(Recipe<?> recipe) {
		return getShapedRecipe(recipe).<Collection<Optional<Ingredient>>>map(shapedRecipe -> shapedRecipe.pattern.ingredients())
				.orElseGet(() -> getShapelessIngredients(recipe)
						.<Collection<Optional<Ingredient>>>map(ingredients -> ingredients.stream().map(Optional::of).toList())
						.orElseGet(Collections::emptyList));
	}
}
