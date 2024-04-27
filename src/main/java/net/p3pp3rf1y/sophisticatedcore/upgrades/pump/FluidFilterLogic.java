package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

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
		NBTHelper.getTagValue(upgrade, "", "fluidFilters", (c, n1) -> c.getList(n1, Tag.TAG_COMPOUND)).ifPresent(listNbt -> {
			int i = 0;
			for (Tag elementNbt : listNbt) {
				FluidStack value = FluidStack.loadFluidStackFromNBT((CompoundTag) elementNbt);
				if (value != null) {
					fluidFilters.set(i, value);
				}
				i++;
				if (i >= fluidFilters.size()) {
					break;
				}
			}
		});
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
			if (fluidFilter.isFluidEqual(fluid)) {
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
		ListTag fluids = new ListTag();
		fluidFilters.forEach(f -> fluids.add(f.writeToNBT(new CompoundTag())));
		upgrade.getOrCreateTag().put("fluidFilters", fluids);
	}
}
