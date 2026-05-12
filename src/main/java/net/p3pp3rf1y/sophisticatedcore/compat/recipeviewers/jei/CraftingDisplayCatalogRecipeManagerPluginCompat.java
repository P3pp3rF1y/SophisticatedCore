package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CraftingDisplayCatalogRecipeManagerPluginCompat implements IRecipeManagerPlugin {
	private final Supplier<IRecipeViewerDisplayCatalog> catalogSupplier;
	private final Predicate<ItemStack> focusedStackPredicate;

	public CraftingDisplayCatalogRecipeManagerPluginCompat(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier) {
		this(catalogSupplier, stack -> true);
	}

	public CraftingDisplayCatalogRecipeManagerPluginCompat(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedStackPredicate) {
		this.catalogSupplier = catalogSupplier;
		this.focusedStackPredicate = focusedStackPredicate;
	}

	@Override
	public <V> List<mezz.jei.api.recipe.RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
		return focus.checkedCast(VanillaTypes.ITEM_STACK)
				.filter(this::isHandledFocus)
				.map(ignored -> List.<mezz.jei.api.recipe.RecipeType<?>>of(RecipeTypes.CRAFTING))
				.orElse(List.of());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, V> List<T> getRecipes(IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
		if (!RecipeTypes.CRAFTING.equals(recipeCategory.getRecipeType())) {
			return List.of();
		}

		return focus.checkedCast(VanillaTypes.ITEM_STACK)
				.map(itemFocus -> (List<T>) getRecipesForFocus(itemFocus))
				.orElse(List.of());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> getRecipes(IRecipeCategory<T> recipeCategory) {
		if (!RecipeTypes.CRAFTING.equals(recipeCategory.getRecipeType())) {
			return List.of();
		}

		return (List<T>) distinctRecipes(catalogSupplier.get().getGlobalCraftingDisplays().stream()
				.flatMap(view -> view.variants().stream().map(view.spec()::recipeHolder))
				.toList());
	}

	private boolean isHandledFocus(IFocus<ItemStack> focus) {
		ItemStack stack = focus.getTypedValue().getIngredient();
		if (!focusedStackPredicate.test(stack)) {
			return false;
		}
		return switch (focus.getRole()) {
			case INPUT -> !catalogSupplier.get().getCraftingUsagesFor(stack).isEmpty();
			case OUTPUT -> !catalogSupplier.get().getCraftingRecipesFor(stack).isEmpty();
			default -> false;
		};
	}

	private List<RecipeHolder<CraftingRecipe>> getRecipesForFocus(IFocus<ItemStack> focus) {
		ItemStack stack = focus.getTypedValue().getIngredient();
		if (!focusedStackPredicate.test(stack)) {
			return List.of();
		}
		if (focus.getRole() == RecipeIngredientRole.INPUT) {
			return distinctRecipes(catalogSupplier.get().getCraftingUsagesFor(stack).stream()
					.flatMap(view -> view.variants().stream().map(view.spec()::recipeHolder))
					.toList());
		}
		if (focus.getRole() == RecipeIngredientRole.OUTPUT) {
			return distinctRecipes(catalogSupplier.get().getCraftingRecipesFor(stack).stream()
					.flatMap(view -> view.variants().stream().map(view.spec()::recipeHolder))
					.toList());
		}
		return List.of();
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
		recipeHolder.value().getIngredients().forEach(ingredient -> {
			key.append('|');
			for (ItemStack item : ingredient.getItems()) {
				key.append(item).append(';');
			}
		});
		return key.toString();
	}
}
