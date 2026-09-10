package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CraftingContainerRecipeTransferHandlerServerTest {
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
	@CsvSource({"50, true", "50, false", "60, true", "60, false"})
	void recipeReplacementSavesAcceptedItemsAndReturnsRemainder(int storedCount, boolean playerHasSpace) throws Exception {
		StorageContainerMenuBase<?> menu = mock(StorageContainerMenuBase.class);
		SimpleContainer contents = new SimpleContainer(3);
		contents.setItem(0, new ItemStack(Items.IRON_INGOT, 10));
		contents.setItem(1, new ItemStack(Items.IRON_INGOT, storedCount));
		contents.setItem(2, new ItemStack(Items.DIAMOND, 64));
		contents.getItem(2).grow(1);
		AtomicInteger savedIronCount = new AtomicInteger(storedCount);
		contents.addListener(changedContents -> savedIronCount.set(changedContents.getItem(1).getCount()));

		Slot grid = spy(new Slot(contents, 0, 0, 0));
		List<Slot> slots = List.of(grid, new Slot(contents, 1, 0, 0), new Slot(contents, 2, 0, 0));
		Field inventorySlotsField = StorageContainerMenuBase.class.getField("realInventorySlots");
		inventorySlotsField.setAccessible(true);
		inventorySlotsField.set(menu, slots);
		Field upgradeSlotsField = StorageContainerMenuBase.class.getField("upgradeSlots");
		upgradeSlotsField.setAccessible(true);
		upgradeSlotsField.set(menu, List.of());
		when(menu.getSlot(anyInt())).thenAnswer(i -> slots.get(i.getArgument(0)));

		Player player = mock(Player.class);
		Inventory inventory = mock(Inventory.class);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.add(any(ItemStack.class))).thenReturn(playerHasSpace);
		player.containerMenu = menu;

		CraftingContainerRecipeTransferHandlerServer.setItems(player, Map.of(0, 2), List.of(0), List.of(1, 2), true);

		assertEquals(64, contents.getItem(0).getCount());
		assertTrue(contents.getItem(0).is(Items.DIAMOND));
		assertEquals(Math.min(64, storedCount + 10), contents.getItem(1).getCount());
		assertEquals(contents.getItem(1).getCount(), savedIronCount.get());
		assertEquals(1, contents.getItem(2).getCount());
		if (storedCount + 10 > 64) {
			ArgumentCaptor<ItemStack> remainder = ArgumentCaptor.forClass(ItemStack.class);
			verify(inventory).add(remainder.capture());
			assertEquals(storedCount + 10 - 64, remainder.getValue().getCount());
			assertTrue(remainder.getValue().is(Items.IRON_INGOT));
			verify(player, times(playerHasSpace ? 0 : 1)).drop(remainder.getValue(), false);
		} else {
			verify(inventory, never()).add(any(ItemStack.class));
			verify(player, never()).drop(any(ItemStack.class), anyBoolean());
		}
		verify(grid).remove(10);
		verify(grid, never()).remove(Integer.MAX_VALUE);
	}
}
