package net.p3pp3rf1y.sophisticatedcore.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IStorageWrapperInventoryLayoutTest {
	@Test
	void defaultLayoutPartsTreatStacksAsMovableAndInaccessibleSlotsAsFixed() {
		InventoryHandler inventoryHandler = mockInventoryHandler(Map.of(1, new ItemStack(Items.DIAMOND), 3, ItemStack.EMPTY), Set.of(3), 5);
		IStorageWrapper storageWrapper = mockStorageWrapper(inventoryHandler);

		assertEquals(List.of(
				new InventoryLayoutPart("stack:1", 1, 1, 1, Set.of(1)),
				new InventoryLayoutPart("fixed:3", 3, 1, 1, Set.of(3))
		), storageWrapper.getInventoryLayoutParts(5));
	}

	@Test
	void defaultLayoutPartsKeepStackSlotIndexWhenColumnsChange() {
		InventoryHandler inventoryHandler = mockInventoryHandler(Map.of(20, new ItemStack(Items.COBBLESTONE)), Set.of(), 42);
		IStorageWrapper storageWrapper = mockStorageWrapper(inventoryHandler);

		assertEquals(List.of(
				new InventoryLayoutPart("stack:20", 20, 1, 1, Set.of(20))
		), storageWrapper.getInventoryLayoutParts(9, 7));
	}

	@Test
	void defaultLayoutFitKeepsStackSlotIndexWhenItFitsAfterColumnsChange() {
		InventoryHandler inventoryHandler = mockInventoryHandler(Map.of(10, new ItemStack(Items.COBBLESTONE)), Set.of(), 18);
		IStorageWrapper storageWrapper = mockStorageWrapper(inventoryHandler);

		InventoryLayoutFitResult fitResult = InventoryLayoutFitter.fit(storageWrapper.getInventoryLayoutParts(9, 7), 14, 7);

		assertTrue(fitResult.fits());
		assertEquals(10, fitResult.fittedSlots().get("stack:10"));
	}

	@Test
	void defaultLayoutPartsTreatNoSortSlotsAsFixed() {
		InventoryHandler inventoryHandler = mockInventoryHandler(Map.of(2, new ItemStack(Items.COBBLESTONE)), Set.of(), 9);
		NoSortSettingsCategory noSortSettings = new NoSortSettingsCategory(new NoSortSettingsCategoryData(), () -> {});
		noSortSettings.selectSlot(2);
		IStorageWrapper storageWrapper = mockStorageWrapper(inventoryHandler, noSortSettings, emptyMemorySettings(inventoryHandler));

		assertEquals(List.of(
				new InventoryLayoutPart("fixed:2", 2, 1, 1, Set.of(2))
		), storageWrapper.getInventoryLayoutParts(9));
	}

	@Test
	void defaultLayoutPartsTreatMemorizedSlotsAsFixed() {
		InventoryHandler inventoryHandler = mockInventoryHandler(Map.of(4, new ItemStack(Items.DIAMOND)), Set.of(), 9);
		MemorySettingsCategory memorySettings = emptyMemorySettings(inventoryHandler);
		memorySettings.selectSlot(4);
		IStorageWrapper storageWrapper = mockStorageWrapper(inventoryHandler, emptyNoSortSettings(), memorySettings);

		assertEquals(List.of(
				new InventoryLayoutPart("fixed:4", 4, 1, 1, Set.of(4))
		), storageWrapper.getInventoryLayoutParts(9));
	}

	@Test
	void emptyNoSortSlotDoesNotContributeLayoutPart() {
		InventoryHandler inventoryHandler = mockInventoryHandler(Map.of(), Set.of(), 18);
		NoSortSettingsCategory noSortSettings = new NoSortSettingsCategory(new NoSortSettingsCategoryData(), () -> {});
		noSortSettings.selectSlot(17);
		IStorageWrapper storageWrapper = mockStorageWrapper(inventoryHandler, noSortSettings, emptyMemorySettings(inventoryHandler));

		InventoryLayoutFitResult fitResult = InventoryLayoutFitter.fit(storageWrapper.getInventoryLayoutParts(9, 7), 14, 7);

		assertTrue(fitResult.fits());
		assertEquals(List.of(), storageWrapper.getInventoryLayoutParts(9, 7));
		assertTrue(noSortSettings.getNoSortSlots().contains(17));
	}

	@Test
	void emptyMemorizedSlotDoesNotContributeLayoutPart() {
		InventoryHandler inventoryHandler = mockInventoryHandler(Map.of(), Set.of(), 18);
		MemorySettingsCategory memorySettings = emptyMemorySettings(inventoryHandler);
		memorySettings.setFilter(17, new ItemStack(Items.DIAMOND));
		IStorageWrapper storageWrapper = mockStorageWrapper(inventoryHandler, emptyNoSortSettings(), memorySettings);

		InventoryLayoutFitResult fitResult = InventoryLayoutFitter.fit(storageWrapper.getInventoryLayoutParts(9, 7), 14, 7);

		assertTrue(fitResult.fits());
		assertEquals(List.of(), storageWrapper.getInventoryLayoutParts(9, 7));
		assertTrue(memorySettings.isSlotSelected(17));
	}

	@Test
	void noSortSlotOutsideReducedInventoryBlocksLayoutFit() {
		InventoryHandler inventoryHandler = mockInventoryHandler(Map.of(15, new ItemStack(Items.COBBLESTONE)), Set.of(), 18);
		NoSortSettingsCategory noSortSettings = new NoSortSettingsCategory(new NoSortSettingsCategoryData(), () -> {});
		noSortSettings.selectSlot(15);
		IStorageWrapper storageWrapper = mockStorageWrapper(inventoryHandler, noSortSettings, emptyMemorySettings(inventoryHandler));

		InventoryLayoutFitResult fitResult = InventoryLayoutFitter.fit(storageWrapper.getInventoryLayoutParts(9, 7), 14, 7);

		assertFalse(fitResult.fits());
		assertEquals(Set.of(15), fitResult.errorSlots());
	}

	@Test
	void memorizedSlotOutsideReducedInventoryBlocksLayoutFit() {
		InventoryHandler inventoryHandler = mockInventoryHandler(Map.of(17, new ItemStack(Items.DIAMOND)), Set.of(), 18);
		MemorySettingsCategory memorySettings = emptyMemorySettings(inventoryHandler);
		memorySettings.selectSlot(17);
		IStorageWrapper storageWrapper = mockStorageWrapper(inventoryHandler, emptyNoSortSettings(), memorySettings);

		InventoryLayoutFitResult fitResult = InventoryLayoutFitter.fit(storageWrapper.getInventoryLayoutParts(9, 7), 14, 7);

		assertFalse(fitResult.fits());
		assertEquals(Set.of(17), fitResult.errorSlots());
	}

	@Test
	void noSortSlotStackKeepsSameSlotIndexWhenColumnsChange() {
		Map<Integer, ItemStack> stacks = new HashMap<>(Map.of(10, new ItemStack(Items.COBBLESTONE)));
		InventoryHandler inventoryHandler = mockInventoryHandler(stacks, Set.of(), 18);
		NoSortSettingsCategory noSortSettings = new NoSortSettingsCategory(new NoSortSettingsCategoryData(), () -> {});
		noSortSettings.selectSlot(10);
		IStorageWrapper storageWrapper = mockStorageWrapper(inventoryHandler, noSortSettings, emptyMemorySettings(inventoryHandler));
		InventoryLayoutFitResult fitResult = InventoryLayoutFitter.fit(storageWrapper.getInventoryLayoutParts(9, 7), 14, 7);

		assertTrue(fitResult.fits());
		storageWrapper.applyInventoryLayout(fitResult, 7);

		assertEquals(Items.COBBLESTONE, stacks.get(10).getItem());
		assertTrue(noSortSettings.getNoSortSlots().contains(10));
		assertFalse(noSortSettings.getNoSortSlots().contains(8));
	}

	@Test
	void memorizedSlotStackKeepsSameSlotIndexWhenColumnsChange() {
		Map<Integer, ItemStack> stacks = new HashMap<>(Map.of(10, new ItemStack(Items.DIAMOND)));
		InventoryHandler inventoryHandler = mockInventoryHandler(stacks, Set.of(), 18);
		MemorySettingsCategory memorySettings = emptyMemorySettings(inventoryHandler);
		memorySettings.selectSlot(10);
		IStorageWrapper storageWrapper = mockStorageWrapper(inventoryHandler, emptyNoSortSettings(), memorySettings);
		InventoryLayoutFitResult fitResult = InventoryLayoutFitter.fit(storageWrapper.getInventoryLayoutParts(9, 7), 14, 7);

		assertTrue(fitResult.fits());
		storageWrapper.applyInventoryLayout(fitResult, 7);

		assertEquals(Items.DIAMOND, stacks.get(10).getItem());
		assertTrue(memorySettings.isSlotSelected(10));
		assertFalse(memorySettings.isSlotSelected(8));
	}

	@Test
	void defaultLayoutApplicationMovesFittedStacks() {
		Map<Integer, ItemStack> stacks = new HashMap<>();
		stacks.put(0, ItemStack.EMPTY);
		stacks.put(1, ItemStack.EMPTY);
		stacks.put(2, new ItemStack(Items.DIAMOND));
		InventoryHandler inventoryHandler = mockInventoryHandler(stacks, Set.of(), 3);
		IStorageWrapper storageWrapper = mockStorageWrapper(inventoryHandler);

		storageWrapper.applyInventoryLayout(InventoryLayoutFitResult.fit(Map.of("stack:2", 0)), 3);

		assertTrue(stacks.get(2).isEmpty());
		assertEquals(Items.DIAMOND, stacks.get(0).getItem());
	}

	@Test
	void defaultLayoutApplicationDoesNotLoseStacksWhenMovingRowLeft() {
		Map<Integer, ItemStack> stacks = new HashMap<>();
		for (int slot = 0; slot <= 6; slot++) {
			stacks.put(slot, new ItemStack(Items.COBBLESTONE, slot + 1));
		}
		for (int slot = 9; slot <= 15; slot++) {
			stacks.put(slot, new ItemStack(Items.COBBLESTONE, slot + 1));
		}
		InventoryHandler inventoryHandler = mockInventoryHandler(stacks, Set.of(), 18);
		IStorageWrapper storageWrapper = mockStorageWrapper(inventoryHandler);
		InventoryLayoutFitResult fitResult = InventoryLayoutFitter.fit(storageWrapper.getInventoryLayoutParts(9, 7), 14, 7);

		assertTrue(fitResult.fits());
		storageWrapper.applyInventoryLayout(fitResult, 7);

		for (int slot = 0; slot <= 13; slot++) {
			assertEquals(Items.COBBLESTONE, stacks.get(slot).getItem(), "slot " + slot);
		}
		assertEquals(16, stacks.get(13).getCount());
	}

	@Test
	void defaultLayoutApplicationDoesNotLoseStacksWhenSameFitIsAppliedTwice() {
		Map<Integer, ItemStack> stacks = new HashMap<>();
		for (int slot = 0; slot <= 6; slot++) {
			stacks.put(slot, new ItemStack(Items.COBBLESTONE, slot + 1));
		}
		for (int slot = 9; slot <= 15; slot++) {
			stacks.put(slot, new ItemStack(Items.COBBLESTONE, slot + 1));
		}
		InventoryHandler inventoryHandler = mockInventoryHandler(stacks, Set.of(), 18);
		IStorageWrapper storageWrapper = mockStorageWrapper(inventoryHandler);
		InventoryLayoutFitResult fitResult = InventoryLayoutFitter.fit(storageWrapper.getInventoryLayoutParts(9, 7), 14, 7);

		assertTrue(fitResult.fits());
		storageWrapper.applyInventoryLayout(fitResult, 7);
		storageWrapper.applyInventoryLayout(fitResult, 7);

		for (int slot = 0; slot <= 13; slot++) {
			assertEquals(Items.COBBLESTONE, stacks.get(slot).getItem(), "slot " + slot);
		}
		assertEquals(16, stacks.get(13).getCount());
	}

	private InventoryHandler mockInventoryHandler(Map<Integer, ItemStack> stacks, Set<Integer> inaccessibleSlots, int slots) {
		Map<Integer, ItemStack> mutableStacks = stacks instanceof HashMap ? stacks : new HashMap<>(stacks);
		for (int slot = 0; slot < slots; slot++) {
			mutableStacks.putIfAbsent(slot, ItemStack.EMPTY);
		}

		InventoryHandler inventoryHandler = mock(InventoryHandler.class);
		when(inventoryHandler.size()).thenReturn(slots);
		when(inventoryHandler.getStackInSlot(anyInt())).thenAnswer(invocation -> mutableStacks.get(invocation.getArgument(0)));
		when(inventoryHandler.isSlotAccessible(anyInt())).thenAnswer(invocation -> !inaccessibleSlots.contains(invocation.getArgument(0)));
		doAnswer(invocation -> {
			mutableStacks.put(invocation.getArgument(0), invocation.getArgument(1));
			return null;
		}).when(inventoryHandler).setStackInSlot(anyInt(), any(ItemStack.class));
		return inventoryHandler;
	}

	private IStorageWrapper mockStorageWrapper(InventoryHandler inventoryHandler) {
		return mockStorageWrapper(inventoryHandler, emptyNoSortSettings(), emptyMemorySettings(inventoryHandler));
	}

	private IStorageWrapper mockStorageWrapper(InventoryHandler inventoryHandler, NoSortSettingsCategory noSortSettings, MemorySettingsCategory memorySettings) {
		SettingsHandler settingsHandler = mock(SettingsHandler.class);
		when(settingsHandler.getTypeCategory(NoSortSettingsCategory.class)).thenReturn(noSortSettings);
		when(settingsHandler.getTypeCategory(MemorySettingsCategory.class)).thenReturn(memorySettings);
		UpgradeHandler upgradeHandler = mock(UpgradeHandler.class);
		when(upgradeHandler.getWrappersThatImplement(any())).thenReturn(List.of());
		IStorageWrapper storageWrapper = mock(IStorageWrapper.class, CALLS_REAL_METHODS);
		when(storageWrapper.getInventoryHandler()).thenReturn(inventoryHandler);
		when(storageWrapper.getSettingsHandler()).thenReturn(settingsHandler);
		when(storageWrapper.getUpgradeHandler()).thenReturn(upgradeHandler);
		return storageWrapper;
	}

	private NoSortSettingsCategory emptyNoSortSettings() {
		return new NoSortSettingsCategory(new NoSortSettingsCategoryData(), () -> {});
	}

	private MemorySettingsCategory emptyMemorySettings(InventoryHandler inventoryHandler) {
		return new MemorySettingsCategory(() -> inventoryHandler, new MemorySettingsCategoryData(), () -> {});
	}
}
