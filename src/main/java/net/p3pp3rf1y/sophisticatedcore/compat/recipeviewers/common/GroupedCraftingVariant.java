package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record GroupedCraftingVariant(List<ItemStack> displayedInputs, ItemStack result) {
	public GroupedCraftingVariant {
		displayedInputs = List.copyOf(displayedInputs);
	}
}
