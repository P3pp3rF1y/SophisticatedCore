package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class ClientRecipeHelper {
	private ClientRecipeHelper() {}

	public static <C extends Container, T extends Recipe<C>, U extends Recipe<C>, V> List<V> transformAllRecipesOfType(RecipeType<T> recipeType, Class<U> filterRecipeClass, Function<U, V> transformRecipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return Collections.emptyList();
		}

		return level.getRecipeManager()
				.getAllRecipesFor(recipeType)
				.stream()
				.filter(filterRecipeClass::isInstance)
				.map(r -> transformRecipe.apply(filterRecipeClass.cast(r)))
				.toList();
	}

	public static <C extends Container, T extends Recipe<C>, U extends Recipe<C>, V> List<V> transformAllRecipesOfTypeIntoMultiple(RecipeType<T> recipeType, Class<U> filterRecipeClass, Function<U, List<V>> transformRecipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return Collections.emptyList();
		}

		return level.getRecipeManager()
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
		return recipe.assemble(container, registryAccess);
	}

	public static <C extends Container> ItemStack getResultItem(Recipe<C> recipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			throw new NullPointerException("level must not be null.");
		}
		RegistryAccess registryAccess = level.registryAccess();
		return recipe.getResultItem(registryAccess);
	}
}
