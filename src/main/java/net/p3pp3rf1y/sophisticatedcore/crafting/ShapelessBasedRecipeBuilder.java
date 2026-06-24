package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.core.HolderGetter;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;

import java.util.function.Function;

public class ShapelessBasedRecipeBuilder extends ShapelessRecipeBuilder {
	private final Function<ShapelessRecipe, ? extends CraftingRecipe> factory;

	public ShapelessBasedRecipeBuilder(HolderGetter<Item> items, ItemStack result, Function<ShapelessRecipe, ? extends CraftingRecipe> factory) {
		super(items, RecipeCategory.MISC, result);
		this.factory = factory;
	}

	public ShapelessBasedRecipeBuilder(HolderGetter<Item> items, ItemLike result, int count, Function<ShapelessRecipe, ? extends CraftingRecipe> factory) {
		this(items, new ItemStack(result, count), factory);
	}

	public static ShapelessBasedRecipeBuilder shapeless(HolderGetter<Item> items, ItemStack result,
			Function<ShapelessRecipe, ? extends CraftingRecipe> factory) {
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

	public static ShapelessBasedRecipeBuilder shapeless(HolderGetter<Item> items, ItemLike result,
			Function<ShapelessRecipe, ? extends CraftingRecipe> factory) {
		return shapeless(items, result, 1, factory);
	}

	public static ShapelessBasedRecipeBuilder shapeless(HolderGetter<Item> items, ItemLike result, int count,
			Function<ShapelessRecipe, ? extends CraftingRecipe> factory) {
		return new ShapelessBasedRecipeBuilder(items, result, count, factory);
	}

	@Override
	public void save(RecipeOutput recipeOutput, ResourceKey<Recipe<?>> id) {
		HoldingRecipeOutput holdingRecipeOutput = new HoldingRecipeOutput(recipeOutput.advancement());
		super.save(holdingRecipeOutput, id);

		if (!(holdingRecipeOutput.getRecipe() instanceof ShapelessRecipe compose)) {
			return;
		}

		recipeOutput.withConditions(new ItemEnabledCondition(getResult())).accept(id, factory.apply(compose), holdingRecipeOutput.getAdvancementHolder(),
				holdingRecipeOutput.getConditions());
	}
}
