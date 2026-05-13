package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record CraftingDisplaySpec(ResourceLocation id, boolean shapeless, int width, int height, NonNullList<Ingredient> baseIngredients,
								  List<CraftingDisplayVariant> variants, List<CraftingDisplayVariant> globalVariants, Set<ResourceLocation> replacedRecipeIds,
								  IFocusBehavior<CraftingDisplayVariant> focusBehavior) implements IRecipeViewerDisplaySpec<CraftingDisplayVariant> {
	public CraftingDisplaySpec(ResourceLocation id, boolean shapeless, int width, int height, NonNullList<Ingredient> baseIngredients,
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
			inputSlots.add(variantInputs.isEmpty() ? List.of(baseIngredients.get(i).getItems()) : variantInputs);
		}
		return inputSlots;
	}

	public List<ItemStack> getOutputStacks(List<CraftingDisplayVariant> displayVariants) {
		return displayVariants.stream().flatMap(variant -> variant.outputs().stream()).toList();
	}

	public boolean replacesCraftingRecipe(Recipe<?> recipe) {
		return replacedRecipeIds.contains(recipe.getId());
	}

	public CraftingRecipe recipe(CraftingDisplayVariant variant) {
		NonNullList<Ingredient> ingredients = NonNullList.createWithCapacity(baseIngredients.size());
		for (int i = 0; i < baseIngredients.size(); i++) {
			if (variant.inputs().size() > i && !variant.inputs().get(i).isEmpty()) {
				ingredients.add(Ingredient.of(variant.inputs().get(i)));
			} else {
				ingredients.add(baseIngredients.get(i));
			}
		}
		return shapeless ? new ShapelessRecipe(id, "", CraftingBookCategory.MISC, variant.firstOutput(), ingredients)
				: new ShapedRecipe(id, "", CraftingBookCategory.MISC, width, height, ingredients, variant.firstOutput());
	}

}
