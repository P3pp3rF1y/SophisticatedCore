package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class TankUpgradeItemTest {
	@Test
	void canSwapUpgradeForRejectsSameTankWithPersistedContentsAboveCapacity() {
		TankUpgradeItem tankUpgrade = mock(TankUpgradeItem.class, CALLS_REAL_METHODS);
		ItemStack upgradeStack = spy(new ItemStack(Items.IRON_INGOT));
		upgradeStack.set(ModCoreDataComponents.FLUID_CONTENTS.get(), SimpleFluidContent.copyOf(new FluidStack(Fluids.WATER, 40_001)));
		doReturn(tankUpgrade).when(upgradeStack).getItem();
		doReturn(40_000).when(tankUpgrade).getTankCapacity(any());

		UpgradeSlotChangeResult result = tankUpgrade.canSwapUpgradeFor(upgradeStack, 0, mock(IStorageWrapper.class), false);

		assertFalse(result.successful());
		assertEquals(40_001, upgradeStack.get(ModCoreDataComponents.FLUID_CONTENTS.get()).getAmount());
		TranslatableContents errorContents = assertInstanceOf(TranslatableContents.class, result.errorMessage().getContents());
		assertArrayEquals(new Object[]{"1.1"}, errorContents.getArgs());
	}
}
