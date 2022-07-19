package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class ClientRecipeHelper {
	private ClientRecipeHelper() {}

	public static Optional<? extends Recipe<?>> getRecipeByKey(ResourceLocation recipeKey) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel world = minecraft.level;
		if (world == null) {
			return Optional.empty();
		}
		return world.getRecipeManager().byKey(recipeKey);
	}

	public static <T extends Recipe<?>, U extends Recipe<?>> List<T> getAndTransformAvailableRecipes(Set<ResourceLocation> recipeKeys, Class<U> originalRecipeClass, Function<U, T> transformRecipe) {
		List<T> ret = new ArrayList<>();
		recipeKeys.forEach(key -> getRecipeByKey(key).ifPresent(r -> {
			if (originalRecipeClass.isInstance(r)) {
				ret.add(transformRecipe.apply(originalRecipeClass.cast(r)));
			}
		}));
		return ret;
	}

	public static <T extends Recipe<?>, U extends Recipe<?>> List<T> getAndTransformAvailableItemGroupRecipes(Set<ResourceLocation> recipeKeys, Class<U> originalRecipeClass, Function<U, List<T>> getTransformedRecipes) {
		List<T> ret = new ArrayList<>();
		recipeKeys.forEach(key -> getRecipeByKey(key).ifPresent(r -> {
			if (originalRecipeClass.isInstance(r)) {
				ret.addAll(getTransformedRecipes.apply(originalRecipeClass.cast(r)));
			}
		}));
		return ret;
	}


	public static CraftingRecipe copyShapedRecipe(ShapedRecipe recipe) {
		return new ShapedRecipe(recipe.getId(), "", recipe.getRecipeWidth(), recipe.getRecipeHeight(), recipe.getIngredients(), recipe.getResultItem());
	}
}
