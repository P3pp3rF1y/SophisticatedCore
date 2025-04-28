package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes;

import java.util.List;

public class PropertyBasedSubtypeInterpreterWrapper extends PropertyBasedSubtypeInterpreter {
	private final PropertyBasedSubtypeInterpreter wrapped;

	public PropertyBasedSubtypeInterpreterWrapper(PropertyBasedSubtypeInterpreter wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	protected List<IPropertyDefinition<?>> getPropertyDefinitions() {
		return wrapped.getPropertyDefinitions();
	}
}
