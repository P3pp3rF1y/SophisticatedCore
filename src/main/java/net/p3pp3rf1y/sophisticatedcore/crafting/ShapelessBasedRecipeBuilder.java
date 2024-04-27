package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;

import java.util.function.Function;

public class ShapelessBasedRecipeBuilder extends ShapelessRecipeBuilder {
	private final Function<ShapelessRecipe, ? extends ShapelessRecipe> factory;

	public ShapelessBasedRecipeBuilder(ItemStack result, Function<ShapelessRecipe, ? extends ShapelessRecipe> factory) {
		super(RecipeCategory.MISC, result);
		this.factory = factory;
	}

	public ShapelessBasedRecipeBuilder(ItemLike result, int count, Function<ShapelessRecipe, ? extends ShapelessRecipe> factory) {
		this(new ItemStack(result, count), factory);
	}

	public static ShapelessBasedRecipeBuilder shapeless(ItemStack result) {
		return new ShapelessBasedRecipeBuilder(result, r -> r);
	}

	public static ShapelessBasedRecipeBuilder shapeless(ItemLike result) {
		return shapeless(result, 1);
	}

	public static ShapelessBasedRecipeBuilder shapeless(ItemLike result, int count) {
		return shapeless(new ItemStack(result, count));
	}

	public static ShapelessBasedRecipeBuilder shapeless(ItemLike result, Function<ShapelessRecipe, ? extends ShapelessRecipe> factory) {
		return shapeless(result, 1, factory);
	}

	public static ShapelessBasedRecipeBuilder shapeless(ItemLike result, int count, Function<ShapelessRecipe, ? extends ShapelessRecipe> factory) {
		return new ShapelessBasedRecipeBuilder(result, count, factory);
	}

	@Override
	public void save(RecipeOutput recipeOutput, ResourceLocation id) {
		HoldingRecipeOutput holdingRecipeOutput = new HoldingRecipeOutput(recipeOutput.advancement());
		super.save(holdingRecipeOutput, id);

		if (!(holdingRecipeOutput.getRecipe() instanceof ShapelessRecipe compose)) {
			return;
		}

		recipeOutput.withConditions(new ItemEnabledCondition(getResult())).accept(id, factory.apply(compose), holdingRecipeOutput.getAdvancementHolder(), holdingRecipeOutput.getConditions());
	}
}