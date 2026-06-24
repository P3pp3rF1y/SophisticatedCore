package net.p3pp3rf1y.sophisticatedcore.upgrades.stack;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StackUpgradeItemTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void inventorySlotLimitDoesNotGoBelowOneWithDowngrades() {
		IStorageWrapper storageWrapper = mock(IStorageWrapper.class);
		UpgradeHandler upgradeHandler = mock(UpgradeHandler.class);
		StackUpgradeItem.Wrapper firstDowngrade = stackUpgradeWrapper(0.03125);
		StackUpgradeItem.Wrapper secondDowngrade = stackUpgradeWrapper(0.03125);
		when(storageWrapper.getUpgradeHandler()).thenReturn(upgradeHandler);
		when(storageWrapper.getBaseStackSizeMultiplier()).thenReturn(1);
		when(upgradeHandler.getTypeWrappers(StackUpgradeItem.TYPE)).thenReturn(List.of(firstDowngrade, secondDowngrade));

		Assertions.assertEquals(1, StackUpgradeItem.getInventorySlotLimit(storageWrapper));
	}

	@Test
	void lowMultiplierErrorReportsEffectiveSlotLimitMinimum() {
		IStorageWrapper storageWrapper = mock(IStorageWrapper.class);
		UpgradeHandler upgradeHandler = mock(UpgradeHandler.class);
		InventoryHandler inventoryHandler = mock(InventoryHandler.class);
		when(storageWrapper.getUpgradeHandler()).thenReturn(upgradeHandler);
		when(storageWrapper.getInventoryHandler()).thenReturn(inventoryHandler);
		when(upgradeHandler.getSlotWrappers()).thenReturn(Map.of());
		when(inventoryHandler.getSlots()).thenReturn(1);
		when(inventoryHandler.getSlotStack(0)).thenReturn(new ItemStack(Items.DIAMOND, 2));

		UpgradeSlotChangeResult result = StackUpgradeItem.isMultiplierHighEnough(storageWrapper, 0.0009765625, 0);

		Assertions.assertFalse(result.isSuccessful());
		TranslatableContents contents = Assertions.assertInstanceOf(TranslatableContents.class, result.getErrorMessage().orElseThrow().getContents());
		Assertions.assertArrayEquals(new Object[]{1}, contents.getArgs());
	}

	private static StackUpgradeItem.Wrapper stackUpgradeWrapper(double stackSizeMultiplier) {
		StackUpgradeItem.Wrapper wrapper = mock(StackUpgradeItem.Wrapper.class);
		when(wrapper.getStackSizeMultiplier()).thenReturn(stackSizeMultiplier);
		return wrapper;
	}
}
