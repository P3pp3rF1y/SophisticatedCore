package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.util.ItemStackHelper;

public interface IOverflowResponseUpgrade {
	default boolean matchesFilter(Item item, int damageValue, boolean isEmpty, DataComponentMap components, Item filterItem, int filterDamageValue, boolean filterIsEmpty, DataComponentMap filterComponents) {
		if (item != filterItem) {
			return false;
		}

		if (getFilterLogic().getPrimaryMatch() == PrimaryMatch.TAGS) {
			return true;
		}

		if (getFilterLogic().shouldMatchDurability() && damageValue != filterDamageValue) {
			return false;
		}

		return !getFilterLogic().shouldMatchComponents() || ItemStackHelper.areItemStackComponentsEqualIgnoreDurability(filterIsEmpty, filterComponents, isEmpty, components);
	}

	FilterLogic getFilterLogic();

	boolean voidsOverflow();

	boolean worksInGui();

	ItemStack onOverflow(ItemStack stack);

	int onOverflow(ItemResource resource, int amount);

	boolean stackMatchesFilter(ItemStack stack);

	boolean matchesFilter(ItemResource resource);
}
