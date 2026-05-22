package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SmithingSpecRecipeManagerPlugin implements ISimpleRecipeManagerPlugin<SmithingRecipe> {
	private final Supplier<IRecipeViewerDisplayCatalog> catalogSupplier;
	private final Predicate<ItemStack> focusedInputPredicate;
	private final Predicate<ItemStack> focusedOutputPredicate;

	public SmithingSpecRecipeManagerPlugin(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedStackPredicate) {
		this(catalogSupplier, focusedStackPredicate, focusedStackPredicate);
	}

	public SmithingSpecRecipeManagerPlugin(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedInputPredicate, Predicate<ItemStack> focusedOutputPredicate) {
		this.catalogSupplier = catalogSupplier;
		this.focusedInputPredicate = focusedInputPredicate;
		this.focusedOutputPredicate = focusedOutputPredicate;
	}

	@Override
	public boolean isHandledInput(ITypedIngredient<?> input) {
		ItemStack stack = input.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return focusedInputPredicate.test(stack) && !catalogSupplier.get().getSmithingUsagesFor(stack).isEmpty();
	}

	@Override
	public boolean isHandledOutput(ITypedIngredient<?> output) {
		ItemStack stack = output.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return focusedOutputPredicate.test(stack) && !catalogSupplier.get().getSmithingRecipesFor(stack).isEmpty();
	}

	@Override
	public List<SmithingRecipe> getRecipesForInput(ITypedIngredient<?> input) {
		ItemStack stack = input.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		if (!focusedInputPredicate.test(stack)) {
			return List.of();
		}
		return catalogSupplier.get().getSmithingUsagesFor(stack).stream()
				.flatMap(view -> view.variants().stream().map(view.spec()::recipe))
				.toList();
	}

	@Override
	public List<SmithingRecipe> getRecipesForOutput(ITypedIngredient<?> output) {
		ItemStack stack = output.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		if (!focusedOutputPredicate.test(stack)) {
			return List.of();
		}
		return catalogSupplier.get().getSmithingRecipesFor(stack).stream()
				.flatMap(view -> view.variants().stream().map(view.spec()::recipe))
				.toList();
	}

	@Override
	public List<SmithingRecipe> getAllRecipes() {
		return List.of();
	}
}
