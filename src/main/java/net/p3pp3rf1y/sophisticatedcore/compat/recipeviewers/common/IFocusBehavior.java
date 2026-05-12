package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IFocusBehavior<TVariant> {
	List<TVariant> allDisplays(List<TVariant> variants);

	List<TVariant> recipesFor(List<TVariant> variants, ItemStack focusedOutput);

	List<TVariant> usagesFor(List<TVariant> variants, ItemStack focusedInput);
}
