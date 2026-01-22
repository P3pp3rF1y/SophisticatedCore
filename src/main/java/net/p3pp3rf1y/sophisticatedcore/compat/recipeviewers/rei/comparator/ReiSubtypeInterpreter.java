package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei.comparator;

import me.shedaniel.rei.api.common.entry.comparison.ComparisonContext;
import me.shedaniel.rei.api.common.entry.comparison.EntryComparator;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreterWrapper;
import org.jspecify.annotations.Nullable;

public class ReiSubtypeInterpreter extends PropertyBasedSubtypeInterpreterWrapper implements EntryComparator<ItemStack> {
	public static ReiSubtypeInterpreter of(PropertyBasedSubtypeInterpreter wrapped) {
		return new ReiSubtypeInterpreter(wrapped);
	}

	private ReiSubtypeInterpreter(PropertyBasedSubtypeInterpreter wrapped) {
		super(wrapped);
	}

	@Override
	public long hash(ComparisonContext context, ItemStack stack) {
		long hashCode = 1;
		for (IPropertyDefinition<?> definition : getPropertyDefinitions()) {
			@Nullable Object value = definition.getPropertyValue(stack);
			hashCode = 31 * hashCode + (value == null ? 0 : value.hashCode());
		}
		return hashCode;
	}
}
