package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.GroupedCraftingRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplaySpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class GroupedCraftingReiDisplayGenerator implements DynamicDisplayGenerator<GroupedCraftingReiDisplay> {
	private final Supplier<IRecipeViewerDisplayCatalog> catalogSupplier;
	private final Predicate<ItemStack> stackPredicate;

	public GroupedCraftingReiDisplayGenerator(Supplier<IRecipeViewerDisplayCatalog> catalogSupplier, Predicate<ItemStack> stackPredicate) {
		this.catalogSupplier = catalogSupplier;
		this.stackPredicate = stackPredicate;
	}

	@Override
	public Optional<List<GroupedCraftingReiDisplay>> getRecipeFor(EntryStack<?> entry) {
		if (!(entry.getValue() instanceof ItemStack stack) || !stackPredicate.test(stack)) {
			return Optional.empty();
		}

		List<GroupedCraftingReiDisplay> displays = new ArrayList<>();
		for (IRecipeViewerDisplaySpec<GroupedCraftingRecipe> spec : catalogSupplier.get().getGroupedCraftingSpecs()) {
			spec.getRecipesFor(stack).forEach(recipe -> displays.add(new GroupedCraftingReiDisplay(recipe)));
		}
		return displays.isEmpty() ? Optional.empty() : Optional.of(displays);
	}

	@Override
	public Optional<List<GroupedCraftingReiDisplay>> getUsageFor(EntryStack<?> entry) {
		if (!(entry.getValue() instanceof ItemStack stack) || !stackPredicate.test(stack)) {
			return Optional.empty();
		}

		List<GroupedCraftingReiDisplay> displays = catalogSupplier.get().getGroupedCraftingSpecs().stream()
				.flatMap(spec -> spec.getUsagesFor(stack).stream())
				.map(GroupedCraftingReiDisplay::new)
				.toList();
		return displays.isEmpty() ? Optional.empty() : Optional.of(displays);
	}

	@Override
	public Optional<List<GroupedCraftingReiDisplay>> generate(ViewSearchBuilder builder) {
		if (!builder.getRecipesFor().isEmpty() || !builder.getUsagesFor().isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(catalogSupplier.get().getGroupedCraftingSpecs().stream().flatMap(spec -> spec.getAllDisplays().stream()).map(GroupedCraftingReiDisplay::new).toList());
	}
}
