package net.p3pp3rf1y.sophisticatedcore.upgrades.battery;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
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

class BatteryUpgradeItemTest {
	@Test
	void canSwapUpgradeForRejectsSameBatteryWithEnergyAboveTargetCapacity() {
		IStorageWrapper storageWrapper = mock(IStorageWrapper.class);
		BatteryUpgradeItem batteryUpgrade = mock(BatteryUpgradeItem.class, CALLS_REAL_METHODS);
		when(batteryUpgrade.asItem()).thenReturn(batteryUpgrade);
		when(batteryUpgrade.components()).thenReturn(DataComponentMap.EMPTY);
		doReturn(40_000).when(batteryUpgrade).getMaxEnergyStored(any());
		ItemStack upgradeStack = new ItemStack(batteryUpgrade);
		upgradeStack.set(ModCoreDataComponents.ENERGY_STORED, 40_001);

		UpgradeSlotChangeResult result = batteryUpgrade.canSwapUpgradeFor(upgradeStack, 0, storageWrapper, false);

		assertFalse(result.successful());
		TranslatableContents errorContents = assertInstanceOf(TranslatableContents.class, result.errorMessage().getContents());
		assertArrayEquals(new Object[]{"1.1"}, errorContents.getArgs());
	}
}
