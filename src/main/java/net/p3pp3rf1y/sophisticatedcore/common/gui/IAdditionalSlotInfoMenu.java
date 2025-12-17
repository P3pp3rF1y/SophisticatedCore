package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.Set;

public interface IAdditionalSlotInfoMenu {
	void updateAdditionalSlotInfo(Set<Integer> inaccessibleSlots, Map<Integer, Integer> slotLimitOverrides, Set<Integer> infiniteSlots, Map<Integer, Holder<Item>> slotFilterItems);

	void updateEmptySlotIcons(Map<Identifier, Set<Integer>> emptySlotIcons);
}
