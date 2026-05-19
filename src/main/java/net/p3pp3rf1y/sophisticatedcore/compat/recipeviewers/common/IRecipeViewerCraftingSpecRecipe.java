package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import java.util.List;

public interface IRecipeViewerCraftingSpecRecipe {
	CraftingDisplaySpec spec();

	List<CraftingDisplayVariant> variants();
}
