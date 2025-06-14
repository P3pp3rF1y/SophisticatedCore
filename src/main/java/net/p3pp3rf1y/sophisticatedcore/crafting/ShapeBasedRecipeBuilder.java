package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.core.HolderGetter;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.ItemLike;

import java.util.function.Function;

public class ShapeBasedRecipeBuilder extends ShapedRecipeBuilder {

	private final Function<ShapedRecipe, ? extends ShapedRecipe> factory;

	private ShapeBasedRecipeBuilder(HolderGetter<Item> items, ItemStack result, Function<ShapedRecipe, ? extends ShapedRecipe> factory) {
		super(items, RecipeCategory.MISC, result);
		this.factory = factory;
	}

	public static ShapeBasedRecipeBuilder shaped(HolderGetter<Item> items, ItemStack result) {
		return new ShapeBasedRecipeBuilder(items, result, r -> r);
	}

	public static ShapeBasedRecipeBuilder shaped(HolderGetter<Item> items, ItemLike result) {
		return shaped(items, new ItemStack(result));
	}

	public static ShapeBasedRecipeBuilder shaped(HolderGetter<Item> items, ItemLike result, Function<ShapedRecipe, ? extends ShapedRecipe> factory) {
		return shaped(items, new ItemStack(result, 1), factory);
	}

	public static ShapeBasedRecipeBuilder shaped(HolderGetter<Item> items, ItemStack result, Function<ShapedRecipe, ? extends ShapedRecipe> factory) {
		return new ShapeBasedRecipeBuilder(items, result, factory);
	}

	@Override
	public void save(RecipeOutput recipeOutput, ResourceKey<Recipe<?>> id) {
		HoldingRecipeOutput holdingRecipeOutput = new HoldingRecipeOutput(recipeOutput.advancement());
		super.save(holdingRecipeOutput, id);

		if (!(holdingRecipeOutput.getRecipe() instanceof ShapedRecipe compose)) {
			return;
		}

		recipeOutput.withConditions(new ItemEnabledCondition(getResult())).accept(id, factory.apply(compose), holdingRecipeOutput.getAdvancementHolder(), holdingRecipeOutput.getConditions());
	}
}
