package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.common.displays.DefaultSmithingDisplay;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SmithingDisplaySpec;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SmithingDisplayVariant;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SmithingSpecReiDisplayGenerator implements DynamicDisplayGenerator<DefaultSmithingDisplay> {
	private final Supplier<IRecipeViewerDisplayCatalog> catalogSupplier;
	private final Predicate<ItemStack> focusedStackPredicate;

	public SmithingSpecReiDisplayGenerator(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedStackPredicate) {
		this.catalogSupplier = catalogSupplier;
		this.focusedStackPredicate = focusedStackPredicate;
	}

	@Override
	public Optional<List<DefaultSmithingDisplay>> getRecipeFor(EntryStack<?> entry) {
		if (!(entry.getValue() instanceof ItemStack stack) || !focusedStackPredicate.test(stack)) {
			return Optional.empty();
		}

		List<DefaultSmithingDisplay> displays = new ArrayList<>();
		catalogSupplier.get().getSmithingRecipesFor(stack).forEach(view -> displays.add(toDisplay(view.spec(), view.variants())));
		return displays.isEmpty() ? Optional.empty() : Optional.of(displays);
	}

	@Override
	public Optional<List<DefaultSmithingDisplay>> getUsageFor(EntryStack<?> entry) {
		if (!(entry.getValue() instanceof ItemStack stack) || !focusedStackPredicate.test(stack)) {
			return Optional.empty();
		}

		List<DefaultSmithingDisplay> displays = new ArrayList<>();
		catalogSupplier.get().getSmithingUsagesFor(stack).forEach(view -> displays.add(toDisplay(view.spec(), view.variants())));
		return displays.isEmpty() ? Optional.empty() : Optional.of(displays);
	}

	private static DefaultSmithingDisplay toDisplay(SmithingDisplaySpec spec, List<SmithingDisplayVariant> variants) {
		List<EntryIngredient> inputs = List.of(
				EntryIngredients.ofIngredient(spec.template().orElseThrow()),
				EntryIngredients.ofItemStacks(spec.getBaseStacks(variants)),
				EntryIngredients.ofIngredient(spec.addition().orElseThrow())
		);
		return new DefaultSmithingDisplay(inputs, List.of(EntryIngredients.ofItemStacks(spec.getResultStacks(variants))), Optional.of(spec.id()));
	}
}
