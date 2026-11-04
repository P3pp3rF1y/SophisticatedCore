package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class UpgradeHandlerTest {
	@Test
	void requiresRegistryAccessForMigrationOnlyForUpgradesWithLegacyData() {
		ItemStack legacyUpgrade = spy(new ItemStack(Items.STICK));
		doReturn(mock(VoidUpgradeItem.class)).when(legacyUpgrade).getItem();
		CompoundTag legacyData = new CompoundTag();
		legacyData.putBoolean("shouldWorkInGUI", true);
		legacyUpgrade.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));
		ItemStack currentUpgrade = spy(new ItemStack(Items.STICK));
		doReturn(mock(VoidUpgradeItem.class)).when(currentUpgrade).getItem();
		ItemStack nonUpgrade = new ItemStack(Items.STICK);
		nonUpgrade.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));

		assertTrue(UpgradeHandler.requiresRegistryAccessForMigration(legacyUpgrade));
		assertFalse(UpgradeHandler.requiresRegistryAccessForMigration(currentUpgrade));
		assertFalse(UpgradeHandler.requiresRegistryAccessForMigration(nonUpgrade));
	}

	@Test
	void retriesTypeAndAccessorInitializationAfterRegistryAccessBecomesAvailable() {
		RegistryAccess registryAccess = mock(RegistryAccess.class);
		AtomicReference<Optional<RegistryAccess>> availableRegistryAccess = new AtomicReference<>(Optional.empty());
		try (MockedStatic<RegistryHelper> registryHelper = mockStatic(RegistryHelper.class)) {
			registryHelper.when(RegistryHelper::getRegistryAccess).thenAnswer(invocation -> availableRegistryAccess.get());

			VoidUpgradeItem upgradeItem = mock(VoidUpgradeItem.class);
			VoidUpgradeWrapper wrapper = mock(VoidUpgradeWrapper.class);
			UpgradeType<VoidUpgradeWrapper> type = new UpgradeType<>((storageWrapper, upgrade, saveHandler) -> wrapper);
			when(upgradeItem.getType()).thenReturn(type);
			ItemStack legacyUpgrade = legacyUpgradeStack(upgradeItem);
			when(wrapper.getUpgradeStack()).thenReturn(legacyUpgrade);
			when(wrapper.isEnabled()).thenReturn(true);

			IStorageWrapper storageWrapper = mock(IStorageWrapper.class);
			when(storageWrapper.getRenderInfo()).thenReturn(mock(RenderInfo.class));
			when(storageWrapper.isUpgradeRunnable(legacyUpgrade)).thenReturn(true);
			TestUpgradeHandler handler = new TestUpgradeHandler(storageWrapper);
			handler.setStackDirectly(legacyUpgrade);

			assertTrue(handler.getTypeWrappers(type).isEmpty());
			assertTrue(handler.getWrappersThatImplement(IUpgradeWrapper.class).isEmpty());

			availableRegistryAccess.set(Optional.of(registryAccess));

			assertEquals(1, handler.getSlotWrappers().size());
			assertSame(wrapper, handler.getSlotWrappers().get(0));
			assertSame(wrapper, handler.getTypeWrappers(type).getFirst());
			assertEquals(1, handler.getWrappersThatImplement(IUpgradeWrapper.class).size());
			assertSame(wrapper, handler.getWrappersThatImplement(IUpgradeWrapper.class).getFirst());
		}
	}

	private static ItemStack legacyUpgradeStack(VoidUpgradeItem upgradeItem) {
		ItemStack legacyUpgrade = spy(new ItemStack(Items.STICK));
		doReturn(upgradeItem).when(legacyUpgrade).getItem();
		CompoundTag legacyData = new CompoundTag();
		legacyData.putString("unknownLegacyData", "retained");
		legacyUpgrade.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));
		return legacyUpgrade;
	}

	private static class TestUpgradeHandler extends UpgradeHandler {
		private TestUpgradeHandler(IStorageWrapper storageWrapper) {
			super(1, storageWrapper, new CompoundTag(), () -> {
			}, () -> {
			});
		}

		private void setStackDirectly(ItemStack stack) {
			stacks.set(0, stack);
		}
	}
}
