package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import java.util.List;

public record SmithingDisplayView(SmithingDisplaySpec spec, List<SmithingDisplayVariant> variants) {
	public SmithingDisplayView {
		variants = List.copyOf(variants);
	}
}
