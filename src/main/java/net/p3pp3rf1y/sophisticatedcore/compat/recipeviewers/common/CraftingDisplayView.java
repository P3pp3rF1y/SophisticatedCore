package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import java.util.List;

public record CraftingDisplayView(CraftingDisplaySpec spec, List<CraftingDisplayVariant> variants) {
	public CraftingDisplayView {
		variants = List.copyOf(variants);
	}
}
