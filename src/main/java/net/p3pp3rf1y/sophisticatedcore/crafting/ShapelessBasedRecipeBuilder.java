package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ShapelessBasedRecipeBuilder implements RecipeBuilder {
	private final HolderGetter<Item> items;
	private final RecipeCategory category;
	private final Function<ShapelessRecipe, ? extends CraftingRecipe> factory;
	private final ItemStack result;
	private final ItemStackTemplate resultTemplate;
	private final List<Ingredient> ingredients = new ArrayList<>();
	private final RecipeUnlockAdvancementBuilder advancementBuilder = new RecipeUnlockAdvancementBuilder();
	@Nullable
	private String group = null;

	public ShapelessBasedRecipeBuilder(HolderGetter<Item> items, ItemStack result, Function<ShapelessRecipe, ? extends CraftingRecipe> factory) {
		this.items = items;
		this.category = RecipeCategory.MISC;
		this.result = result;
		this.resultTemplate = ItemStackTemplate.fromNonEmptyStack(result);
		this.factory = factory;
	}

	public ShapelessBasedRecipeBuilder(HolderGetter<Item> items, ItemLike result, int count, Function<ShapelessRecipe, ? extends CraftingRecipe> factory) {
		this(items, new ItemStack(result, count), factory);
	}

	public static ShapelessBasedRecipeBuilder shapeless(HolderGetter<Item> items, ItemStack result, Function<ShapelessRecipe, ? extends CraftingRecipe> factory) {
		return new ShapelessBasedRecipeBuilder(items, result, factory);
	}

	public static ShapelessBasedRecipeBuilder shapeless(HolderGetter<Item> items, ItemStack result) {
		return new ShapelessBasedRecipeBuilder(items, result, r -> r);
	}

	public static ShapelessBasedRecipeBuilder shapeless(HolderGetter<Item> items, ItemLike result) {
		return shapeless(items, result, 1);
	}

	public static ShapelessBasedRecipeBuilder shapeless(HolderGetter<Item> items, ItemLike result, int count) {
		return shapeless(items, new ItemStack(result, count));
	}

	public static ShapelessBasedRecipeBuilder shapeless(HolderGetter<Item> items, ItemLike result, Function<ShapelessRecipe, ? extends CraftingRecipe> factory) {
		return shapeless(items, result, 1, factory);
	}

	public static ShapelessBasedRecipeBuilder shapeless(HolderGetter<Item> items, ItemLike result, int count, Function<ShapelessRecipe, ? extends CraftingRecipe> factory) {
		return new ShapelessBasedRecipeBuilder(items, result, count, factory);
	}

	public ShapelessBasedRecipeBuilder requires(TagKey<Item> tag) {
		return requires(Ingredient.of(items.getOrThrow(tag)));
	}

	public ShapelessBasedRecipeBuilder requires(ItemLike item) {
		return requires(item, 1);
	}

	public ShapelessBasedRecipeBuilder requires(ItemLike item, int count) {
		for (int i = 0; i < count; i++) {
			requires(Ingredient.of(item));
		}
		return this;
	}

	public ShapelessBasedRecipeBuilder requires(Ingredient ingredient) {
		return requires(ingredient, 1);
	}

	public ShapelessBasedRecipeBuilder requires(Ingredient ingredient, int count) {
		for (int i = 0; i < count; i++) {
			ingredients.add(ingredient);
		}
		return this;
	}

	@Override
	public ShapelessBasedRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
		advancementBuilder.unlockedBy(name, criterion);
		return this;
	}

	@Override
	public ShapelessBasedRecipeBuilder group(@Nullable String group) {
		this.group = group;
		return this;
	}

	@Override
	public ResourceKey<Recipe<?>> defaultId() {
		return RecipeBuilder.getDefaultRecipeId(result);
	}

	@Override
	public void save(RecipeOutput recipeOutput, ResourceKey<Recipe<?>> id) {
		ShapelessRecipe compose = new ShapelessRecipe(
				RecipeBuilder.createCraftingCommonInfo(true),
				RecipeBuilder.createCraftingBookInfo(category, group),
				resultTemplate,
				ingredients
		);
		HoldingRecipeOutput holdingRecipeOutput = new HoldingRecipeOutput(recipeOutput.advancement());
		holdingRecipeOutput.accept(id, compose, advancementBuilder.build(recipeOutput, id, category));
		recipeOutput.withConditions(new ItemEnabledCondition(result.getItem())).accept(id, factory.apply(compose), holdingRecipeOutput.getAdvancementHolder(), holdingRecipeOutput.getConditions());
	}
}
