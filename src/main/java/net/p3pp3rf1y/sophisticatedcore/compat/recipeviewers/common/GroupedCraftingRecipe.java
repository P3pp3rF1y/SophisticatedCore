package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;

public class GroupedCraftingRecipe extends ShapedRecipe {
	private final ResourceLocation id;
	private final int width;
	private final int height;
	private final List<List<ItemStack>> fixedInputSlots;
	private final List<GroupedCraftingVariant> variants;
	private final BiPredicate<ItemStack, ItemStack> resultMatcher;

	public GroupedCraftingRecipe(ResourceLocation id, int width, int height, List<List<ItemStack>> fixedInputSlots, List<GroupedCraftingVariant> variants) {
		this(id, width, height, fixedInputSlots, variants, ItemStack::isSameItemSameComponents);
	}

	public GroupedCraftingRecipe(ResourceLocation id, int width, int height, List<List<ItemStack>> fixedInputSlots, List<GroupedCraftingVariant> variants,
			BiPredicate<ItemStack, ItemStack> resultMatcher) {
		super("", CraftingBookCategory.MISC, new ShapedRecipePattern(width, height, getIngredients(fixedInputSlots, variants), Optional.empty()),
				variants.getFirst().result());
		this.id = id;
		this.width = width;
		this.height = height;
		this.fixedInputSlots = fixedInputSlots.stream().map(List::copyOf).toList();
		this.variants = List.copyOf(variants);
		this.resultMatcher = resultMatcher;
	}

	public ResourceLocation getId() {
		return id;
	}

	public int getDisplayWidth() {
		return width;
	}

	public int getDisplayHeight() {
		return height;
	}

	public List<List<ItemStack>> getInputSlots() {
		List<List<ItemStack>> inputSlots = new ArrayList<>(fixedInputSlots);
		int groupedInputCount = variants.stream().mapToInt(variant -> variant.displayedInputs().size()).max().orElse(0);
		for (int groupedInputIndex = 0; groupedInputIndex < groupedInputCount; groupedInputIndex++) {
			int index = groupedInputIndex;
			inputSlots.add(variants.stream().filter(variant -> variant.displayedInputs().size() > index).map(variant -> variant.displayedInputs().get(index))
					.toList());
		}
		return inputSlots;
	}

	public List<List<ItemStack>> getFixedInputSlots() {
		return fixedInputSlots;
	}

	public List<ItemStack> getResultStacks() {
		return variants.stream().map(GroupedCraftingVariant::result).toList();
	}

	public List<GroupedCraftingVariant> getVariants() {
		return variants;
	}

	public Optional<GroupedCraftingVariant> findVariantForResult(ItemStack stack) {
		return variants.stream().filter(variant -> resultMatcher.test(variant.result(), stack)).findFirst();
	}

	public BiPredicate<ItemStack, ItemStack> getResultMatcher() {
		return resultMatcher;
	}

	public Optional<ItemStack> findResultForDisplayedInput(ItemStack stack) {
		return variants.stream().filter(variant -> variant.displayedInputs().stream().anyMatch(input -> ItemStack.isSameItemSameComponents(input, stack)))
				.map(GroupedCraftingVariant::result).findFirst();
	}

	public boolean hasFixedInput(ItemStack stack) {
		return fixedInputSlots.stream().flatMap(List::stream).anyMatch(input -> ItemStack.isSameItem(input, stack));
	}

	public Optional<GroupedCraftingRecipe> narrowForResult(ItemStack stack) {
		return findVariantForResult(stack).map(variant -> new GroupedCraftingVariant(variant.displayedInputs(), stack))
				.map(variant -> new GroupedCraftingRecipe(id, width, height, fixedInputSlots, List.of(variant), resultMatcher));
	}

	private static List<Optional<Ingredient>> getIngredients(List<List<ItemStack>> fixedInputSlots, List<GroupedCraftingVariant> variants) {
		List<Optional<Ingredient>> ingredients = new ArrayList<>();
		fixedInputSlots.stream().map(GroupedCraftingRecipe::ingredientFromStacks).map(Optional::of).forEach(ingredients::add);

		int groupedInputCount = variants.stream().mapToInt(variant -> variant.displayedInputs().size()).max().orElse(0);
		IntStream
				.range(0, groupedInputCount).mapToObj(index -> ingredientFromStacks(variants.stream()
						.filter(variant -> variant.displayedInputs().size() > index).map(variant -> variant.displayedInputs().get(index)).toList()))
				.map(Optional::of).forEach(ingredients::add);

		return ingredients;
	}

	private static Ingredient ingredientFromStacks(List<ItemStack> stacks) {
		return Ingredient.of(stacks.stream().map(ItemStack::getItem));
	}
}
