package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi.comparison;

import dev.emi.emi.api.stack.Comparison;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;

public interface EmiSubtypeInterpreter {
	static Comparison of(PropertyBasedSubtypeInterpreter wrapped) {
		return Comparison.compareData(stack -> wrapped.getComparableData(stack.getItemStack()));
	}
}
