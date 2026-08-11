package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VoidUpgradeWrapperTest {

	@Test
	void overflowMatchIncludesPartialStackWhenNbtIsIgnored() {
		Object partiallyFilledStack = new Object();

		assertTrue(VoidUpgradeWrapper.hasOverflowMatch(Set.of(), Set.of(partiallyFilledStack), stackKey -> stackKey == partiallyFilledStack));
	}

	@Test
	void shouldVoidFluidMatchesConfiguredFluidFilter() {
		VoidUpgradeWrapper wrapper = getVoidUpgradeWrapper();
		wrapper.getFluidFilterLogic().setFluid(0, new FluidStack(Fluids.WATER, 1_000));

		assertTrue(wrapper.shouldVoidFluid(new FluidStack(Fluids.WATER, 1_000), VoidType.ALWAYS));
	}

	@Test
	void shouldVoidFluidRejectsConfiguredFluidForDenyList() {
		VoidUpgradeWrapper wrapper = getVoidUpgradeWrapper();
		wrapper.getFluidFilterLogic().setFluid(0, new FluidStack(Fluids.LAVA, 1_000));
		wrapper.getFilterLogic().setAllowList(false);

		assertFalse(wrapper.shouldVoidFluid(new FluidStack(Fluids.LAVA, 1_000), VoidType.ALWAYS));
	}

	private static VoidUpgradeWrapper getVoidUpgradeWrapper() {
		VoidUpgradeItem upgradeItem = mock(VoidUpgradeItem.class);
		when(upgradeItem.getFilterSlotCount()).thenReturn(1);
		when(upgradeItem.isVoidAlwaysEnabled()).thenReturn(true);
		ItemStack upgrade = mock(ItemStack.class);
		CompoundTag tag = new CompoundTag();
		when(upgrade.getItem()).thenReturn(upgradeItem);
		when(upgrade.getTag()).thenReturn(tag);
		when(upgrade.getOrCreateTag()).thenReturn(tag);
		return new VoidUpgradeWrapper(mock(IStorageWrapper.class), upgrade, stack -> {
		});
	}
}
