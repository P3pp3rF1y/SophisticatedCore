package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IInsertResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IOverflowResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryHandlerSlotOverflowTest {
	@Test
	void insertItemVoidsSlotOverflowAfterPartialStackBecomesFull() {
		Fixture fixture = new Fixture(VoidType.SLOT_OVERFLOW);

		ItemStack remainder = fixture.inventory.insertItem(new ItemStack(Items.DIAMOND, 2), false);

		assertTrue(remainder.isEmpty());
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
		verify(fixture.voidUpgrade).onAfterInsert(fixture.inventory, 0);
		verify(fixture.voidUpgrade, never()).onAfterInsert(fixture.inventory, 1);
	}

	@Test
	void insertItemSimulationVoidsSlotOverflowWithoutChangingContents() {
		Fixture fixture = new Fixture(VoidType.SLOT_OVERFLOW);

		ItemStack remainder = fixture.inventory.insertItem(new ItemStack(Items.DIAMOND, 2), true);

		assertTrue(remainder.isEmpty());
		assertEquals(63, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
		verify(fixture.voidUpgrade, never()).onAfterInsert(eq(fixture.inventory), anyInt());
	}

	@Test
	void insertItemStoresRemainderInEmptySlotWithStorageOverflowMode() {
		Fixture fixture = new Fixture(VoidType.STORAGE_OVERFLOW);

		ItemStack remainder = fixture.inventory.insertItem(new ItemStack(Items.DIAMOND, 2), false);

		assertTrue(remainder.isEmpty());
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertEquals(1, fixture.inventory.getStackInSlot(1).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).is(Items.DIAMOND));
	}

	@Test
	void insertItemSimulationUsesEmptySlotWithStorageOverflowModeWithoutChangingContents() {
		Fixture fixture = new Fixture(VoidType.STORAGE_OVERFLOW);

		ItemStack remainder = fixture.inventory.insertItem(new ItemStack(Items.DIAMOND, 2), true);

		assertTrue(remainder.isEmpty());
		assertEquals(63, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	private static class Fixture {
		private InventoryHandler inventory;
		private final VoidUpgradeWrapper voidUpgrade;

		private Fixture(VoidType voidType) {
			IStorageWrapper wrapper = mock(IStorageWrapper.class);
			SettingsHandler settings = mock(SettingsHandler.class);
			UpgradeHandler upgrades = mock(UpgradeHandler.class);
			MemorySettingsCategory memory = new MemorySettingsCategory(() -> inventory, new CompoundTag(), tag -> {
			});
			when(wrapper.getSettingsHandler()).thenReturn(settings);
			when(wrapper.getUpgradeHandler()).thenReturn(upgrades);
			when(settings.getTypeCategory(MemorySettingsCategory.class)).thenReturn(memory);
			StackUpgradeConfig config = mock(StackUpgradeConfig.class);
			when(config.canStackItem(any(Item.class))).thenReturn(true);
			inventory = new InventoryHandler(2, wrapper, new CompoundTag(), () -> {
			}, 64, config) {
				@Override
				protected boolean isAllowed(ItemStack stack) {
					return true;
				}
			};
			inventory.setPersistent(false);
			inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 63));
			inventory.getSlotTracker().refreshSlotIndexesFrom(inventory);
			when(wrapper.getInventoryHandler()).thenReturn(inventory);
			VoidUpgradeItem item = mock(VoidUpgradeItem.class);
			when(item.getFilterSlotCount()).thenReturn(1);
			when(item.isVoidAlwaysEnabled()).thenReturn(true);
			ItemStack stack = mock(ItemStack.class);
			Map<Object, Object> components = new HashMap<>();
			when(stack.getItem()).thenReturn(item);
			when(stack.getOrDefault(anyDataComponentSupplier(), any()))
					.thenAnswer(invocation -> components.getOrDefault(invocation.getArgument(0), invocation.getArgument(1)));
			doAnswer(invocation -> components.put(invocation.getArgument(0), invocation.getArgument(1))).when(stack).set(anySetDataComponentSupplier(), any());
			voidUpgrade = spy(new VoidUpgradeWrapper(wrapper, stack, ignored -> {
			}));
			voidUpgrade.setVoidType(voidType);
			voidUpgrade.getFilterLogic().setAllowList(false);
			when(upgrades.getTypeWrappers(VoidUpgradeItem.TYPE)).thenReturn(List.of(voidUpgrade));
			when(upgrades.getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class)).thenReturn(List.of(voidUpgrade));
			when(upgrades.getWrappersThatImplementFromMainStorage(IOverflowResponseUpgrade.class)).thenReturn(List.of(voidUpgrade));
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Supplier<? extends DataComponentType<? extends T>> anyDataComponentSupplier() {
		return (Supplier<? extends DataComponentType<? extends T>>) any(Supplier.class);
	}

	@SuppressWarnings("unchecked")
	private static Supplier<? extends DataComponentType<Object>> anySetDataComponentSupplier() {
		return (Supplier<? extends DataComponentType<Object>>) any(Supplier.class);
	}
}
