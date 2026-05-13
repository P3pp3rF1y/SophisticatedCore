package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ClientRecipeHelper {
	private ClientRecipeHelper() {}

	public static <C extends Container, T extends Recipe<C>, U extends Recipe<C>, V> List<V> transformAllRecipesOfType(RecipeType<T> recipeType, Class<U> filterRecipeClass, Function<U, V> transformRecipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return Collections.emptyList();
		}

		return transformAllRecipesOfType(level.getRecipeManager(), recipeType, filterRecipeClass, transformRecipe);
	}

	public static <C extends Container, T extends Recipe<C>, U extends Recipe<C>, V> List<V> transformAllRecipesOfType(RecipeManager recipeManager, RecipeType<T> recipeType, Class<U> filterRecipeClass, Function<U, V> transformRecipe) {
		return recipeManager
				.getAllRecipesFor(recipeType)
				.stream()
				.filter(filterRecipeClass::isInstance)
				.map(r -> transformRecipe.apply(filterRecipeClass.cast(r)))
				.toList();
	}

	public static <C extends Container, T extends Recipe<C>, U extends Recipe<C>, V> List<V> transformAllRecipesOfTypeWithIds(RecipeType<T> recipeType, Class<U> filterRecipeClass, BiFunction<ResourceLocation, U, V> transformRecipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return Collections.emptyList();
		}

		return transformAllRecipesOfTypeWithIds(level.getRecipeManager(), recipeType, filterRecipeClass, transformRecipe);
	}

	public static <C extends Container, T extends Recipe<C>, U extends Recipe<C>, V> List<V> transformAllRecipesOfTypeWithIds(RecipeManager recipeManager, RecipeType<T> recipeType, Class<U> filterRecipeClass, BiFunction<ResourceLocation, U, V> transformRecipe) {
		return recipeManager
				.getAllRecipesFor(recipeType)
				.stream()
				.filter(filterRecipeClass::isInstance)
				.map(r -> filterRecipeClass.cast(r))
				.map(r -> transformRecipe.apply(r.getId(), r))
				.toList();
	}

	public static <C extends Container, T extends Recipe<C>, U extends Recipe<C>, V> List<V> transformAllRecipesOfTypeIntoMultiple(RecipeType<T> recipeType, Class<U> filterRecipeClass, Function<U, List<V>> transformRecipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return Collections.emptyList();
		}

		return transformAllRecipesOfTypeIntoMultiple(level.getRecipeManager(), recipeType, filterRecipeClass, transformRecipe);
	}

	public static <C extends Container, T extends Recipe<C>, U extends Recipe<C>, V> List<V> transformAllRecipesOfTypeIntoMultiple(RecipeManager recipeManager, RecipeType<T> recipeType, Class<U> filterRecipeClass, Function<U, List<V>> transformRecipe) {
		return recipeManager
				.getAllRecipesFor(recipeType)
				.stream()
				.filter(filterRecipeClass::isInstance)
				.map(r -> transformRecipe.apply(filterRecipeClass.cast(r)))
				.collect(ArrayList::new, List::addAll, List::addAll);
	}

	public static <C extends Container> ItemStack assemble(Recipe<C> recipe, C container) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			throw new NullPointerException("level must not be null.");
		}
		RegistryAccess registryAccess = level.registryAccess();
		return assemble(recipe, container, registryAccess);
	}

	public static <C extends Container> ItemStack assemble(Recipe<C> recipe, C container, RegistryAccess registries) {
		return recipe.assemble(container, registries);
	}

	public static <C extends Container> ItemStack getResultItem(Recipe<C> recipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			throw new NullPointerException("level must not be null.");
		}
		RegistryAccess registryAccess = level.registryAccess();
		return getResultItem(recipe, registryAccess);
	}

	public static <C extends Container> ItemStack getResultItem(Recipe<C> recipe, RegistryAccess registries) {
		return recipe.getResultItem(registries);
	}
}
