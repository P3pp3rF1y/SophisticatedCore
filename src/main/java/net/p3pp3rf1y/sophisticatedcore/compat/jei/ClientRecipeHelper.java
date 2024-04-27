package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import mezz.jei.library.util.RecipeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ClientRecipeHelper {
	private ClientRecipeHelper() {}

	public static Optional<RecipeHolder<?>> getRecipeByKey(ResourceLocation recipeKey) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel world = minecraft.level;
		if (world == null) {
			return Optional.empty();
		}
		return world.getRecipeManager().byKey(recipeKey);
	}

	public static <C extends Container, T extends Recipe<C>, U extends Recipe<?>> List<RecipeHolder<T>> transformAllRecipesOfType(RecipeType<T> recipeType, Class<U> filterRecipeClass, Function<U, T> transformRecipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return Collections.emptyList();
		}

		return level.getRecipeManager()
				.getAllRecipesFor(recipeType)
				.stream()
				.filter(r -> filterRecipeClass.isInstance(r.value()))
				.map(r -> new RecipeHolder<>(r.id(), transformRecipe.apply(filterRecipeClass.cast(r.value()))))
				.toList();
	}

	public static <C extends Container, T extends Recipe<C>, U extends Recipe<?>> List<RecipeHolder<T>> transformAllRecipesOfTypeIntoMultiple(RecipeType<T> recipeType, Class<U> filterRecipeClass, Function<U, List<RecipeHolder<T>>> transformRecipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return Collections.emptyList();
		}

		return level.getRecipeManager()
				.getAllRecipesFor(recipeType)
				.stream()
				.filter(r -> filterRecipeClass.isInstance(r.value()))
				.map(r -> transformRecipe.apply(filterRecipeClass.cast(r.value())))
				.collect(ArrayList::new, List::addAll, List::addAll);
	}

	public static CraftingRecipe copyShapedRecipe(ShapedRecipe recipe) {
		return new ShapedRecipe("", recipe.category(), recipe.pattern, RecipeUtil.getResultItem(recipe));
	}

	public static <C extends Container> ItemStack assemble(Recipe<C> recipe, C container) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			throw new NullPointerException("level must not be null.");
		}
		RegistryAccess registryAccess = level.registryAccess();
		return recipe.assemble(container, registryAccess);
	}
}
