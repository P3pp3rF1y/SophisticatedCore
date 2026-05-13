package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class SmithingSourceResultFocusBehavior implements IFocusBehavior<SmithingDisplayVariant> {
	private final BiFunction<SmithingDisplayVariant, ItemStack, Optional<SmithingDisplayVariant>> sourceFocusFactory;
	private final BiFunction<SmithingDisplayVariant, ItemStack, Optional<SmithingDisplayVariant>> resultFocusFactory;

	public SmithingSourceResultFocusBehavior(BiFunction<SmithingDisplayVariant, ItemStack, Optional<SmithingDisplayVariant>> sourceFocusFactory,
			BiFunction<SmithingDisplayVariant, ItemStack, Optional<SmithingDisplayVariant>> resultFocusFactory) {
		this.sourceFocusFactory = sourceFocusFactory;
		this.resultFocusFactory = resultFocusFactory;
	}

	@Override
	public List<SmithingDisplayVariant> allDisplays(List<SmithingDisplayVariant> variants) {
		return variants;
	}

	@Override
	public List<SmithingDisplayVariant> recipesFor(List<SmithingDisplayVariant> variants, ItemStack focusedOutput) {
		return variants.stream().flatMap(variant -> resultFocusFactory.apply(variant, focusedOutput).stream()).toList();
	}

	@Override
	public List<SmithingDisplayVariant> usagesFor(List<SmithingDisplayVariant> variants, ItemStack focusedInput) {
		return variants.stream().flatMap(variant -> sourceFocusFactory.apply(variant, focusedInput).stream()).toList();
	}
}
