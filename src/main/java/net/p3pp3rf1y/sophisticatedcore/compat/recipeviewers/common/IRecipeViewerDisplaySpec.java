package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IRecipeViewerDisplaySpec<TVariant> {
	ResourceLocation id();

	List<TVariant> getAllDisplays();

	default List<TVariant> getGlobalDisplays() {
		return getAllDisplays();
	}

	List<TVariant> getRecipesFor(ItemStack focusedOutput);

	List<TVariant> getUsagesFor(ItemStack focusedInput);
}
