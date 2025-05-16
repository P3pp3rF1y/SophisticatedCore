package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public abstract class PropertyBasedSubtypeInterpreter {
	private final List<IPropertyDefinition<?>> propertyDefinitions = new ArrayList<>();

	protected List<IPropertyDefinition<?>> getPropertyDefinitions() {
		return propertyDefinitions;
	}

	protected <T> void addOptionalProperty(Function<ItemStack, Optional<T>> propertyGetter, String propertyName,
										   Function<T, String> propertyValueSerializer) {
		addDefinition(s -> propertyGetter.andThen(i -> i.orElse(null)).apply(s), propertyName, propertyValueSerializer);
	}

	private <T> void addDefinition(IPropertyValueGetter<T> getter, String propertyName, Function<T, String> propertyValueSerializer) {
		getPropertyDefinitions().add(new IPropertyDefinition<T>() {
			@Override
			public @Nullable T getPropertyValue(ItemStack itemStack) {
				return getter.getPropertyValue(itemStack);
			}

			@Override
			public String getPropertyName() {
				return propertyName;
			}

			@Override
			public String serializePropertyValue(@Nullable T property) {
				return property != null ? propertyValueSerializer.apply(property) : "";
			}
		});
	}

	protected <T> void addProperty(IPropertyValueGetter<T> propertyGetter, String propertyName, Function<T, String> propertyValueSerializer) {
		addDefinition(propertyGetter, propertyName, propertyValueSerializer);
	}

	public final @Nullable Object getComparableData(ItemStack stack) {
		boolean allNulls = true;
		List<Object> results = new ArrayList<>(getPropertyDefinitions().size());
		for (IPropertyDefinition<?> definition : getPropertyDefinitions()) {
			@Nullable Object value = definition.getPropertyValue(stack);
			if (value != null) {
				allNulls = false;
			}
			results.add(value);
		}
		if (allNulls) {
			return null;
		}
		return results;
	}

	private <T> String getSerializedPropertyValue(IPropertyDefinition<T> definition, Object value) {
		//noinspection unchecked
		return definition.serializePropertyValue((T) value);
	}

	public String getRegistrySanitizedItemString(ItemStack stack) {
		StringBuilder result = new StringBuilder();
		for (IPropertyDefinition<?> definition : getPropertyDefinitions()) {
			@Nullable Object value = definition.getPropertyValue(stack);
			if (value != null) {
				String serializedValue = sanitize(getSerializedPropertyValue(definition, value));
				if (!result.isEmpty()) {
					result.append('_');
				}
				result.append(definition.getPropertyName().toLowerCase(Locale.ROOT)).append('_').append(serializedValue);
			}
		}
		return getItemPath(stack) + "_" + result;
	}

	private String sanitize(String value) {
		return value.replaceAll(":", "_").toLowerCase(Locale.ROOT);
	}

	private static String getItemPath(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
	}

	public interface IPropertyValueGetter<T> {
		@Nullable
		T getPropertyValue(ItemStack itemStack);
	}

	public interface IPropertyDefinition<T> {
		@Nullable
		T getPropertyValue(ItemStack itemStack);

		String getPropertyName();

		String serializePropertyValue(@Nullable T property);
	}
}
