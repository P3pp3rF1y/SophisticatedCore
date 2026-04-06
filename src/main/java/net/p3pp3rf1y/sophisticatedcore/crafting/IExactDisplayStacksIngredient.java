package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;

import java.util.List;

public interface IExactDisplayStacksIngredient extends ICustomIngredient {
	List<ItemStack> getExactDisplayStacks();
}
