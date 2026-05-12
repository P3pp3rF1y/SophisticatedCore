package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CraftingDisplayView;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CraftingSpecReiDisplayGenerator implements DynamicDisplayGenerator<CraftingSpecReiDisplay> {
	private final Supplier<IRecipeViewerDisplayCatalog> catalogSupplier;
	private final Predicate<ItemStack> focusedStackPredicate;

	public CraftingSpecReiDisplayGenerator(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> focusedStackPredicate) {
		this.catalogSupplier = catalogSupplier;
		this.focusedStackPredicate = focusedStackPredicate;
	}

	@Override
	public Optional<List<CraftingSpecReiDisplay>> getRecipeFor(EntryStack<?> entry) {
		if (!(entry.getValue() instanceof ItemStack stack) || !focusedStackPredicate.test(stack)) {
			return Optional.empty();
		}

		List<CraftingSpecReiDisplay> displays = catalogSupplier.get().getCraftingRecipesFor(stack).stream()
				.map(CraftingSpecReiDisplayGenerator::toDisplay)
				.toList();
		return displays.isEmpty() ? Optional.empty() : Optional.of(displays);
	}

	@Override
	public Optional<List<CraftingSpecReiDisplay>> getUsageFor(EntryStack<?> entry) {
		if (!(entry.getValue() instanceof ItemStack stack) || !focusedStackPredicate.test(stack)) {
			return Optional.empty();
		}

		List<CraftingSpecReiDisplay> displays = catalogSupplier.get().getCraftingUsagesFor(stack).stream()
				.map(CraftingSpecReiDisplayGenerator::toDisplay)
				.toList();
		return displays.isEmpty() ? Optional.empty() : Optional.of(displays);
	}

	private static CraftingSpecReiDisplay toDisplay(CraftingDisplayView view) {
		return new CraftingSpecReiDisplay(view.spec(), view.variants());
	}
}
