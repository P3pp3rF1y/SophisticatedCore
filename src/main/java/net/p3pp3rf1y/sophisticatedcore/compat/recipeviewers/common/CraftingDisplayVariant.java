package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record CraftingDisplayVariant(List<ItemStack> inputs, List<ItemStack> outputs) {
	public CraftingDisplayVariant {
		inputs = List.copyOf(inputs);
		outputs = List.copyOf(outputs);
	}

	public ItemStack firstOutput() {
		return outputs.get(0);
	}
}
