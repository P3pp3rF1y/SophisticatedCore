package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.Set;

public interface IAdditionalSlotInfoMenu {
	void updateAdditionalSlotInfo(Set<Integer> inaccessibleSlots, Map<Integer, Integer> slotLimitOverrides, Map<Integer, Item> slotFilterItems);

	void updateEmptySlotIcons(Map<ResourceLocation, Set<Integer>> emptySlotIcons);
}
