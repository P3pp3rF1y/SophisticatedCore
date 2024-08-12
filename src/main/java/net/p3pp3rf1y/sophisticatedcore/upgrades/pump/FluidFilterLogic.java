package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class FluidFilterLogic {
	private final List<FluidStack> fluidFilters;
	private final ItemStack upgrade;
	private final Consumer<ItemStack> saveHandler;
	private boolean noFilter = true;

	public FluidFilterLogic(int filterSlots, ItemStack upgrade, Consumer<ItemStack> saveHandler) {
		fluidFilters = NonNullList.withSize(filterSlots, FluidStack.EMPTY);
		this.upgrade = upgrade;
		this.saveHandler = saveHandler;
		deserializeFluidFilters();
		updateNoFilter();
	}

	private void deserializeFluidFilters() {
		List<SimpleFluidContent> deserializedFilters = upgrade.getOrDefault(ModCoreDataComponents.FLUID_FILTERS, Collections.emptyList());
		for (int i = 0; i < deserializedFilters.size() && i < fluidFilters.size(); i++) {
			fluidFilters.set(i, deserializedFilters.get(i).copy());
		}
	}

	private void updateNoFilter() {
		noFilter = true;
		for (FluidStack fluidFilter : fluidFilters) {
			if (!fluidFilter.isEmpty()) {
				noFilter = false;
				return;
			}
		}
	}

	public boolean fluidMatches(FluidStack fluid) {
		return noFilter || matchesFluidFilter(fluid);
	}

	private boolean matchesFluidFilter(FluidStack fluid) {
		for (FluidStack fluidFilter : fluidFilters) {
			if (FluidStack.isSameFluidSameComponents(fluidFilter, fluid)) {
				return true;
			}
		}
		return false;
	}

	private void save() {
		saveHandler.accept(upgrade);
	}

	public void setFluid(int index, FluidStack fluid) {
		fluidFilters.set(index, fluid.copy());
		serializeFluidFilters();
		updateNoFilter();
		save();
	}

	public FluidStack getFluid(int index) {
		return fluidFilters.get(index);
	}

	public int getNumberOfFluidFilters() {
		return fluidFilters.size();
	}

	private void serializeFluidFilters() {
		upgrade.set(ModCoreDataComponents.FLUID_FILTERS, fluidFilters.stream().map(SimpleFluidContent::copyOf).toList());
	}
}
