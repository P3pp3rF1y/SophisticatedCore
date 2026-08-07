package net.p3pp3rf1y.sophisticatedcore.upgrades.battery;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import org.junit.jupiter.api.BeforeAll;
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
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void canSwapUpgradeForRejectsSameBatteryWithPersistedEnergyAboveTargetCapacity() {
		BatteryUpgradeItem batteryUpgrade = mock(BatteryUpgradeItem.class, CALLS_REAL_METHODS);
		ItemStack upgradeStack = mock(ItemStack.class);
		CompoundTag tag = new CompoundTag();
		tag.putInt(BatteryUpgradeWrapper.ENERGY_STORED_TAG, 40_001);
		when(upgradeStack.getItem()).thenReturn(batteryUpgrade);
		when(upgradeStack.getTag()).thenReturn(tag);
		doReturn(40_000).when(batteryUpgrade).getMaxEnergyStored(any());

		UpgradeSlotChangeResult result = batteryUpgrade.canSwapUpgradeFor(upgradeStack, 0, mock(IStorageWrapper.class), false);

		assertFalse(result.isSuccessful());
		TranslatableContents errorContents = assertInstanceOf(TranslatableContents.class, result.getErrorMessage().orElseThrow().getContents());
		assertArrayEquals(new Object[]{"1.1"}, errorContents.getArgs());
	}
}
