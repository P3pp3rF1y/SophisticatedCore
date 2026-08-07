package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.core.component.DataComponentMap;
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
		IStorageWrapper storageWrapper = mock(IStorageWrapper.class);
		TankUpgradeItem tankUpgrade = mock(TankUpgradeItem.class, CALLS_REAL_METHODS);
		when(tankUpgrade.asItem()).thenReturn(tankUpgrade);
		when(tankUpgrade.components()).thenReturn(DataComponentMap.EMPTY);
		doReturn(40_000).when(tankUpgrade).getTankCapacity(any());
		ItemStack upgradeStack = new ItemStack(tankUpgrade);
		upgradeStack.set(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.copyOf(new FluidStack(Fluids.WATER, 40_001)));

		UpgradeSlotChangeResult result = tankUpgrade.canSwapUpgradeFor(upgradeStack, 0, storageWrapper, false);

		assertFalse(result.successful());
		TranslatableContents errorContents = assertInstanceOf(TranslatableContents.class, result.errorMessage().getContents());
		assertArrayEquals(new Object[]{"1.1"}, errorContents.getArgs());
	}
}
