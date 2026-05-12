package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayView;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SourceResultFocusBehavior;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CraftingDisplayCatalogRecipeManagerPlugin implements ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> {
	private final Supplier<IRecipeViewerDisplayCatalog> catalogSupplier;
	private final Predicate<ItemStack> focusedStackPredicate;

	public CraftingDisplayCatalogRecipeManagerPlugin(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedStackPredicate) {
		this.catalogSupplier = catalogSupplier;
		this.focusedStackPredicate = focusedStackPredicate;
	}

	@Override
	public boolean isHandledInput(ITypedIngredient<?> input) {
		ItemStack stack = input.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return focusedStackPredicate.test(stack) && !catalogSupplier.get().getCraftingUsagesFor(stack).isEmpty();
	}

	@Override
	public boolean isHandledOutput(ITypedIngredient<?> output) {
		ItemStack stack = output.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return focusedStackPredicate.test(stack) && !catalogSupplier.get().getCraftingRecipesFor(stack).isEmpty();
	}

	@Override
	public List<RecipeHolder<CraftingRecipe>> getRecipesForInput(ITypedIngredient<?> input) {
		ItemStack stack = input.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		if (!focusedStackPredicate.test(stack)) {
			return List.of();
		}
		IRecipeViewerDisplayCatalog catalog = catalogSupplier.get();
		List<RecipeHolder<CraftingRecipe>> globalRecipes = getGlobalRecipes(catalog);
		return distinctRecipes(catalog.getCraftingUsagesFor(stack).stream()
				.flatMap(view -> view.variants().stream()
						.filter(variant -> !isDuplicateFocusedSourceRecipe(view, variant, stack, globalRecipes))
						.map(view.spec()::recipeHolder))
				.toList());
	}

	@Override
	public List<RecipeHolder<CraftingRecipe>> getRecipesForOutput(ITypedIngredient<?> output) {
		ItemStack stack = output.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		if (!focusedStackPredicate.test(stack)) {
			return List.of();
		}
		IRecipeViewerDisplayCatalog catalog = catalogSupplier.get();
		List<RecipeHolder<CraftingRecipe>> globalRecipes = getGlobalRecipes(catalog);
		return distinctRecipes(catalog.getCraftingRecipesFor(stack).stream()
				.flatMap(view -> view.variants().stream()
						.filter(variant -> !hasSameDisplayRecipe(globalRecipes, view.spec().recipeHolder(variant)))
						.map(view.spec()::recipeHolder))
				.toList());
	}

	@Override
	public List<RecipeHolder<CraftingRecipe>> getAllRecipes() {
		return distinctRecipes(catalogSupplier.get().getGlobalCraftingDisplays().stream()
				.flatMap(view -> view.variants().stream().map(view.spec()::recipeHolder))
				.toList());
	}

	private static List<RecipeHolder<CraftingRecipe>> distinctRecipes(List<RecipeHolder<CraftingRecipe>> recipes) {
		Map<String, RecipeHolder<CraftingRecipe>> distinctRecipes = new LinkedHashMap<>();
		for (RecipeHolder<CraftingRecipe> recipeHolder : recipes) {
			distinctRecipes.putIfAbsent(getRecipeKey(recipeHolder), recipeHolder);
		}
		return List.copyOf(distinctRecipes.values());
	}

	private static List<RecipeHolder<CraftingRecipe>> getGlobalRecipes(IRecipeViewerDisplayCatalog catalog) {
		return catalog.getGlobalCraftingDisplays().stream()
				.flatMap(view -> view.variants().stream().map(view.spec()::recipeHolder))
				.toList();
	}

	private static boolean isDuplicateFocusedSourceRecipe(CraftingDisplayView view, CraftingDisplayVariant variant, ItemStack focusedInput, List<RecipeHolder<CraftingRecipe>> globalRecipes) {
		return hasSameDisplayRecipe(globalRecipes, view.spec().recipeHolder(variant)) && isSourceFocus(view, variant, focusedInput);
	}

	private static boolean isSourceFocus(CraftingDisplayView view, CraftingDisplayVariant variant, ItemStack focusedInput) {
		return view.spec().focusBehavior() instanceof SourceResultFocusBehavior sourceResultFocusBehavior
				&& sourceResultFocusBehavior.sourceInputIndex() < variant.inputs().size()
				&& ItemStack.isSameItemSameComponents(variant.inputs().get(sourceResultFocusBehavior.sourceInputIndex()), focusedInput);
	}

	private static String getRecipeKey(RecipeHolder<CraftingRecipe> recipeHolder) {
		StringBuilder key = new StringBuilder(recipeHolder.id().toString());
		recipeHolder.value().getIngredients().forEach(ingredient -> {
			key.append('|');
			for (ItemStack item : ingredient.getItems()) {
				key.append(item).append(';');
			}
		});
		return key.toString();
	}

	private static boolean hasSameDisplayRecipe(List<RecipeHolder<CraftingRecipe>> recipes, RecipeHolder<CraftingRecipe> recipeHolder) {
		return recipes.stream().anyMatch(recipe -> hasSameDisplayRecipe(recipe, recipeHolder));
	}

	private static boolean hasSameDisplayRecipe(RecipeHolder<CraftingRecipe> first, RecipeHolder<CraftingRecipe> second) {
		return first.id().equals(second.id())
				&& ingredientsMatch(first.value(), second.value())
				&& ItemStack.isSameItemSameComponents(first.value().getResultItem(null), second.value().getResultItem(null));
	}

	private static boolean ingredientsMatch(CraftingRecipe first, CraftingRecipe second) {
		if (first.getIngredients().size() != second.getIngredients().size()) {
			return false;
		}
		for (int i = 0; i < first.getIngredients().size(); i++) {
			ItemStack[] firstItems = first.getIngredients().get(i).getItems();
			ItemStack[] secondItems = second.getIngredients().get(i).getItems();
			if (firstItems.length != secondItems.length) {
				return false;
			}
			for (int j = 0; j < firstItems.length; j++) {
				if (!ItemStack.isSameItemSameComponents(firstItems[j], secondItems[j])) {
					return false;
				}
			}
		}
		return true;
	}
}
