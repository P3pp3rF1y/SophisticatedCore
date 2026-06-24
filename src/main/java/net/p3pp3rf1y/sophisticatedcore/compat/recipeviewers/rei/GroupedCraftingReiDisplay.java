package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import net.minecraft.world.item.crafting.Recipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingRecipe;

import java.util.List;
import java.util.Optional;

public class GroupedCraftingReiDisplay extends DefaultCraftingDisplay<Recipe<?>> {
	public GroupedCraftingReiDisplay(GroupedCraftingRecipe recipe) {
		super(recipe.getInputSlots().stream().map(EntryIngredients::ofItemStacks).toList(), List.of(EntryIngredients.ofItemStacks(recipe.getResultStacks())),
				Optional.of((Recipe<?>) recipe));
	}

	@Override
	public int getWidth() {
		return getOptionalRecipe().map(recipe -> ((GroupedCraftingRecipe) recipe).getDisplayWidth()).orElse(1);
	}

	@Override
	public int getHeight() {
		return getOptionalRecipe().map(recipe -> ((GroupedCraftingRecipe) recipe).getDisplayHeight()).orElse(2);
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
