package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import net.minecraft.core.HolderGetter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeDisplayBuilder;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeDisplayGenerator;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ShapedRecipeDisplayBuilder;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ShapelessRecipeDisplayBuilder;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JeiRecipeDisplayGenerator implements IRecipeDisplayGenerator<CraftingRecipe> {
	private final HolderGetter<Item> items = RecipeHelper.getItemLookup();
	private final List<RecipeHolder<CraftingRecipe>> craftingRecipes = new ArrayList<>();
	private final List<RecipeHolder<SmithingRecipe>> smithingRecipes = new ArrayList<>();

	@Override
	public ShapedRecipeDisplayBuilder<CraftingRecipe> shaped(ItemStack result) {
		return new JeiShapedRecipeDisplayBuilder(items, this, result);
	}

	@Override
	public ShapelessRecipeDisplayBuilder<CraftingRecipe> shapeless(ItemStack result) {
		return new JeiShapelessRecipeDisplayBuilder(items, this, result);
	}

	@Override
	public IRecipeDisplayBuilder smithing(Optional<Ingredient> template, Ingredient base, Optional<Ingredient> addition, ItemStack result) {
		return id -> acceptSmithing(new RecipeHolder<>(id, new SmithingTransformRecipe(template, base, addition, new TransmuteResult(result.getItemHolder(), result.getCount(), result.getComponentsPatch()))));
	}

	private void acceptSmithing(RecipeHolder<SmithingRecipe> recipe) {
		smithingRecipes.add(recipe);
	}

	public void acceptCrafting(RecipeHolder<CraftingRecipe> recipe) {
		craftingRecipes.add(recipe);
	}

	public List<RecipeHolder<CraftingRecipe>> getCraftingRecipes() {
		return craftingRecipes;
	}

	public List<RecipeHolder<SmithingRecipe>> getSmithingRecipes() {
		return smithingRecipes;
	}
}
