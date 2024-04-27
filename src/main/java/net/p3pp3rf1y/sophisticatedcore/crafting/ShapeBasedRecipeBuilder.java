package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.ItemLike;

import java.util.function.Function;

public class ShapeBasedRecipeBuilder extends ShapedRecipeBuilder {

	private final Function<ShapedRecipe, ? extends ShapedRecipe> factory;

	private ShapeBasedRecipeBuilder(ItemStack result, Function<ShapedRecipe, ? extends ShapedRecipe> factory) {
		super(RecipeCategory.MISC, result);
		this.factory = factory;
	}

	public static ShapeBasedRecipeBuilder shaped(ItemStack result) {
		return new ShapeBasedRecipeBuilder(result, r -> r);
	}

	public static ShapeBasedRecipeBuilder shaped(ItemLike result) {
		return shaped(new ItemStack(result));
	}

	public static ShapeBasedRecipeBuilder shaped(ItemLike result, Function<ShapedRecipe, ? extends ShapedRecipe> factory) {
		return new ShapeBasedRecipeBuilder(new ItemStack(result, 1), factory);
	}

	@Override
	public void save(RecipeOutput recipeOutput) {
		save(recipeOutput, BuiltInRegistries.ITEM.getKey(getResult().asItem()));
	}

	@Override
	public void save(RecipeOutput recipeOutput, ResourceLocation id) {
		HoldingRecipeOutput holdingRecipeOutput = new HoldingRecipeOutput(recipeOutput.advancement());
		super.save(holdingRecipeOutput, id);

		if (!(holdingRecipeOutput.getRecipe() instanceof ShapedRecipe compose)) {
			return;
		}

		recipeOutput.withConditions(new ItemEnabledCondition(getResult())).accept(id, factory.apply(compose), holdingRecipeOutput.getAdvancementHolder(), holdingRecipeOutput.getConditions());
	}
}
