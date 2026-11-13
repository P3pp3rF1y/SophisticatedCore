package net.p3pp3rf1y.sophisticatedcore.upgrades.battery;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class BatteryUpgradeWrapperTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void receiveEnergyDoesNotPersistOrRenderWhenNoEnergyMoves() {
		BatteryUpgradeFixture fullBattery = createBatteryUpgradeFixture(100, 100);
		BatteryUpgradeFixture emptyBattery = createBatteryUpgradeFixture(0, 100);

		int receivedByFullBattery = fullBattery.wrapper().receiveEnergy(20, false);
		int receivedByZeroRequest = emptyBattery.wrapper().receiveEnergy(0, false);
		int simulatedReceived = emptyBattery.wrapper().receiveEnergy(20, true);

		assertEquals(0, receivedByFullBattery);
		assertEquals(0, receivedByZeroRequest);
		assertEquals(20, simulatedReceived);
		assertUnchanged(fullBattery, 100);
		assertUnchanged(emptyBattery, 0);
	}

	@Test
	void extractEnergyDoesNotPersistOrRenderWhenNoEnergyMoves() {
		BatteryUpgradeFixture emptyBattery = createBatteryUpgradeFixture(0, 100);
		BatteryUpgradeFixture chargedBattery = createBatteryUpgradeFixture(100, 100);

		int extractedFromEmptyBattery = emptyBattery.wrapper().extractEnergy(20, false);
		int extractedByZeroRequest = chargedBattery.wrapper().extractEnergy(0, false);
		int simulatedExtracted = chargedBattery.wrapper().extractEnergy(20, true);

		assertEquals(0, extractedFromEmptyBattery);
		assertEquals(0, extractedByZeroRequest);
		assertEquals(20, simulatedExtracted);
		assertUnchanged(emptyBattery, 0);
		assertUnchanged(chargedBattery, 100);
	}

	@Test
	void receiveEnergyPersistsAndRendersWhenEnergyMoves() {
		BatteryUpgradeFixture battery = createBatteryUpgradeFixture(40, 100);

		int received = battery.wrapper().receiveEnergy(20, false);

		assertEquals(20, received);
		assertEquals(60, battery.wrapper().getEnergyStored());
		assertEquals(60, battery.upgrade().getOrDefault(ModCoreDataComponents.ENERGY_STORED, 0));
		assertEquals(1, battery.saveCount().get());
		assertEquals(1, battery.renderUpdateCount().get());
	}

	@Test
	void extractEnergyPersistsAndRendersWhenEnergyMoves() {
		BatteryUpgradeFixture battery = createBatteryUpgradeFixture(60, 100);

		int extracted = battery.wrapper().extractEnergy(20, false);

		assertEquals(20, extracted);
		assertEquals(40, battery.wrapper().getEnergyStored());
		assertEquals(40, battery.upgrade().getOrDefault(ModCoreDataComponents.ENERGY_STORED, 0));
		assertEquals(1, battery.saveCount().get());
		assertEquals(1, battery.renderUpdateCount().get());
	}

	private static void assertUnchanged(BatteryUpgradeFixture battery, int expectedEnergy) {
		assertEquals(expectedEnergy, battery.wrapper().getEnergyStored());
		assertEquals(expectedEnergy, battery.upgrade().getOrDefault(ModCoreDataComponents.ENERGY_STORED, 0));
		assertEquals(0, battery.saveCount().get());
		assertEquals(0, battery.renderUpdateCount().get());
	}

	private static BatteryUpgradeFixture createBatteryUpgradeFixture(int energyStored, int maxEnergyStored) {
		BatteryUpgradeConfig config = createBatteryUpgradeConfig();
		BatteryUpgradeItem upgradeItem = mock(BatteryUpgradeItem.class, CALLS_REAL_METHODS);
		doReturn(config).when(upgradeItem).getBatteryUpgradeConfig();
		doReturn(1D).when(upgradeItem).getAdjustedStackMultiplier(any());
		doReturn(maxEnergyStored).when(upgradeItem).getMaxEnergyStored(any());
		ItemStack upgrade = spy(new ItemStack(Items.STICK));
		doReturn(upgradeItem).when(upgrade).getItem();
		upgrade.set(ModCoreDataComponents.ENERGY_STORED, energyStored);
		IStorageWrapper storageWrapper = mock(IStorageWrapper.class);
		when(storageWrapper.getNumberOfSlotRows()).thenReturn(1);
		AtomicInteger saveCount = new AtomicInteger();
		AtomicInteger renderUpdateCount = new AtomicInteger();
		BatteryUpgradeWrapper wrapper = new BatteryUpgradeWrapper(storageWrapper, upgrade, stack -> saveCount.incrementAndGet());
		wrapper.setBatteryRenderInfoUpdateCallback(renderInfo -> renderUpdateCount.incrementAndGet());
		return new BatteryUpgradeFixture(wrapper, upgrade, saveCount, renderUpdateCount);
	}

	private static BatteryUpgradeConfig createBatteryUpgradeConfig() {
		ModConfigSpec.Builder builder = mock(ModConfigSpec.Builder.class, RETURNS_SELF);
		ModConfigSpec.IntValue maxInputOutput = mock(ModConfigSpec.IntValue.class);
		when(builder.defineInRange("maxInputOutput", 20, 1, 1000)).thenReturn(maxInputOutput);
		when(maxInputOutput.get()).thenReturn(20);
		return new BatteryUpgradeConfig(builder);
	}

	private record BatteryUpgradeFixture(BatteryUpgradeWrapper wrapper, ItemStack upgrade, AtomicInteger saveCount, AtomicInteger renderUpdateCount) {
	}
}
