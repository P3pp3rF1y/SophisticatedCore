package net.p3pp3rf1y.sophisticatedcore.upgrades.battery;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

class BatteryUpgradeItemTest {
	@Test
	void canSwapUpgradeForRejectsSameBatteryWithPersistedEnergyAboveCapacity() {
		BatteryUpgradeItem batteryUpgrade = mock(BatteryUpgradeItem.class, CALLS_REAL_METHODS);
		ItemStack upgradeStack = spy(new ItemStack(Items.IRON_INGOT));
		upgradeStack.set(ModCoreDataComponents.ENERGY_STORED.get(), 40_001);
		doReturn(batteryUpgrade).when(upgradeStack).getItem();
		doReturn(40_000).when(batteryUpgrade).getMaxEnergyStored(any());

		UpgradeSlotChangeResult result = batteryUpgrade.canSwapUpgradeFor(upgradeStack, 0, mock(IStorageWrapper.class), false);

		assertFalse(result.successful());
		assertEquals(40_001, upgradeStack.get(ModCoreDataComponents.ENERGY_STORED.get()));
		TranslatableContents errorContents = assertInstanceOf(TranslatableContents.class, result.errorMessage().getContents());
		assertArrayEquals(new Object[]{"1.1"}, errorContents.getArgs());
	}
}
