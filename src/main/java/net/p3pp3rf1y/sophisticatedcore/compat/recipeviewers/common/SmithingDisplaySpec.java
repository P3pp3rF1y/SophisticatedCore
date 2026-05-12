package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record SmithingDisplaySpec(Identifier id, Optional<Ingredient> template, Optional<Ingredient> addition, List<SmithingDisplayVariant> variants,
								 List<SmithingDisplayVariant> globalVariants, Set<SmithingRecipe> replacedRecipes,
								 IFocusBehavior<SmithingDisplayVariant> focusBehavior) implements IRecipeViewerDisplaySpec<SmithingDisplayVariant> {

	public SmithingDisplaySpec(Identifier id, Optional<Ingredient> template, Optional<Ingredient> addition, List<SmithingDisplayVariant> variants,
			List<SmithingDisplayVariant> globalVariants, IFocusBehavior<SmithingDisplayVariant> focusBehavior) {
		this(id, template, addition, variants, globalVariants, Set.of(), focusBehavior);
	}

	public SmithingDisplaySpec {
		variants = List.copyOf(variants);
		globalVariants = List.copyOf(globalVariants);
		replacedRecipes = Set.copyOf(replacedRecipes);
	}

	@Override
	public List<SmithingDisplayVariant> getAllDisplays() {
		return focusBehavior.allDisplays(variants);
	}

	@Override
	public List<SmithingDisplayVariant> getGlobalDisplays() {
		return focusBehavior.allDisplays(globalVariants);
	}

	@Override
	public List<SmithingDisplayVariant> getRecipesFor(ItemStack focusedOutput) {
		return focusBehavior.recipesFor(variants, focusedOutput);
	}

	@Override
	public List<SmithingDisplayVariant> getUsagesFor(ItemStack focusedInput) {
		return focusBehavior.usagesFor(variants, focusedInput);
	}

	public List<ItemStack> getBaseStacks(List<SmithingDisplayVariant> displayVariants) {
		return displayVariants.stream().map(SmithingDisplayVariant::base).toList();
	}

	public List<ItemStack> getResultStacks(List<SmithingDisplayVariant> displayVariants) {
		return displayVariants.stream().map(SmithingDisplayVariant::result).toList();
	}

	public boolean replacesSmithingRecipe(SmithingRecipe recipe) {
		return replacedRecipes.contains(recipe);
	}

	public RecipeHolder<SmithingRecipe> recipeHolder(SmithingDisplayVariant variant) {
		Ingredient base = variant.base().getComponentsPatch().isEmpty() ? Ingredient.of(variant.base().getItem()) : DataComponentIngredient.of(false, variant.base());
		return new RecipeHolder<>(ClientRecipeHelper.recipeKey(id), new SmithingTransformRecipe(new Recipe.CommonInfo(true), template, base, addition, ItemStackTemplate.fromNonEmptyStack(variant.result())));
	}
}
