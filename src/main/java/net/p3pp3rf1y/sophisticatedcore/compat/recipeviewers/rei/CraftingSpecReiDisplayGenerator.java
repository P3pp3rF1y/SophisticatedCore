package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayView;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CraftingSpecReiDisplayGenerator implements DynamicDisplayGenerator<Display> {
	private final Supplier<IRecipeViewerDisplayCatalog> catalogSupplier;
	private final Predicate<ItemStack> focusedStackPredicate;

	public CraftingSpecReiDisplayGenerator(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedStackPredicate) {
		this.catalogSupplier = catalogSupplier;
		this.focusedStackPredicate = focusedStackPredicate;
	}

	@Override
	public Optional<List<Display>> getRecipeFor(EntryStack<?> entry) {
		if (!(entry.getValue() instanceof ItemStack stack) || !focusedStackPredicate.test(stack)) {
			return Optional.empty();
		}

		List<Display> displays = new ArrayList<>(catalogSupplier.get().getCraftingRecipesFor(stack).stream()
				.map(CraftingSpecReiDisplayGenerator::toDisplay)
				.toList());
		return displays.isEmpty() ? Optional.empty() : Optional.of(displays);
	}

	@Override
	public Optional<List<Display>> getUsageFor(EntryStack<?> entry) {
		if (!(entry.getValue() instanceof ItemStack stack) || !focusedStackPredicate.test(stack)) {
			return Optional.empty();
		}

		List<Display> displays = new ArrayList<>(catalogSupplier.get().getCraftingUsagesFor(stack).stream()
				.map(CraftingSpecReiDisplayGenerator::toDisplay)
				.toList());
		return displays.isEmpty() ? Optional.empty() : Optional.of(displays);
	}

	@Override
	public Optional<List<Display>> generate(ViewSearchBuilder builder) {
		if (!builder.getRecipesFor().isEmpty() || !builder.getUsagesFor().isEmpty()) {
			return Optional.empty();
		}

		List<Display> displays = new ArrayList<>(catalogSupplier.get().getGlobalCraftingDisplays().stream().map(CraftingSpecReiDisplayGenerator::toDisplay).toList());
		catalogSupplier.get().getCraftingRecipes().stream()
				.map(CraftingSpecReiDisplayGenerator::toDisplay)
				.filter(Objects::nonNull)
				.forEach(displays::add);
		return displays.isEmpty() ? Optional.empty() : Optional.of(displays);
	}

	private static CraftingSpecReiDisplay toDisplay(CraftingDisplayView view) {
		return new CraftingSpecReiDisplay(view.spec(), view.variants());
	}

	@SuppressWarnings("unchecked")
	private static Display toDisplay(RecipeHolder<CraftingRecipe> recipeHolder) {
		return new CatalogCraftingReiDisplay((RecipeHolder<Recipe<?>>) (RecipeHolder<?>) recipeHolder);
	}

	private static class CatalogCraftingReiDisplay extends DefaultCraftingDisplay<Recipe<?>> {
		private final CraftingRecipe recipe;

		private CatalogCraftingReiDisplay(RecipeHolder<Recipe<?>> recipeHolder) {
			super(getInputs(recipeHolder.value()), List.of(EntryIngredients.of(ClientRecipeHelper.getResultItem(recipeHolder.value()))), Optional.of(recipeHolder));
			this.recipe = (CraftingRecipe) recipeHolder.value();
		}

		@Override
		public int getWidth() {
			return recipe instanceof ShapedRecipe shapedRecipe ? shapedRecipe.getWidth() : getInputEntries().size() > 4 ? 3 : 2;
		}

		@Override
		public int getHeight() {
			return recipe instanceof ShapedRecipe shapedRecipe ? shapedRecipe.getHeight() : getInputEntries().size() > 4 ? 3 : 2;
		}

		@Override
		public int getInputWidth(int craftingWidth, int craftingHeight) {
			return recipe instanceof ShapedRecipe shapedRecipe ? shapedRecipe.getWidth() : craftingWidth * craftingHeight <= getInputEntries().size() ? craftingWidth : Math.min(getInputEntries().size(), 3);
		}

		@Override
		public int getInputHeight(int craftingWidth, int craftingHeight) {
			return recipe instanceof ShapedRecipe shapedRecipe ? shapedRecipe.getHeight() : (int) Math.ceil(getInputEntries().size() / (double) getInputWidth(craftingWidth, craftingHeight));
		}

		@Override
		public boolean isShapeless() {
			return !(recipe instanceof ShapedRecipe);
		}
	}

	private static List<EntryIngredient> getInputs(Recipe<?> recipe) {
		return EntryIngredients.ofIngredients(recipe.getIngredients());
	}
}
