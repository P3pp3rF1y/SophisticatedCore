package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingRefillType;

import java.util.List;

public interface ICraftingContainer {
	List<Slot> getRecipeSlots();

	Container getCraftMatrix();
	void setRecipeUsed(ResourceLocation recipeId);

	CraftingRefillType shouldRefillCraftingGrid();
}
