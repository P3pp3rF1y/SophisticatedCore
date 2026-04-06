package net.p3pp3rf1y.sophisticatedcore.crafting;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ShapeBasedRecipeBuilder implements RecipeBuilder {
	private final HolderGetter<Item> items;
	private final RecipeCategory category;
	private final Item result;
	private final ItemStackTemplate resultTemplate;
	private final List<String> rows = Lists.newArrayList();
	private final Map<Character, Ingredient> key = Maps.newLinkedHashMap();
	private final RecipeUnlockAdvancementBuilder advancementBuilder = new RecipeUnlockAdvancementBuilder();
	private final Function<ShapedRecipe, ? extends CraftingRecipe> factory;
	@Nullable
	private String group = null;
	private boolean showNotification = true;

	private ShapeBasedRecipeBuilder(HolderGetter<Item> items, ItemStack result, Function<ShapedRecipe, ? extends CraftingRecipe> factory) {
		this.items = items;
		this.category = RecipeCategory.MISC;
		this.result = result.getItem();
		this.resultTemplate = ItemStackTemplate.fromNonEmptyStack(result);
		this.factory = factory;
	}

	private ShapeBasedRecipeBuilder(HolderGetter<Item> items, ItemLike result, int count, Function<ShapedRecipe, ? extends CraftingRecipe> factory) {
		this.items = items;
		this.category = RecipeCategory.MISC;
		this.result = result.asItem();
		this.resultTemplate = new ItemStackTemplate(this.result, count);
		this.factory = factory;
	}

	private ShapeBasedRecipeBuilder(HolderGetter<Item> items, ItemLike result, ItemStackTemplate resultTemplate, Function<ShapedRecipe, ? extends CraftingRecipe> factory) {
		this.items = items;
		this.category = RecipeCategory.MISC;
		this.result = result.asItem();
		this.resultTemplate = resultTemplate;
		this.factory = factory;
	}

	public static ShapeBasedRecipeBuilder shaped(HolderGetter<Item> items, ItemStack result) {
		return new ShapeBasedRecipeBuilder(items, result, r -> r);
	}

	public static ShapeBasedRecipeBuilder shaped(HolderGetter<Item> items, ItemLike result) {
		return new ShapeBasedRecipeBuilder(items, result, 1, r -> r);
	}

	public static ShapeBasedRecipeBuilder shaped(HolderGetter<Item> items, ItemLike result, Function<ShapedRecipe, ? extends CraftingRecipe> factory) {
		return new ShapeBasedRecipeBuilder(items, result, 1, factory);
	}

	public static ShapeBasedRecipeBuilder shaped(HolderGetter<Item> items, ItemLike result, int count) {
		return new ShapeBasedRecipeBuilder(items, result, count, r -> r);
	}

	public static ShapeBasedRecipeBuilder shaped(HolderGetter<Item> items, ItemLike result, ItemStackTemplate resultTemplate) {
		return new ShapeBasedRecipeBuilder(items, result, resultTemplate, r -> r);
	}

	public static ShapeBasedRecipeBuilder shaped(HolderGetter<Item> items, ItemLike result, ItemStackTemplate resultTemplate, Function<ShapedRecipe, ? extends CraftingRecipe> factory) {
		return new ShapeBasedRecipeBuilder(items, result, resultTemplate, factory);
	}

	public static ShapeBasedRecipeBuilder shaped(HolderGetter<Item> items, ItemStack result, Function<ShapedRecipe, ? extends CraftingRecipe> factory) {
		return new ShapeBasedRecipeBuilder(items, result, factory);
	}

	public ShapeBasedRecipeBuilder define(Character symbol, TagKey<Item> tag) {
		return define(symbol, Ingredient.of(items.getOrThrow(tag)));
	}

	public ShapeBasedRecipeBuilder define(Character symbol, ItemLike item) {
		return define(symbol, Ingredient.of(item));
	}

	public ShapeBasedRecipeBuilder define(Character symbol, Ingredient ingredient) {
		if (key.containsKey(symbol)) {
			throw new IllegalArgumentException("Symbol '" + symbol + "' is already defined!");
		} else if (symbol == ' ') {
			throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
		}
		key.put(symbol, ingredient);
		return this;
	}

	public ShapeBasedRecipeBuilder pattern(String row) {
		if (!rows.isEmpty() && row.length() != rows.getFirst().length()) {
			throw new IllegalArgumentException("Pattern must be the same width on every line!");
		}
		rows.add(row);
		return this;
	}

	@Override
	public ShapeBasedRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
		advancementBuilder.unlockedBy(name, criterion);
		return this;
	}

	@Override
	public ShapeBasedRecipeBuilder group(@Nullable String group) {
		this.group = group;
		return this;
	}

	public ShapeBasedRecipeBuilder showNotification(boolean showNotification) {
		this.showNotification = showNotification;
		return this;
	}

	@Override
	public ResourceKey<Recipe<?>> defaultId() {
		return RecipeBuilder.getDefaultRecipeId(resultTemplate);
	}

	@Override
	public void save(RecipeOutput recipeOutput, ResourceKey<Recipe<?>> id) {
		ShapedRecipe compose = new ShapedRecipe(
				RecipeBuilder.createCraftingCommonInfo(showNotification),
				RecipeBuilder.createCraftingBookInfo(category, group),
				ShapedRecipePattern.of(key, rows),
				resultTemplate
		);
		HoldingRecipeOutput holdingRecipeOutput = new HoldingRecipeOutput(recipeOutput.advancement());
		holdingRecipeOutput.accept(id, compose, advancementBuilder.build(recipeOutput, id, category));
		recipeOutput.withConditions(new ItemEnabledCondition(result)).accept(id, factory.apply(compose), holdingRecipeOutput.getAdvancementHolder(), holdingRecipeOutput.getConditions());
	}
}
