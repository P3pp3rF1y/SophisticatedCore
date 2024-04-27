package net.p3pp3rf1y.sophisticatedcore.api;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public interface IStorageFluidHandler extends IFluidHandlerItem {
	default int fill(TagKey<Fluid> fluidTag, int maxFill, Fluid fallbackFluid, FluidAction action) {
		return fill(fluidTag, maxFill, fallbackFluid, action, false);
	}

	default int fill(TagKey<Fluid> fluidTag, int maxFill, Fluid fallbackFluid, FluidAction action, boolean ignoreInOutLimit) {
		for (int tank = 0; tank < getTanks(); tank++) {
			FluidStack tankFluid = getFluidInTank(tank);
			if (tankFluid.getFluid().is(fluidTag)) {
				return fill(new FluidStack(tankFluid, maxFill), action, ignoreInOutLimit);
			}
		}
		return fill(new FluidStack(fallbackFluid, maxFill), action, ignoreInOutLimit);
	}

	int fill(FluidStack resource, FluidAction action, boolean ignoreInOutLimit);

	FluidStack drain(TagKey<Fluid> resourceTag, int maxDrain, FluidAction action, boolean ignoreInOutLimit);

	FluidStack drain(FluidStack resource, FluidAction action, boolean ignoreInOutLimit);

	FluidStack drain(int maxDrain, FluidAction action, boolean ignoreInOutLimit);
}
