package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;

import java.util.Optional;

public interface IRecipeViewerDisplayContext {
	Optional<PropertyBasedSubtypeInterpreter> getSubtypeInterpreter(ItemStack stack);

	static IRecipeViewerDisplayContext empty() {
		return stack -> Optional.empty();
	}
}
