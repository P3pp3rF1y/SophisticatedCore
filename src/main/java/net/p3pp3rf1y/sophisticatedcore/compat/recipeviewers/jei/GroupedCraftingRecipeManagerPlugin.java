package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplaySpec;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class GroupedCraftingRecipeManagerPlugin implements ISimpleRecipeManagerPlugin<CraftingRecipe> {
	private final Supplier<List<? extends IRecipeViewerDisplaySpec<GroupedCraftingRecipe>>> specsSupplier;
	private final Predicate<ItemStack> focusedStackPredicate;
	private final Predicate<ItemStack> focusedOutputPredicate;

	public GroupedCraftingRecipeManagerPlugin(Supplier<List<? extends IRecipeViewerDisplaySpec<GroupedCraftingRecipe>>> specsSupplier,
			Predicate<ItemStack> focusedStackPredicate) {
		this(specsSupplier, focusedStackPredicate, focusedStackPredicate);
	}

	public GroupedCraftingRecipeManagerPlugin(Supplier<List<? extends IRecipeViewerDisplaySpec<GroupedCraftingRecipe>>> specsSupplier,
			Predicate<ItemStack> focusedStackPredicate, Predicate<ItemStack> focusedOutputPredicate) {
		this.specsSupplier = specsSupplier;
		this.focusedStackPredicate = focusedStackPredicate;
		this.focusedOutputPredicate = focusedOutputPredicate;
	}

	@Override
	public boolean isHandledInput(ITypedIngredient<?> input) {
		ItemStack stack = input.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return focusedStackPredicate.test(stack) && specsSupplier.get().stream().anyMatch(spec -> !spec.getUsagesFor(stack).isEmpty());
	}

	@Override
	public boolean isHandledOutput(ITypedIngredient<?> output) {
		ItemStack stack = output.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return focusedOutputPredicate.test(stack) && specsSupplier.get().stream().anyMatch(spec -> !spec.getRecipesFor(stack).isEmpty());
	}

	@Override
	public List<CraftingRecipe> getRecipesForInput(ITypedIngredient<?> input) {
		ItemStack stack = input.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return specsSupplier.get().stream().flatMap(spec -> spec.getUsagesFor(stack).stream()).map(recipe -> (CraftingRecipe) recipe).toList();
	}

	@Override
	public List<CraftingRecipe> getRecipesForOutput(ITypedIngredient<?> output) {
		ItemStack stack = output.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		if (!focusedOutputPredicate.test(stack)) {
			return List.of();
		}
		return specsSupplier.get().stream().flatMap(spec -> spec.getRecipesFor(stack).stream()).map(recipe -> (CraftingRecipe) recipe).toList();
	}

	@Override
	public List<CraftingRecipe> getAllRecipes() {
		return specsSupplier.get().stream().flatMap(spec -> spec.getAllDisplays().stream()).map(recipe -> (CraftingRecipe) recipe).toList();
	}
}
