package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.*;

class StorageContainerMenuBaseTest {
	@BeforeAll
	static void setup() throws Exception {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		try (MockedStatic<ObfuscationReflectionHelper> reflection = mockStatic(ObfuscationReflectionHelper.class)) {
			Method method = Slot.class.getDeclaredMethod("onSwapCraft", int.class);
			method.setAccessible(true);
			reflection.when(() -> ObfuscationReflectionHelper.findMethod(Slot.class, "m_6405_", int.class)).thenReturn(method);
			Class.forName(StorageContainerMenuBase.class.getName());
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {32, 1000})
	void hotbarExtractionDoesNotShareTheMutableSlotStack(int count) {
		StorageContainerMenuBase<?> menu = mock(StorageContainerMenuBase.class, CALLS_REAL_METHODS);
		Player player = mock(Player.class);
		Inventory inventory = mock(Inventory.class);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.getItem(0)).thenReturn(ItemStack.EMPTY);
		Slot slot = mock(Slot.class);
		ItemStack contents = new ItemStack(Items.IRON_INGOT, count);
		when(slot.getItem()).thenReturn(contents);
		when(slot.mayPickup(player)).thenReturn(true);
		doReturn(slot).when(menu).getSlot(0);
		doAnswer(i -> {
			contents.setCount(((ItemStack) i.getArgument(0)).getCount());
			return null;
		}).when(slot).set(any(ItemStack.class));

		menu.doClick(0, 0, ClickType.SWAP, player);

		ArgumentCaptor<ItemStack> taken = ArgumentCaptor.forClass(ItemStack.class);
		verify(inventory).setItem(eq(0), taken.capture());
		assertEquals(Math.min(count, 64), taken.getValue().getCount());
		assertEquals(count, taken.getValue().getCount() + contents.getCount());
		assertNotSame(contents, taken.getValue());
	}

	@Test
	void anotherPlayersGridChangeRefreshesUpgradeSlotsOnce() throws Exception {
		StorageContainerMenuBase<?> menu = mock(StorageContainerMenuBase.class, CALLS_REAL_METHODS);
		Field listeners = AbstractContainerMenu.class.getDeclaredField("containerListeners");
		listeners.setAccessible(true);
		listeners.set(menu, List.of());
		Slot craftingSlot = mock(Slot.class);
		craftingSlot.index = 42;
		doReturn(true).when(menu).isUpgradeSettingsSlot(42);
		NonNullList<ItemStack> previous = NonNullList.withSize(1, new ItemStack(Items.IRON_INGOT));

		menu.triggerSlotListeners(0, ItemStack.EMPTY, () -> ItemStack.EMPTY, previous, 42, craftingSlot);
		verify(craftingSlot).setChanged();

		menu.triggerSlotListeners(0, ItemStack.EMPTY, () -> ItemStack.EMPTY, previous, 42, craftingSlot);
		verify(craftingSlot, times(1)).setChanged();
	}
}
