package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidFilterLogicTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
		bindTestComponents(Items.STICK, Items.WATER_BUCKET);
		bindTestComponents(Fluids.WATER, Fluids.LAVA);
	}

	private static void bindTestComponents(Item... items) {
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		for (Item item : items) {
			item.builtInRegistryHolder().bindComponents(components);
		}
	}

	private static void bindTestComponents(Fluid... fluids) {
		for (Fluid fluid : fluids) {
			fluid.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		}
	}

	@Test
	void voidFilterStartsEmptyAndRetainsConfiguredFluid() {
		ItemStack upgrade = new ItemStack(Items.STICK);
		FluidFilterLogic fluidFilter = new FluidFilterLogic(1, upgrade, ignored -> {
		}, false);

		assertFalse(fluidFilter.fluidMatches(new FluidStack(Fluids.WATER, 1_000)));
		assertFalse(fluidFilter.fluidMatches(FluidResource.of(Fluids.WATER)));

		fluidFilter.setFluid(0, new FluidStack(Fluids.WATER, 1_000));
		FluidFilterLogic reloadedFilter = new FluidFilterLogic(1, upgrade, ignored -> {
		}, false);

		assertTrue(reloadedFilter.fluidMatches(new FluidStack(Fluids.WATER, 1_000)));
		assertTrue(reloadedFilter.fluidMatches(FluidResource.of(Fluids.WATER)));
		assertFalse(reloadedFilter.fluidMatches(new FluidStack(Fluids.LAVA, 1_000)));
	}

	@Test
	void getsFluidFromFilledBucket() {
		assertTrue(FluidFilterContainer.getContainedFluid(new ItemStack(Items.WATER_BUCKET)).is(Fluids.WATER));
	}
}
