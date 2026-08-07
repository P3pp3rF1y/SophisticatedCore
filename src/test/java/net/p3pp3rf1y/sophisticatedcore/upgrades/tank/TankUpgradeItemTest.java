package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TankUpgradeItemTest {
	@Test
	void canSwapUpgradeForRejectsSameTankWithContentsAboveTargetCapacity() {
		TankUpgradeItem tankUpgrade = mock(TankUpgradeItem.class, CALLS_REAL_METHODS);
		ItemStack upgradeStack = mock(ItemStack.class);
		when(upgradeStack.getItem()).thenReturn(tankUpgrade);
		when(upgradeStack.getOrDefault(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.EMPTY))
				.thenReturn(SimpleFluidContent.copyOf(new FluidStack(Fluids.WATER, 40_001)));
		doReturn(40_000).when(tankUpgrade).getTankCapacity(any());

		UpgradeSlotChangeResult result = tankUpgrade.canSwapUpgradeFor(upgradeStack, 0, mock(IStorageWrapper.class), false);

		assertFalse(result.successful());
		TranslatableContents errorContents = assertInstanceOf(TranslatableContents.class, result.errorMessage().getContents());
		assertArrayEquals(new Object[]{"1.1"}, errorContents.getArgs());
	}
}
