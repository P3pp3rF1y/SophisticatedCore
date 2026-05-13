package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayView;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SourceResultFocusBehavior;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CraftingDisplayCatalogRecipeManagerPlugin implements ISimpleRecipeManagerPlugin<CraftingRecipe> {
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
	public List<CraftingRecipe> getRecipesForInput(ITypedIngredient<?> input) {
		ItemStack stack = input.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		if (!focusedStackPredicate.test(stack)) {
			return List.of();
		}
		IRecipeViewerDisplayCatalog catalog = catalogSupplier.get();
		List<CraftingRecipe> globalRecipes = getGlobalRecipes(catalog);
		return distinctRecipes(catalog.getCraftingUsagesFor(stack).stream()
				.flatMap(view -> view.variants().stream()
						.filter(variant -> !isDuplicateFocusedSourceRecipe(view, variant, stack, globalRecipes))
						.map(view.spec()::recipe))
				.toList());
	}

	@Override
	public List<CraftingRecipe> getRecipesForOutput(ITypedIngredient<?> output) {
		ItemStack stack = output.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		if (!focusedStackPredicate.test(stack)) {
			return List.of();
		}
		IRecipeViewerDisplayCatalog catalog = catalogSupplier.get();
		List<CraftingRecipe> globalRecipes = getGlobalRecipes(catalog);
		return distinctRecipes(catalog.getCraftingRecipesFor(stack).stream()
				.flatMap(view -> view.variants().stream()
						.filter(variant -> !hasSameDisplayRecipe(globalRecipes, view.spec().recipe(variant)))
						.map(view.spec()::recipe))
				.toList());
	}

	@Override
	public List<CraftingRecipe> getAllRecipes() {
		return distinctRecipes(catalogSupplier.get().getGlobalCraftingDisplays().stream()
				.filter(view -> view.spec().replacedRecipeIds().isEmpty())
				.flatMap(view -> view.variants().stream().map(view.spec()::recipe))
				.toList());
	}

	private static List<CraftingRecipe> distinctRecipes(List<CraftingRecipe> recipes) {
		Map<String, CraftingRecipe> distinctRecipes = new LinkedHashMap<>();
		for (CraftingRecipe recipe : recipes) {
			distinctRecipes.putIfAbsent(getRecipeKey(recipe), recipe);
		}
		return List.copyOf(distinctRecipes.values());
	}

	private static List<CraftingRecipe> getGlobalRecipes(IRecipeViewerDisplayCatalog catalog) {
		return catalog.getGlobalCraftingDisplays().stream()
				.flatMap(view -> view.variants().stream().map(view.spec()::recipe))
				.toList();
	}

	private static boolean isDuplicateFocusedSourceRecipe(CraftingDisplayView view, CraftingDisplayVariant variant, ItemStack focusedInput, List<CraftingRecipe> globalRecipes) {
		return hasSameDisplayRecipe(globalRecipes, view.spec().recipe(variant)) && isSourceFocus(view, variant, focusedInput);
	}

	private static boolean isSourceFocus(CraftingDisplayView view, CraftingDisplayVariant variant, ItemStack focusedInput) {
		return view.spec().focusBehavior() instanceof SourceResultFocusBehavior sourceResultFocusBehavior
				&& sourceResultFocusBehavior.sourceInputIndex() < variant.inputs().size()
				&& ItemStack.isSameItemSameTags(variant.inputs().get(sourceResultFocusBehavior.sourceInputIndex()), focusedInput);
	}

	private static String getRecipeKey(CraftingRecipe recipe) {
		StringBuilder key = new StringBuilder(recipe.getId().toString());
		recipe.getIngredients().forEach(ingredient -> {
			key.append('|');
			for (ItemStack item : ingredient.getItems()) {
				key.append(item).append(';');
			}
		});
		return key.toString();
	}

	private static boolean hasSameDisplayRecipe(List<CraftingRecipe> recipes, CraftingRecipe recipe) {
		return recipes.stream().anyMatch(otherRecipe -> hasSameDisplayRecipe(otherRecipe, recipe));
	}

	private static boolean hasSameDisplayRecipe(CraftingRecipe first, CraftingRecipe second) {
		return first.getId().equals(second.getId())
				&& ingredientsMatch(first, second)
				&& ItemStack.isSameItemSameTags(first.getResultItem(null), second.getResultItem(null));
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
				if (!ItemStack.isSameItemSameTags(firstItems[j], secondItems[j])) {
					return false;
				}
			}
		}
		return true;
	}
}
