package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Optional;

public interface IRecipeDisplayGenerator<C> {
	ShapedRecipeDisplayBuilder<C> shaped(ItemStack result);

	ShapelessRecipeDisplayBuilder<C> shapeless(ItemStack result);

	IRecipeDisplayBuilder smithing(Optional<Ingredient> template, Optional<Ingredient> base, Optional<Ingredient> addition, ItemStack result);
}
