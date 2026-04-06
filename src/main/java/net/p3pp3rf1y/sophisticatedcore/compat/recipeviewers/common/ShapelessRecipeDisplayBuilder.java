package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import java.util.List;

public abstract class ShapelessRecipeDisplayBuilder<R> implements IRecipeDisplayBuilder {
	public abstract ShapelessRecipeDisplayBuilder<R> requires(TagKey<Item> tag);

	public abstract ShapelessRecipeDisplayBuilder<R> requires(ItemLike item);

	public abstract ShapelessRecipeDisplayBuilder<R> requires(ItemStack itemStack);

	protected abstract ShapelessRecipeDisplayBuilder<R> requiresItemStacks(List<ItemStack> itemStacks);

	public abstract ShapelessRecipeDisplayBuilder<R> requires(HolderSet<Item> items);

	public ShapelessRecipeDisplayBuilder<R> requires(Ingredient ingredient) {
		if (ingredient.getCustomIngredient() != null) {
			List<ItemStack> displayStacks = ingredient.display().resolveForStacks(RecipeHelper.getContextMap());
			if (!displayStacks.isEmpty()) {
				requiresItemStacks(displayStacks);
				return this;
			}
			requires(HolderSet.direct(ingredient.getCustomIngredient().items().toList()));
		} else {
			requires(ingredient.getValues());
		}

		return this;
	}

	public ShapelessRecipeDisplayBuilder<R> requires(List<Ingredient> ingredients) {
		ingredients.forEach(this::requires);
		return this;
	}
}
