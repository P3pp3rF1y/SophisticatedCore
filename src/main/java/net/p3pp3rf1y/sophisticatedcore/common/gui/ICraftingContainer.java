package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingRefillType;

import java.util.List;

public interface ICraftingContainer {
	List<Slot> getRecipeSlots();

	Container getCraftMatrix();

	CraftingRefillType shouldRefillCraftingGrid();
}
