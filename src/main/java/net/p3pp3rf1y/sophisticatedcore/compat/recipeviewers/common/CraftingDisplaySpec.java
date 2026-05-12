package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record CraftingDisplaySpec(Identifier id, boolean shapeless, int width, int height, NonNullList<Ingredient> baseIngredients,
								  List<CraftingDisplayVariant> variants, List<CraftingDisplayVariant> globalVariants, Set<Identifier> replacedRecipeIds,
								  IFocusBehavior<CraftingDisplayVariant> focusBehavior) implements IRecipeViewerDisplaySpec<CraftingDisplayVariant> {
	public CraftingDisplaySpec(Identifier id, boolean shapeless, int width, int height, NonNullList<Ingredient> baseIngredients,
			List<CraftingDisplayVariant> variants, IFocusBehavior<CraftingDisplayVariant> focusBehavior) {
		this(id, shapeless, width, height, baseIngredients, variants, variants, Set.of(), focusBehavior);
	}

	public CraftingDisplaySpec {
		variants = List.copyOf(variants);
		globalVariants = List.copyOf(globalVariants);
		replacedRecipeIds = Set.copyOf(replacedRecipeIds);
	}

	@Override
	public List<CraftingDisplayVariant> getAllDisplays() {
		return focusBehavior.allDisplays(variants);
	}

	@Override
	public List<CraftingDisplayVariant> getGlobalDisplays() {
		return focusBehavior.allDisplays(globalVariants);
	}

	@Override
	public List<CraftingDisplayVariant> getRecipesFor(ItemStack focusedOutput) {
		return focusBehavior.recipesFor(variants, focusedOutput);
	}

	@Override
	public List<CraftingDisplayVariant> getUsagesFor(ItemStack focusedInput) {
		return focusBehavior.usagesFor(variants, focusedInput);
	}

	public List<List<ItemStack>> getInputSlots(List<CraftingDisplayVariant> displayVariants) {
		List<List<ItemStack>> inputSlots = new ArrayList<>(baseIngredients.size());
		for (int i = 0; i < baseIngredients.size(); i++) {
			int inputIndex = i;
			List<ItemStack> variantInputs = displayVariants.stream()
					.filter(variant -> variant.inputs().size() > inputIndex && !variant.inputs().get(inputIndex).isEmpty())
					.map(variant -> variant.inputs().get(inputIndex))
					.toList();
			inputSlots.add(variantInputs.isEmpty() ? ingredientStacks(baseIngredients.get(i)) : variantInputs);
		}
		return inputSlots;
	}

	public List<ItemStack> getOutputStacks(List<CraftingDisplayVariant> displayVariants) {
		return displayVariants.stream().flatMap(variant -> variant.outputs().stream()).toList();
	}

	public boolean replacesCraftingRecipe(RecipeHolder<?> recipeHolder) {
		return replacedRecipeIds.contains(recipeHolder.id().identifier());
	}

	public RecipeHolder<CraftingRecipe> recipeHolder(CraftingDisplayVariant variant) {
		return recipeHolder(List.of(variant));
	}

	public RecipeHolder<CraftingRecipe> recipeHolder(List<CraftingDisplayVariant> displayVariants) {
		CraftingDisplayVariant variant = displayVariants.getFirst();
		NonNullList<Ingredient> ingredients = NonNullList.createWithCapacity(baseIngredients.size());
		for (int i = 0; i < baseIngredients.size(); i++) {
			if (variant.inputs().size() > i && !variant.inputs().get(i).isEmpty()) {
				ingredients.add(ingredientForStack(variant.inputs().get(i)));
			} else {
				ingredients.add(baseIngredients.get(i));
			}
		}
		List<Optional<Ingredient>> shapedIngredients = ingredients.stream().map(ingredient -> ingredient.isEmpty() ? Optional.<Ingredient>empty() : Optional.of(ingredient)).toList();
		CraftingRecipe recipe = shapeless ? new SpecShapelessRecipe(this, displayVariants, variant.firstOutput(), ingredients)
				: new SpecShapedRecipe(this, displayVariants, width, height, shapedIngredients, variant.firstOutput());
		return new RecipeHolder<>(ClientRecipeHelper.recipeKey(id), recipe);
	}

	private static Ingredient ingredientForStack(ItemStack stack) {
		return stack.getComponentsPatch().isEmpty() ? Ingredient.of(stack.getItem()) : DataComponentIngredient.of(false, stack);
	}

	private static List<ItemStack> ingredientStacks(Ingredient ingredient) {
		return ingredient.items().map(ItemStack::new).toList();
	}

	public static class SpecShapedRecipe extends ShapedRecipe implements IRecipeViewerCraftingSpecRecipe {
		private final CraftingDisplaySpec spec;
		private final List<CraftingDisplayVariant> variants;

		private SpecShapedRecipe(CraftingDisplaySpec spec, List<CraftingDisplayVariant> variants, int width, int height, List<Optional<Ingredient>> ingredients, ItemStack result) {
			super("", CraftingBookCategory.MISC, new ShapedRecipePattern(width, height, ingredients, Optional.empty()), result);
			this.spec = spec;
			this.variants = List.copyOf(variants);
		}

		@Override
		public CraftingDisplaySpec spec() {
			return spec;
		}

		@Override
		public List<CraftingDisplayVariant> variants() {
			return variants;
		}
	}

	public static class SpecShapelessRecipe extends ShapelessRecipe implements IRecipeViewerCraftingSpecRecipe {
		private final CraftingDisplaySpec spec;
		private final List<CraftingDisplayVariant> variants;

		private SpecShapelessRecipe(CraftingDisplaySpec spec, List<CraftingDisplayVariant> variants, ItemStack result, NonNullList<Ingredient> ingredients) {
			super("", CraftingBookCategory.MISC, result, ingredients);
			this.spec = spec;
			this.variants = List.copyOf(variants);
		}

		@Override
		public CraftingDisplaySpec spec() {
			return spec;
		}

		@Override
		public List<CraftingDisplayVariant> variants() {
			return variants;
		}
	}

}
