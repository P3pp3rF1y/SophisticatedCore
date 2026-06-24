package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingRecipe;

import java.util.List;
import java.util.Optional;

public class GroupedCraftingReiDisplay extends DefaultCraftingDisplay<Recipe<?>> {
	@SuppressWarnings("unchecked")
	public GroupedCraftingReiDisplay(RecipeHolder<GroupedCraftingRecipe> recipeHolder) {
		super(recipeHolder.value().getInputSlots().stream().map(EntryIngredients::ofItemStacks).toList(),
				List.of(EntryIngredients.ofItemStacks(recipeHolder.value().getResultStacks())),
				Optional.of((RecipeHolder<Recipe<?>>) (RecipeHolder<?>) recipeHolder));
	}

	@Override
	public int getWidth() {
		return getOptionalRecipe().map(holder -> ((GroupedCraftingRecipe) holder.value()).getDisplayWidth()).orElse(1);
	}

	@Override
	public int getHeight() {
		return getOptionalRecipe().map(holder -> ((GroupedCraftingRecipe) holder.value()).getDisplayHeight()).orElse(2);
	}

	@Override
	public int getInputWidth(int craftingWidth, int craftingHeight) {
		return getWidth();
	}

	@Override
	public int getInputHeight(int craftingWidth, int craftingHeight) {
		return getHeight();
	}
}
