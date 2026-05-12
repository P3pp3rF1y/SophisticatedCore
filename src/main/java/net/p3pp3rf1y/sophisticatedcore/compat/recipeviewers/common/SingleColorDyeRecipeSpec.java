package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.function.BiPredicate;

public record SingleColorDyeRecipeSpec(ResourceLocation id, List<ItemStack> sourceStacks, List<DyeVariantPair> variantPairs, BiPredicate<ItemStack, ItemStack> resultMatcher)
		implements IRecipeViewerDisplaySpec<RecipeHolder<GroupedCraftingRecipe>> {
	public SingleColorDyeRecipeSpec(ResourceLocation id, List<ItemStack> sourceStacks, List<DyeVariantPair> variantPairs) {
		this(id, sourceStacks, variantPairs, ItemStack::isSameItemSameComponents);
	}

	public SingleColorDyeRecipeSpec {
		sourceStacks = List.copyOf(sourceStacks);
		variantPairs = List.copyOf(variantPairs);
	}

	public GroupedCraftingRecipe recipe() {
		return new GroupedCraftingRecipe(id, 1, 2, List.of(sourceStacks), variantPairs.stream()
				.map(pair -> new GroupedCraftingVariant(List.of(pair.dye()), pair.result()))
				.toList(), resultMatcher);
	}

	public RecipeHolder<GroupedCraftingRecipe> recipeHolder() {
		return new RecipeHolder<>(ClientRecipeHelper.recipeKey(id), recipe());
	}

	@Override
	public List<RecipeHolder<GroupedCraftingRecipe>> getAllDisplays() {
		return List.of(recipeHolder());
	}

	@Override
	public List<RecipeHolder<GroupedCraftingRecipe>> getRecipesFor(ItemStack focusedOutput) {
		return recipe().narrowForResult(focusedOutput)
				.map(recipe -> List.of(new RecipeHolder<>(ClientRecipeHelper.recipeKey(id), recipe)))
				.orElse(List.of());
	}

	@Override
	public List<RecipeHolder<GroupedCraftingRecipe>> getUsagesFor(ItemStack focusedInput) {
		return recipe().hasFixedInput(focusedInput) ? getAllDisplays() : List.of();
	}
}
