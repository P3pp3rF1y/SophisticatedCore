package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayVariant;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayView;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IGroupedOutputFocusBehavior;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SourceResultFocusBehavior;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CraftingDisplayCatalogRecipeManagerPlugin implements ISimpleRecipeManagerPlugin<CraftingRecipe> {
	private static final String MAIN_COLOR_TAG = "mainColor";
	private static final String ACCENT_COLOR_TAG = "accentColor";
	private static final String CLOTH_COLOR_TAG = "clothColor";
	private static final String BORDER_COLOR_TAG = "borderColor";
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
		return distinctRecipes(
				catalog.getCraftingUsagesFor(stack).stream().filter(view -> view.spec().replacedRecipeIds().isEmpty() || hasSyntheticColors(stack))
						.filter(view -> !isDuplicateFocusedSourceRecipe(view, view.variants().get(0), stack, globalRecipes))
						.map(view -> view.spec().recipe(view.variants())).toList());
	}

	@Override
	public List<CraftingRecipe> getRecipesForOutput(ITypedIngredient<?> output) {
		ItemStack stack = output.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		if (!focusedStackPredicate.test(stack)) {
			return List.of();
		}
		IRecipeViewerDisplayCatalog catalog = catalogSupplier.get();
		return distinctRecipes(catalog.getCraftingRecipesFor(stack).stream().filter(view -> !isReplaceableVanillaOutputDisplay(view, stack))
				.map(view -> view.spec().recipe(view.variants())).toList());
	}

	@Override
	public List<CraftingRecipe> getAllRecipes() {
		IRecipeViewerDisplayCatalog catalog = catalogSupplier.get();
		List<CraftingRecipe> recipes = new ArrayList<>(
				catalog.getGlobalCraftingDisplays().stream().filter(view -> !(view.spec().focusBehavior() instanceof IGroupedOutputFocusBehavior))
						.filter(view -> view.spec().replacedRecipeIds().isEmpty()).map(view -> view.spec().recipe(view.variants())).toList());
		catalog.getCraftingSpecs().stream().filter(spec -> spec.focusBehavior() instanceof IGroupedOutputFocusBehavior)
				.filter(spec -> !spec.getGlobalDisplays().isEmpty()).map(CraftingDisplayCatalogRecipeManagerPlugin::globalDisplaysRecipe).forEach(recipes::add);
		return distinctRecipes(recipes);
	}

	private static CraftingRecipe globalDisplaysRecipe(CraftingDisplaySpec spec) {
		return spec.recipe(spec.getGlobalDisplays());
	}

	private static List<CraftingRecipe> distinctRecipes(List<CraftingRecipe> recipes) {
		Map<String, CraftingRecipe> distinctRecipes = new LinkedHashMap<>();
		for (CraftingRecipe recipe : recipes) {
			distinctRecipes.putIfAbsent(getRecipeKey(recipe), recipe);
		}
		return List.copyOf(distinctRecipes.values());
	}

	private static List<CraftingRecipe> getGlobalRecipes(IRecipeViewerDisplayCatalog catalog) {
		return catalog.getGlobalCraftingDisplays().stream().map(view -> view.spec().recipe(view.variants())).toList();
	}

	private static boolean isDuplicateFocusedSourceRecipe(CraftingDisplayView view, CraftingDisplayVariant variant, ItemStack focusedInput,
			List<CraftingRecipe> globalRecipes) {
		return hasSameDisplayRecipe(globalRecipes, view.spec().recipe(variant)) && isSourceFocus(view, variant, focusedInput);
	}

	private static boolean isReplaceableVanillaOutputDisplay(CraftingDisplayView view, ItemStack focusedStack) {
		return !hasSyntheticColors(focusedStack) && !(view.spec().focusBehavior() instanceof IGroupedOutputFocusBehavior)
				&& !view.spec().replacedRecipeIds().isEmpty();
	}

	private static boolean hasSyntheticColors(ItemStack stack) {
		return stack.getTag() != null && (stack.getTag().contains(MAIN_COLOR_TAG) || stack.getTag().contains(ACCENT_COLOR_TAG)
				|| stack.getTag().contains(CLOTH_COLOR_TAG) || stack.getTag().contains(BORDER_COLOR_TAG));
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
		return first.getId().equals(second.getId()) && ingredientsMatch(first, second)
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
