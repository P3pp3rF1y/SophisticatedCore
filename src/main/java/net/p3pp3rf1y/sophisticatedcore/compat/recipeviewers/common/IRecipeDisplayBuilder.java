package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

public interface IRecipeDisplayBuilder {
	void save(ResourceKey<Recipe<?>> id);
}
