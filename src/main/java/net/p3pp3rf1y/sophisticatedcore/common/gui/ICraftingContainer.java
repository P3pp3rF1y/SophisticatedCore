package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public interface ICraftingContainer {
	List<Slot> getRecipeSlots();

	Container getCraftMatrix();

	boolean shouldReplenish();
}
