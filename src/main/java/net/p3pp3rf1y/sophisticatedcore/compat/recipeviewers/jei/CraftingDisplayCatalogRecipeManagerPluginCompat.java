package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CraftingDisplayCatalogRecipeManagerPluginCompat implements IRecipeManagerPlugin {
	private final Supplier<IRecipeViewerDisplayCatalog> catalogSupplier;
	private final Predicate<ItemStack> focusedInputPredicate;
	private final Predicate<ItemStack> focusedOutputPredicate;
	private final Map<String, List<CraftingRecipe>> inputRecipesByFocus = new HashMap<>();
	private final Map<String, List<CraftingRecipe>> outputRecipesByFocus = new HashMap<>();

	public CraftingDisplayCatalogRecipeManagerPluginCompat(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier) {
		this(catalogSupplier, stack -> true);
	}

	public CraftingDisplayCatalogRecipeManagerPluginCompat(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedStackPredicate) {
		this(catalogSupplier, focusedStackPredicate, focusedStackPredicate);
	}

	public CraftingDisplayCatalogRecipeManagerPluginCompat(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedInputPredicate,
			Predicate<ItemStack> focusedOutputPredicate) {
		this.catalogSupplier = catalogSupplier;
		this.focusedInputPredicate = focusedInputPredicate;
		this.focusedOutputPredicate = focusedOutputPredicate;
		RecipeHelper.addRecipeChangeListener(this::clearCaches);
	}

	private void clearCaches() {
		inputRecipesByFocus.clear();
		outputRecipesByFocus.clear();
	}

	@Override
	public <V> List<mezz.jei.api.recipe.RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
		return focus.checkedCast(VanillaTypes.ITEM_STACK).filter(this::isHandledFocus)
				.map(ignored -> List.<mezz.jei.api.recipe.RecipeType<?>>of(RecipeTypes.CRAFTING)).orElse(List.of());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, V> List<T> getRecipes(IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
		if (!RecipeTypes.CRAFTING.equals(recipeCategory.getRecipeType())) {
			return List.of();
		}

		return focus.checkedCast(VanillaTypes.ITEM_STACK).map(itemFocus -> (List<T>) getRecipesForFocus(itemFocus)).orElse(List.of());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> getRecipes(IRecipeCategory<T> recipeCategory) {
		if (!RecipeTypes.CRAFTING.equals(recipeCategory.getRecipeType())) {
			return List.of();
		}

		return (List<T>) distinctRecipes(catalogSupplier.get().getGlobalCraftingDisplays().stream().filter(view -> view.spec().replacedRecipeIds().isEmpty())
				.flatMap(view -> view.variants().stream().map(view.spec()::recipe)).toList());
	}

	private boolean isHandledFocus(IFocus<ItemStack> focus) {
		ItemStack stack = focus.getTypedValue().getIngredient();
		return switch (focus.getRole()) {
			case INPUT -> !getRecipesForInput(stack).isEmpty();
			case OUTPUT -> !getRecipesForOutput(stack).isEmpty();
			default -> false;
		};
	}

	private List<CraftingRecipe> getRecipesForFocus(IFocus<ItemStack> focus) {
		ItemStack stack = focus.getTypedValue().getIngredient();
		if (focus.getRole() == RecipeIngredientRole.INPUT) {
			return getRecipesForInput(stack);
		}
		if (focus.getRole() == RecipeIngredientRole.OUTPUT) {
			return getRecipesForOutput(stack);
		}
		return List.of();
	}

	private List<CraftingRecipe> getRecipesForInput(ItemStack stack) {
		if (!focusedInputPredicate.test(stack)) {
			return List.of();
		}
		return inputRecipesByFocus.computeIfAbsent(getFocusKey(stack), ignored -> distinctRecipes(
				catalogSupplier.get().getCraftingUsagesFor(stack).stream().flatMap(view -> view.variants().stream().map(view.spec()::recipe)).toList()));
	}

	private List<CraftingRecipe> getRecipesForOutput(ItemStack stack) {
		if (!focusedOutputPredicate.test(stack)) {
			return List.of();
		}
		return outputRecipesByFocus.computeIfAbsent(getFocusKey(stack), ignored -> distinctRecipes(
				catalogSupplier.get().getCraftingRecipesFor(stack).stream().flatMap(view -> view.variants().stream().map(view.spec()::recipe)).toList()));
	}

	private static List<CraftingRecipe> distinctRecipes(List<CraftingRecipe> recipes) {
		Map<String, CraftingRecipe> distinctRecipes = new LinkedHashMap<>();
		for (CraftingRecipe recipe : recipes) {
			distinctRecipes.putIfAbsent(getRecipeKey(recipe), recipe);
		}
		return List.copyOf(distinctRecipes.values());
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

	private static String getFocusKey(ItemStack stack) {
		return stack.getItem() + "|" + stack.getTag();
	}
}
