package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiPredicate;

public record SingleColorDyeRecipeSpec(ResourceLocation id, List<ItemStack> sourceStacks, List<DyeVariantPair> variantPairs,
		BiPredicate<ItemStack, ItemStack> resultMatcher) implements IRecipeViewerDisplaySpec<GroupedCraftingRecipe> {
	public SingleColorDyeRecipeSpec(ResourceLocation id, List<ItemStack> sourceStacks, List<DyeVariantPair> variantPairs) {
		this(id, sourceStacks, variantPairs, ItemStack::isSameItemSameTags);
	}

	public SingleColorDyeRecipeSpec {
		sourceStacks = List.copyOf(sourceStacks);
		variantPairs = List.copyOf(variantPairs);
	}

	public GroupedCraftingRecipe recipe() {
		return new GroupedCraftingRecipe(id, 1, 2, List.of(sourceStacks),
				variantPairs.stream().map(pair -> new GroupedCraftingVariant(List.of(pair.dye()), pair.result())).toList(), resultMatcher);
	}

	@Override
	public List<GroupedCraftingRecipe> getAllDisplays() {
		return List.of(recipe());
	}

	@Override
	public List<GroupedCraftingRecipe> getRecipesFor(ItemStack focusedOutput) {
		return recipe().narrowForResult(focusedOutput).map(List::of).orElse(List.of());
	}

	@Override
	public List<GroupedCraftingRecipe> getUsagesFor(ItemStack focusedInput) {
		return recipe().hasFixedInput(focusedInput) ? getAllDisplays() : List.of();
	}
}
