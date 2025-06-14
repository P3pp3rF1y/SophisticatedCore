package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipe;

public interface ICustomSmithingRecipe extends SmithingRecipe {
	ItemStack result();
}
