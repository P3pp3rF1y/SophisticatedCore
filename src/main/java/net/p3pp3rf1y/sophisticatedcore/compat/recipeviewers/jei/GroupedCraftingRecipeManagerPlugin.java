package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplaySpec;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class GroupedCraftingRecipeManagerPlugin implements ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> {
	private final Supplier<List<? extends IRecipeViewerDisplaySpec<RecipeHolder<GroupedCraftingRecipe>>>> specsSupplier;
	private final Predicate<ItemStack> focusedStackPredicate;

	public GroupedCraftingRecipeManagerPlugin(Supplier<List<? extends IRecipeViewerDisplaySpec<RecipeHolder<GroupedCraftingRecipe>>>> specsSupplier, Predicate<ItemStack> focusedStackPredicate) {
		this.specsSupplier = specsSupplier;
		this.focusedStackPredicate = focusedStackPredicate;
	}

	@Override
	public boolean isHandledInput(ITypedIngredient<?> input) {
		ItemStack stack = input.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return focusedStackPredicate.test(stack) && specsSupplier.get().stream().anyMatch(spec -> !spec.getUsagesFor(stack).isEmpty());
	}

	@Override
	public boolean isHandledOutput(ITypedIngredient<?> output) {
		ItemStack stack = output.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return focusedStackPredicate.test(stack) && specsSupplier.get().stream().anyMatch(spec -> !spec.getRecipesFor(stack).isEmpty());
	}

	@Override
	public List<RecipeHolder<CraftingRecipe>> getRecipesForInput(ITypedIngredient<?> input) {
		ItemStack stack = input.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return specsSupplier.get().stream()
				.flatMap(spec -> spec.getUsagesFor(stack).stream())
				.map(GroupedCraftingRecipeManagerPlugin::toCraftingRecipeHolder)
				.toList();
	}

	@Override
	public List<RecipeHolder<CraftingRecipe>> getRecipesForOutput(ITypedIngredient<?> output) {
		ItemStack stack = output.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
		return specsSupplier.get().stream()
				.flatMap(spec -> spec.getRecipesFor(stack).stream())
				.map(GroupedCraftingRecipeManagerPlugin::toCraftingRecipeHolder)
				.toList();
	}

	@Override
	public List<RecipeHolder<CraftingRecipe>> getAllRecipes() {
		return specsSupplier.get().stream()
				.flatMap(spec -> spec.getAllDisplays().stream())
				.map(GroupedCraftingRecipeManagerPlugin::toCraftingRecipeHolder)
				.toList();
	}

	private static RecipeHolder<CraftingRecipe> toCraftingRecipeHolder(RecipeHolder<GroupedCraftingRecipe> recipeHolder) {
		return new RecipeHolder<>(recipeHolder.id(), recipeHolder.value());
	}
}
