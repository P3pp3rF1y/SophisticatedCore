package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CraftingDisplayCatalogRecipeManagerPluginCompat implements IRecipeManagerPlugin {
	private final Supplier<IRecipeViewerDisplayCatalog> catalogSupplier;
	private final Predicate<ItemStack> focusedInputPredicate;
	private final Predicate<ItemStack> focusedOutputPredicate;
	private final Map<String, List<RecipeHolder<CraftingRecipe>>> inputRecipesByFocus = new HashMap<>();
	private final Map<String, List<RecipeHolder<CraftingRecipe>>> outputRecipesByFocus = new HashMap<>();

	public CraftingDisplayCatalogRecipeManagerPluginCompat(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier) {
		this(catalogSupplier, stack -> true);
	}

	public CraftingDisplayCatalogRecipeManagerPluginCompat(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedStackPredicate) {
		this(catalogSupplier, focusedStackPredicate, focusedStackPredicate);
	}

	public CraftingDisplayCatalogRecipeManagerPluginCompat(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedInputPredicate, Predicate<ItemStack> focusedOutputPredicate) {
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
	public <V> List<IRecipeType<?>> getRecipeTypes(IFocus<V> focus) {
		return focus.checkedCast(VanillaTypes.ITEM_STACK)
				.filter(this::isHandledFocus)
				.map(ignored -> List.<IRecipeType<?>>of(RecipeTypes.CRAFTING))
				.orElse(List.of());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, V> List<T> getRecipes(IRecipeType<T> recipeType, IFocus<V> focus) {
		if (!RecipeTypes.CRAFTING.equals(recipeType)) {
			return List.of();
		}

		return focus.checkedCast(VanillaTypes.ITEM_STACK)
				.map(itemFocus -> (List<T>) getRecipesForFocus(itemFocus))
				.orElse(List.of());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> getRecipes(IRecipeType<T> recipeType) {
		if (!RecipeTypes.CRAFTING.equals(recipeType)) {
			return List.of();
		}

		return (List<T>) distinctRecipes(catalogSupplier.get().getGlobalCraftingDisplays().stream()
				.flatMap(view -> view.variants().stream().map(view.spec()::recipeHolder))
				.toList());
	}

	private boolean isHandledFocus(IFocus<ItemStack> focus) {
		ItemStack stack = focus.getTypedValue().getIngredient();
		return switch (focus.getRole()) {
			case INPUT -> !getRecipesForInput(stack).isEmpty();
			case OUTPUT -> !getRecipesForOutput(stack).isEmpty();
			default -> false;
		};
	}

	private List<RecipeHolder<CraftingRecipe>> getRecipesForFocus(IFocus<ItemStack> focus) {
		ItemStack stack = focus.getTypedValue().getIngredient();
		if (focus.getRole() == RecipeIngredientRole.INPUT) {
			return getRecipesForInput(stack);
		}
		if (focus.getRole() == RecipeIngredientRole.OUTPUT) {
			return getRecipesForOutput(stack);
		}
		return List.of();
	}

	private List<RecipeHolder<CraftingRecipe>> getRecipesForInput(ItemStack stack) {
		if (!focusedInputPredicate.test(stack)) {
			return List.of();
		}
		return inputRecipesByFocus.computeIfAbsent(getFocusKey(stack), ignored -> distinctRecipes(catalogSupplier.get().getCraftingUsagesFor(stack).stream()
				.flatMap(view -> view.variants().stream().map(view.spec()::recipeHolder))
				.toList()));
	}

	private List<RecipeHolder<CraftingRecipe>> getRecipesForOutput(ItemStack stack) {
		if (!focusedOutputPredicate.test(stack)) {
			return List.of();
		}
		return outputRecipesByFocus.computeIfAbsent(getFocusKey(stack), ignored -> distinctRecipes(catalogSupplier.get().getCraftingRecipesFor(stack).stream()
				.flatMap(view -> view.variants().stream().map(view.spec()::recipeHolder))
				.toList()));
	}

	private static List<RecipeHolder<CraftingRecipe>> distinctRecipes(List<RecipeHolder<CraftingRecipe>> recipes) {
		Map<String, RecipeHolder<CraftingRecipe>> distinctRecipes = new LinkedHashMap<>();
		for (RecipeHolder<CraftingRecipe> recipeHolder : recipes) {
			distinctRecipes.putIfAbsent(getRecipeKey(recipeHolder), recipeHolder);
		}
		return List.copyOf(distinctRecipes.values());
	}

	private static String getRecipeKey(RecipeHolder<CraftingRecipe> recipeHolder) {
		StringBuilder key = new StringBuilder(recipeHolder.id().toString());
		getIngredients(recipeHolder.value()).forEach(ingredient -> {
			key.append('|');
			for (ItemStack item : ingredient.items().map(ItemStack::new).toList()) {
				key.append(item).append(';');
			}
		});
		return key.toString();
	}

	private static String getFocusKey(ItemStack stack) {
		return stack.getItem() + "|" + stack.getComponents();
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
