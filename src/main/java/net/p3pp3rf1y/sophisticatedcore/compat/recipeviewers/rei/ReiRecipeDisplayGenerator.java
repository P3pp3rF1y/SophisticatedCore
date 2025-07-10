package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.common.SmithingDisplay;
import me.shedaniel.rei.plugin.common.displays.DefaultSmithingDisplay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeDisplayBuilder;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeDisplayGenerator;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ShapedRecipeDisplayBuilder;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.ShapelessRecipeDisplayBuilder;

import java.util.List;
import java.util.Optional;

public class ReiRecipeDisplayGenerator implements IRecipeDisplayGenerator<Display> {
	private final DisplayRegistry registry;

	public ReiRecipeDisplayGenerator(DisplayRegistry registry) {
		this.registry = registry;
	}

	@Override
	public ShapedRecipeDisplayBuilder<Display> shaped(ItemStack result) {
		return new ReiShapedRecipeDisplayBuilder(registry, result);
	}

	@Override
	public ShapelessRecipeDisplayBuilder<Display> shapeless(ItemStack result) {
		return new ReiShapelessRecipeDisplayBuilder(registry, result);
	}

	@Override
	public IRecipeDisplayBuilder smithing(Optional<Ingredient> template, Ingredient base, Optional<Ingredient> addition, ItemStack result) {
		return id -> {
			registry.add(new DefaultSmithingDisplay(
					List.of(
							template.map(EntryIngredients::ofIngredient).orElse(EntryIngredient.empty()),
							EntryIngredients.ofIngredient(base),
							addition.map(EntryIngredients::ofIngredient).orElse(EntryIngredient.empty())
					),
					List.of(EntryIngredients.of(result)),
					Optional.of(SmithingDisplay.SmithingRecipeType.TRANSFORM),
					Optional.of(id.location())
			));
		};
	}
}
