package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ClientRecipeHelper {
	private ClientRecipeHelper() {
	}

	public static <I extends RecipeInput, T extends Recipe<I>, U extends Recipe<?>, V> List<V> transformAllRecipesOfType(RecipeType<T> recipeType,
			Class<U> filterRecipeClass, BiFunction<ResourceLocation, U, V> transformRecipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return Collections.emptyList();
		}

		return transformAllRecipesOfType(level.getRecipeManager(), recipeType, filterRecipeClass, transformRecipe);
	}

	public static <I extends RecipeInput, T extends Recipe<I>, U extends Recipe<?>, V> List<V> transformAllRecipesOfType(RecipeManager recipeManager,
			RecipeType<T> recipeType, Class<U> filterRecipeClass, BiFunction<ResourceLocation, U, V> transformRecipe) {
		return recipeManager.getAllRecipesFor(recipeType).stream().filter(r -> filterRecipeClass.isInstance(r.value()))
				.map(r -> transformRecipe.apply(r.id(), filterRecipeClass.cast(r.value()))).toList();
	}

	public static <I extends RecipeInput, T extends Recipe<I>, U extends Recipe<?>, V> List<V> transformAllRecipeHoldersOfType(RecipeType<T> recipeType,
			Class<U> filterRecipeClass, BiFunction<ResourceLocation, RecipeHolder<U>, V> transformRecipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return Collections.emptyList();
		}

		return transformAllRecipeHoldersOfType(level.getRecipeManager(), recipeType, filterRecipeClass, transformRecipe);
	}

	public static <I extends RecipeInput, T extends Recipe<I>, U extends Recipe<?>, V> List<V> transformAllRecipeHoldersOfType(RecipeManager recipeManager,
			RecipeType<T> recipeType, Class<U> filterRecipeClass, BiFunction<ResourceLocation, RecipeHolder<U>, V> transformRecipe) {
		return recipeManager.getAllRecipesFor(recipeType).stream().filter(r -> filterRecipeClass.isInstance(r.value()))
				.map(r -> transformRecipe.apply(r.id(), new RecipeHolder<>(r.id(), filterRecipeClass.cast(r.value())))).toList();
	}

	public static <I extends RecipeInput, T extends Recipe<I>, U extends Recipe<?>, V> List<V> transformAllRecipesOfTypeIntoMultiple(RecipeType<T> recipeType,
			Class<U> filterRecipeClass, Function<U, List<V>> transformRecipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return Collections.emptyList();
		}

		return transformAllRecipesOfTypeIntoMultiple(level.getRecipeManager(), recipeType, filterRecipeClass, transformRecipe);
	}

	public static <I extends RecipeInput, T extends Recipe<I>, U extends Recipe<?>, V> List<V> transformAllRecipesOfTypeIntoMultiple(
			RecipeManager recipeManager, RecipeType<T> recipeType, Class<U> filterRecipeClass, Function<U, List<V>> transformRecipe) {
		return recipeManager.getAllRecipesFor(recipeType).stream().filter(r -> filterRecipeClass.isInstance(r.value()))
				.map(r -> transformRecipe.apply(filterRecipeClass.cast(r.value()))).collect(ArrayList::new, List::addAll, List::addAll);
	}

	public static <I extends RecipeInput, T extends Recipe<I>, U extends Recipe<?>, V> List<V> transformAllRecipeHoldersOfTypeIntoMultiple(
			RecipeType<T> recipeType, Class<U> filterRecipeClass, Function<RecipeHolder<U>, List<V>> transformRecipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return Collections.emptyList();
		}

		return transformAllRecipeHoldersOfTypeIntoMultiple(level.getRecipeManager(), recipeType, filterRecipeClass, transformRecipe);
	}

	public static <I extends RecipeInput, T extends Recipe<I>, U extends Recipe<?>, V> List<V> transformAllRecipeHoldersOfTypeIntoMultiple(
			RecipeManager recipeManager, RecipeType<T> recipeType, Class<U> filterRecipeClass, Function<RecipeHolder<U>, List<V>> transformRecipe) {
		return recipeManager.getAllRecipesFor(recipeType).stream().filter(r -> filterRecipeClass.isInstance(r.value()))
				.map(r -> transformRecipe.apply(new RecipeHolder<>(r.id(), filterRecipeClass.cast(r.value()))))
				.collect(ArrayList::new, List::addAll, List::addAll);
	}

	public static <I extends RecipeInput> ItemStack assemble(Recipe<I> recipe, I container) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			throw new NullPointerException("level must not be null.");
		}
		RegistryAccess registryAccess = level.registryAccess();
		return assemble(recipe, container, registryAccess);
	}

	public static <I extends RecipeInput> ItemStack assemble(Recipe<I> recipe, I container, HolderLookup.Provider registries) {
		return recipe.assemble(container, registries);
	}

	public static <I extends RecipeInput> ItemStack getResultItem(Recipe<I> recipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			throw new NullPointerException("level must not be null.");
		}
		RegistryAccess registryAccess = level.registryAccess();
		return getResultItem(recipe, registryAccess);
	}

	public static <I extends RecipeInput> ItemStack getResultItem(Recipe<I> recipe, HolderLookup.Provider registries) {
		return recipe.getResultItem(registries);
	}
}
