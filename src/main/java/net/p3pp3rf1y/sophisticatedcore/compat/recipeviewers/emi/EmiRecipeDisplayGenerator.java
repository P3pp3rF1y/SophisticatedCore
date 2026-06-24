package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.recipe.EmiSmithingRecipe;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeDisplayBuilder;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeDisplayGenerator;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ShapedRecipeDisplayBuilder;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ShapelessRecipeDisplayBuilder;

import java.util.Optional;

public class EmiRecipeDisplayGenerator implements IRecipeDisplayGenerator<EmiCraftingRecipe> {
	private final EmiRegistry registry;

	public EmiRecipeDisplayGenerator(EmiRegistry registry) {
		this.registry = registry;
	}

	@Override
	public ShapedRecipeDisplayBuilder<EmiCraftingRecipe> shaped(ItemStack result) {
		return new EmiShapedRecipeDisplayBuilder(registry, result);
	}

	@Override
	public ShapelessRecipeDisplayBuilder<EmiCraftingRecipe> shapeless(ItemStack result) {
		return new EmiShapelessRecipeDisplayBuilder(registry, result);
	}

	@Override
	public IRecipeDisplayBuilder smithing(Optional<Ingredient> template, Ingredient base, Optional<Ingredient> addition, ItemStack result) {
		return id -> registry.addRecipe(new EmiSmithingRecipe(EmiIngredient.of(template.orElse(Ingredient.of(HolderSet.empty()))), EmiIngredient.of(base),
				EmiIngredient.of(addition.orElse(Ingredient.of(HolderSet.empty()))), EmiStack.of(result), id.location()));
	}
}
