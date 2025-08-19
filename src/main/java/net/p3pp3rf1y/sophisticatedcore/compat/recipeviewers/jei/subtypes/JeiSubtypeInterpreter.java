package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.subtypes;

import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreterWrapper;

import javax.annotation.Nullable;

public class JeiSubtypeInterpreter extends PropertyBasedSubtypeInterpreterWrapper implements IIngredientSubtypeInterpreter<ItemStack> {
	public static JeiSubtypeInterpreter of(PropertyBasedSubtypeInterpreter wrapped) {
		return new JeiSubtypeInterpreter(wrapped);
	}

	private JeiSubtypeInterpreter(PropertyBasedSubtypeInterpreter wrapped) {
		super(wrapped);
	}

	@Override
	public String apply(ItemStack itemStack, UidContext context) {
		StringBuilder result = new StringBuilder();
		for (IPropertyDefinition<?> definition : getPropertyDefinitions()) {
			@Nullable Object value = definition.getPropertyValue(itemStack);
			if (value != null) {
				String serializedValue = getSerializedPropertyValue(definition, value);
				if (!result.isEmpty()) {
					result.append(',');
				}
				result.append(definition.getPropertyName()).append(':').append(serializedValue);
			}
		}
		return "{" + result + "}";
	}

	private <T> String getSerializedPropertyValue(IPropertyDefinition<T> definition, Object value) {
		//noinspection unchecked
		return definition.serializePropertyValue((T) value);
	}
}
