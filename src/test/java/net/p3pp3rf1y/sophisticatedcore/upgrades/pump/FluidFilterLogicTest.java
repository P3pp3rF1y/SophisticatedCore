package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidFilterLogicTest {
	@Test
	void voidFilterStartsEmptyAndRetainsConfiguredFluid() {
		ItemStack upgrade = new ItemStack(Items.STICK);
		FluidFilterLogic fluidFilter = new FluidFilterLogic(1, upgrade, ignored -> {
		}, false);

		assertFalse(fluidFilter.fluidMatches(new FluidStack(Fluids.WATER, 1_000)));

		fluidFilter.setFluid(0, new FluidStack(Fluids.WATER, 1_000));
		FluidFilterLogic reloadedFilter = new FluidFilterLogic(1, upgrade, ignored -> {
		}, false);

		assertTrue(reloadedFilter.fluidMatches(new FluidStack(Fluids.WATER, 1_000)));
		assertFalse(reloadedFilter.fluidMatches(new FluidStack(Fluids.LAVA, 1_000)));
	}

	@Test
	void getsFluidFromFilledBucket() {
		assertTrue(FluidFilterContainer.getContainedFluid(new ItemStack(Items.WATER_BUCKET)).is(Fluids.WATER));
	}
}
