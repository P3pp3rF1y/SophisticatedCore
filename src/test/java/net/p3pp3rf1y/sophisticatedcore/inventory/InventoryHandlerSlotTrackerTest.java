package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

class InventoryHandlerSlotTrackerTest {
	@BeforeAll
	static void setup() throws Exception {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		try (MockedStatic<ObfuscationReflectionHelper> reflection = mockStatic(ObfuscationReflectionHelper.class)) {
			Field field = ItemStack.class.getDeclaredField("capNBT");
			field.setAccessible(true);
			reflection.when(() -> ObfuscationReflectionHelper.findField(ItemStack.class, "capNBT")).thenReturn(field);
			Class.forName(ItemStackKey.class.getName());
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void fallbackSkipsBothFilterAndMemorySlots(boolean simulate) {
		InventoryHandler handler = mock(InventoryHandler.class);
		when(handler.getSlots()).thenReturn(3);
		when(handler.getStackInSlot(anyInt())).thenReturn(ItemStack.EMPTY);
		MemorySettingsCategory memory = mock(MemorySettingsCategory.class);
		when(memory.isSlotSelected(1)).thenReturn(true);
		InventoryHandlerSlotTracker tracker = new InventoryHandlerSlotTracker(memory, Map.of(Items.IRON_INGOT, Set.of(0)));
		tracker.refreshSlotIndexesFrom(handler);
		List<Integer> insertedSlots = new ArrayList<>();
		ItemStack stack = new ItemStack(Items.DIAMOND, 32);

		ItemStack remainder = tracker.insertItemIntoHandler(handler, (slot, inserted, simulation) -> {
			assertEquals(simulate, simulation);
			insertedSlots.add(slot);
			return ItemStack.EMPTY;
		}, s -> s, stack, simulate);

		assertTrue(remainder.isEmpty());
		assertEquals(List.of(2), insertedSlots);
		assertEquals(32, stack.getCount());
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void reservedSlotsReturnUnmatchedItemsAndAcceptTheirFilter(boolean simulate) {
		InventoryHandler handler = mock(InventoryHandler.class);
		when(handler.getSlots()).thenReturn(1);
		when(handler.getStackInSlot(0)).thenReturn(ItemStack.EMPTY);
		InventoryHandlerSlotTracker tracker = new InventoryHandlerSlotTracker(mock(MemorySettingsCategory.class), Map.of(Items.IRON_INGOT, Set.of(0)));
		tracker.refreshSlotIndexesFrom(handler);
		ItemStack unmatched = new ItemStack(Items.DIAMOND, 32);

		assertSame(unmatched, tracker.insertItemIntoHandler(handler, (slot, stack, simulation) -> {
			fail("Unmatched items reached a reserved slot");
			return ItemStack.EMPTY;
		}, s -> s, unmatched, simulate));
		assertTrue(tracker.insertItemIntoHandler(handler, (slot, stack, simulation) -> ItemStack.EMPTY, s -> s, new ItemStack(Items.IRON_INGOT), simulate).isEmpty());
	}
}
