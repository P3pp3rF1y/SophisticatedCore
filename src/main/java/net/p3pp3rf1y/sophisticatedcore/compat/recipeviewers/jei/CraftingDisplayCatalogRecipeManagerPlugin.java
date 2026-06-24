package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.*;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CraftingDisplayCatalogRecipeManagerPlugin implements ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> {
	private final Supplier<IRecipeViewerDisplayCatalog> catalogSupplier;
	private final Predicate<ItemStack> focusedStackPredicate;
	private final Map<String, List<RecipeHolder<CraftingRecipe>>> inputRecipesByFocus = new HashMap<>();
	private final Map<String, List<RecipeHolder<CraftingRecipe>>> outputRecipesByFocus = new HashMap<>();

	public CraftingDisplayCatalogRecipeManagerPlugin(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedStackPredicate) {
		this.catalogSupplier = catalogSupplier;
		this.focusedStackPredicate = focusedStackPredicate;
	}

	@Override
	public boolean isHandledInput(ITypedIngredient<?> input) {
		ItemStack stack = input.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return !getRecipesForInput(stack).isEmpty();
	}

	@Override
	public boolean isHandledOutput(ITypedIngredient<?> output) {
		ItemStack stack = output.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return !getRecipesForOutput(stack).isEmpty();
	}

	@Override
	public List<RecipeHolder<CraftingRecipe>> getRecipesForInput(ITypedIngredient<?> input) {
		ItemStack stack = input.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return getRecipesForInput(stack);
	}

	private List<RecipeHolder<CraftingRecipe>> getRecipesForInput(ItemStack stack) {
		if (!focusedStackPredicate.test(stack)) {
			return List.of();
		}
		return inputRecipesByFocus.computeIfAbsent(getFocusKey(stack), ignored -> createRecipesForInput(stack));
	}

	private List<RecipeHolder<CraftingRecipe>> createRecipesForInput(ItemStack stack) {
		IRecipeViewerDisplayCatalog catalog = catalogSupplier.get();
		List<RecipeHolder<CraftingRecipe>> globalRecipes = getGlobalRecipes(catalog);
		return distinctRecipes(catalog.getCraftingUsagesFor(stack).stream().flatMap(view -> recipeHoldersForInput(view, stack, globalRecipes)).toList());
	}

	@Override
	public List<RecipeHolder<CraftingRecipe>> getRecipesForOutput(ITypedIngredient<?> output) {
		ItemStack stack = output.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return getRecipesForOutput(stack);
	}

	private List<RecipeHolder<CraftingRecipe>> getRecipesForOutput(ItemStack stack) {
		if (!focusedStackPredicate.test(stack)) {
			return List.of();
		}
		return outputRecipesByFocus.computeIfAbsent(getFocusKey(stack), ignored -> createRecipesForOutput(stack));
	}

	private List<RecipeHolder<CraftingRecipe>> createRecipesForOutput(ItemStack stack) {
		IRecipeViewerDisplayCatalog catalog = catalogSupplier.get();
		List<RecipeHolder<CraftingRecipe>> globalRecipes = getGlobalRecipes(catalog);
		return distinctRecipes(
				catalog.getCraftingRecipesFor(stack).stream()
						.flatMap(view -> view.variants().stream().filter(variant -> !isReplaceableGlobalOutput(view, stack))
								.filter(variant -> !isDuplicateGlobalOutputRecipe(view, variant, stack, globalRecipes)).map(view.spec()::recipeHolder))
						.toList());
	}

	@Override
	public List<RecipeHolder<CraftingRecipe>> getAllRecipes() {
		IRecipeViewerDisplayCatalog catalog = catalogSupplier.get();
		return distinctRecipes(
				catalog.getGlobalCraftingDisplays().stream().filter(view -> !(view.spec().focusBehavior() instanceof IGroupedOutputFocusBehavior))
						.filter(view -> !(view.spec().focusBehavior() instanceof SourceResultFocusBehavior))
						.filter(view -> view.spec().replacedRecipeIds().isEmpty()).map(view -> view.spec().recipeHolder(view.variants())).toList());
	}

	private static List<RecipeHolder<CraftingRecipe>> distinctRecipes(List<RecipeHolder<CraftingRecipe>> recipes) {
		Map<String, RecipeHolder<CraftingRecipe>> distinctRecipes = new LinkedHashMap<>();
		for (RecipeHolder<CraftingRecipe> recipeHolder : recipes) {
			distinctRecipes.putIfAbsent(getRecipeKey(recipeHolder), recipeHolder);
		}
		return List.copyOf(distinctRecipes.values());
	}

	private static List<RecipeHolder<CraftingRecipe>> getGlobalRecipes(IRecipeViewerDisplayCatalog catalog) {
		return catalog.getGlobalCraftingDisplays().stream().flatMap(view -> view.variants().stream().map(view.spec()::recipeHolder)).toList();
	}

	private static Stream<RecipeHolder<CraftingRecipe>> recipeHoldersForInput(CraftingDisplayView view, ItemStack stack,
			List<RecipeHolder<CraftingRecipe>> globalRecipes) {
		if (!view.spec().replacedRecipeIds().isEmpty() && shouldSuppressSyntheticInput(stack)) {
			return Stream.empty();
		}
		if (!view.spec().replacedRecipeIds().isEmpty() && view.spec().focusBehavior() instanceof IGroupedOutputFocusBehavior && isGlobalSource(view, stack)) {
			return Stream.empty();
		}
		if (view.spec().focusBehavior() instanceof IGroupedOutputFocusBehavior) {
			return Stream.of(view.spec().recipeHolder(view.variants()));
		}
		return view.variants().stream().filter(variant -> !isDuplicateFocusedSourceRecipe(view, variant, stack, globalRecipes)).map(view.spec()::recipeHolder);
	}

	private static boolean isDuplicateGlobalOutputRecipe(CraftingDisplayView view, CraftingDisplayVariant variant, ItemStack focusedOutput,
			List<RecipeHolder<CraftingRecipe>> globalRecipes) {
		return focusedOutput.getComponentsPatch().isEmpty() && hasSameDisplayRecipe(globalRecipes, view.spec().recipeHolder(variant));
	}

	private static boolean isReplaceableGlobalOutput(CraftingDisplayView view, ItemStack focusedOutput) {
		return !view.spec().replacedRecipeIds().isEmpty() && shouldSuppressSyntheticReplacement(focusedOutput);
	}

	private static boolean hasOnlyRenderInfo(ItemStack stack) {
		return stack.has(ModCoreDataComponents.RENDER_INFO_TAG) && !stack.has(ModCoreDataComponents.MAIN_COLOR)
				&& !stack.has(ModCoreDataComponents.ACCENT_COLOR);
	}

	private static boolean shouldSuppressSyntheticInput(ItemStack stack) {
		return stack.getComponentsPatch().isEmpty() || hasOnlyRenderInfo(stack) || (!SyntheticDisplayComponents.hasAny(stack) && hasCoreRecipeMetadata(stack));
	}

	private static boolean shouldSuppressSyntheticReplacement(ItemStack stack) {
		return stack.getComponentsPatch().isEmpty() || hasOnlyRenderInfo(stack) || !SyntheticDisplayComponents.hasAny(stack);
	}

	private static boolean hasCoreRecipeMetadata(ItemStack stack) {
		return stack.has(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS) || stack.has(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS)
				|| stack.has(ModCoreDataComponents.STORAGE_UUID);
	}

	private static boolean isGlobalSource(CraftingDisplayView view, ItemStack focusedInput) {
		return view.spec().focusBehavior() instanceof SourceResultFocusBehavior sourceResultFocusBehavior
				&& view.spec().getGlobalDisplays().stream().filter(variant -> sourceResultFocusBehavior.sourceInputIndex() < variant.inputs().size())
						.map(variant -> variant.inputs().get(sourceResultFocusBehavior.sourceInputIndex()))
						.anyMatch(source -> ItemStack.isSameItemSameComponents(source, focusedInput));
	}

	private static boolean isDuplicateFocusedSourceRecipe(CraftingDisplayView view, CraftingDisplayVariant variant, ItemStack focusedInput,
			List<RecipeHolder<CraftingRecipe>> globalRecipes) {
		return shouldSuppressSyntheticReplacement(focusedInput) && hasSameDisplayRecipe(globalRecipes, view.spec().recipeHolder(variant))
				&& isSourceFocus(view, variant, focusedInput);
	}

	private static boolean isSourceFocus(CraftingDisplayView view, CraftingDisplayVariant variant, ItemStack focusedInput) {
		return view.spec().focusBehavior() instanceof SourceResultFocusBehavior sourceResultFocusBehavior
				&& sourceResultFocusBehavior.sourceInputIndex() < variant.inputs().size()
				&& ItemStack.isSameItemSameComponents(variant.inputs().get(sourceResultFocusBehavior.sourceInputIndex()), focusedInput);
	}

	private static String getRecipeKey(RecipeHolder<CraftingRecipe> recipeHolder) {
		StringBuilder key = new StringBuilder(recipeHolder.id().toString());
		key.append('|').append(ClientRecipeHelper.getResultItem(recipeHolder.value()));
		getIngredients(recipeHolder.value()).forEach(ingredient -> {
			key.append('|');
			for (ItemStack item : ingredient.items().map(ItemStack::new).toList()) {
				key.append(item).append(';');
			}
		});
		return key.toString();
	}

	private static boolean hasSameDisplayRecipe(List<RecipeHolder<CraftingRecipe>> recipes, RecipeHolder<CraftingRecipe> recipeHolder) {
		return recipes.stream().anyMatch(recipe -> hasSameDisplayRecipe(recipe, recipeHolder));
	}

	private static boolean hasSameDisplayRecipe(RecipeHolder<CraftingRecipe> first, RecipeHolder<CraftingRecipe> second) {
		return first.id().equals(second.id()) && ingredientsMatch(first.value(), second.value())
				&& ItemStack.isSameItemSameComponents(ClientRecipeHelper.getResultItem(first.value()), ClientRecipeHelper.getResultItem(second.value()));
	}

	private static String getFocusKey(ItemStack stack) {
		return stack.getItem() + "|" + stack.getComponents();
	}

	private static boolean ingredientsMatch(CraftingRecipe first, CraftingRecipe second) {
		List<Ingredient> firstIngredients = getIngredients(first);
		List<Ingredient> secondIngredients = getIngredients(second);
		if (firstIngredients.size() != secondIngredients.size()) {
			return false;
		}
		for (int i = 0; i < firstIngredients.size(); i++) {
			List<ItemStack> firstItems = firstIngredients.get(i).items().map(ItemStack::new).toList();
			List<ItemStack> secondItems = secondIngredients.get(i).items().map(ItemStack::new).toList();
			if (firstItems.size() != secondItems.size()) {
				return false;
			}
			for (int j = 0; j < firstItems.size(); j++) {
				if (!ItemStack.isSameItemSameComponents(firstItems.get(j), secondItems.get(j))) {
					return false;
				}
			}
		}
		return true;
	}

	private static List<Ingredient> getIngredients(CraftingRecipe recipe) {
		if (recipe instanceof ShapedRecipe shapedRecipe) {
			return shapedRecipe.getIngredients().stream().flatMap(Optional::stream).toList();
		}
		if (recipe instanceof ShapelessRecipe) {
			return recipe.placementInfo().ingredients();
		}
		return recipe.placementInfo().ingredients();
	}

}
