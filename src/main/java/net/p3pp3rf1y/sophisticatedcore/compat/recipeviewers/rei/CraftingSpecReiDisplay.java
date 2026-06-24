package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import net.minecraft.world.item.crafting.Recipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayVariant;

import java.util.List;
import java.util.Optional;

public class CraftingSpecReiDisplay extends DefaultCraftingDisplay<Recipe<?>> {
	private final CraftingDisplaySpec spec;

	public CraftingSpecReiDisplay(CraftingDisplaySpec spec, List<CraftingDisplayVariant> variants) {
		super(getInputs(spec, variants), List.of(EntryIngredients.ofItemStacks(spec.getOutputStacks(variants))),
				Optional.of((Recipe<?>) spec.recipe(variants.get(0))));
		this.spec = spec;
	}

	@Override
	public int getWidth() {
		return spec.shapeless() ? getInputEntries().size() > 4 ? 3 : 2 : spec.width();
	}

	@Override
	public int getHeight() {
		return spec.shapeless() ? getInputEntries().size() > 4 ? 3 : 2 : spec.height();
	}

	@Override
	public int getInputWidth(int craftingWidth, int craftingHeight) {
		return spec.shapeless()
				? craftingWidth * craftingHeight <= getInputEntries().size() ? craftingWidth : Math.min(getInputEntries().size(), 3)
				: spec.width();
	}

	@Override
	public int getInputHeight(int craftingWidth, int craftingHeight) {
		return spec.shapeless() ? (int) Math.ceil(getInputEntries().size() / (double) getInputWidth(craftingWidth, craftingHeight)) : spec.height();
	}

	@Override
	public boolean isShapeless() {
		return spec.shapeless();
	}

	private static List<EntryIngredient> getInputs(CraftingDisplaySpec spec, List<CraftingDisplayVariant> variants) {
		return spec.getInputSlots(variants).stream().map(EntryIngredients::ofItemStacks).toList();
	}
}
