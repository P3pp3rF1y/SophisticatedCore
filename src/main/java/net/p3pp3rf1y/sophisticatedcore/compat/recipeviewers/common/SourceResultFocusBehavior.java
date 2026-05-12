package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class SourceResultFocusBehavior implements IFocusBehavior<CraftingDisplayVariant> {
	private final int sourceInputIndex;
	private final BiFunction<CraftingDisplayVariant, ItemStack, Optional<CraftingDisplayVariant>> sourceFocusFactory;
	private final BiFunction<CraftingDisplayVariant, ItemStack, Optional<CraftingDisplayVariant>> resultFocusFactory;

	public SourceResultFocusBehavior(int sourceInputIndex, BiFunction<CraftingDisplayVariant, ItemStack, Optional<CraftingDisplayVariant>> sourceFocusFactory,
			BiFunction<CraftingDisplayVariant, ItemStack, Optional<CraftingDisplayVariant>> resultFocusFactory) {
		this.sourceInputIndex = sourceInputIndex;
		this.sourceFocusFactory = sourceFocusFactory;
		this.resultFocusFactory = resultFocusFactory;
	}

	@Override
	public List<CraftingDisplayVariant> allDisplays(List<CraftingDisplayVariant> variants) {
		return variants;
	}

	@Override
	public List<CraftingDisplayVariant> recipesFor(List<CraftingDisplayVariant> variants, ItemStack focusedOutput) {
		return variants.stream().flatMap(variant -> resultFocusFactory.apply(variant, focusedOutput).stream()).toList();
	}

	@Override
	public List<CraftingDisplayVariant> usagesFor(List<CraftingDisplayVariant> variants, ItemStack focusedInput) {
		return variants.stream().flatMap(variant -> sourceFocusFactory.apply(variant, focusedInput).stream()).toList();
	}

	public int sourceInputIndex() {
		return sourceInputIndex;
	}
}
