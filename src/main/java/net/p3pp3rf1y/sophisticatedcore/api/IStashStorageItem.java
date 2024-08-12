package net.p3pp3rf1y.sophisticatedcore.api;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public interface IStashStorageItem {
	Optional<TooltipComponent> getInventoryTooltip(ItemStack stack);
	StashResult getItemStashable(HolderLookup.Provider registries, ItemStack storageStack, ItemStack stack);

	enum StashResult {
		MATCH_AND_SPACE,
		SPACE,
		NO_SPACE
	}
}
