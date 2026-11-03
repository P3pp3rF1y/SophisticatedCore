package net.p3pp3rf1y.sophisticatedcore.upgrades.compacting;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IExtractResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IInsertResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static net.p3pp3rf1y.sophisticatedcore.HelperAssertions.assertStackEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompactingUpgradeWrapperTest {
	@Test
	void basicUpgradeCompactsConfiguredTwoByTwoShape() {
		ItemStack stack = new ItemStack(Items.CHARCOAL);
		CompactingUpgradeItem upgradeItem = getUpgradeItemWithConfiguredShape(false, 2, 4);

		Optional<CompactingUpgradeWrapper.CompactingDefinition> compactingDefinition = CompactingUpgradeWrapper.getCompactingDefinition(stack, upgradeItem,
				false);

		verify(upgradeItem).getConfiguredCompactingResult(any(ItemStack.class), eq(2), eq(2));
		assertTrue(compactingDefinition.isPresent(), "Basic compacting upgrade should compact configured 2x2-compatible shape");
		CompactingUpgradeWrapper.CompactingDefinition result = compactingDefinition.orElseThrow();
		assertEquals(4, result.count());
		assertStackEquals(new ItemStack(Items.COAL), result.result().getResult(), "Basic compacting upgrade should compact configured 2x2-compatible shape");
	}

	@Test
	void basicUpgradeDoesNotCompactConfiguredThreeByThreeShape() {
		ItemStack stack = new ItemStack(Items.CHARCOAL);
		CompactingUpgradeItem upgradeItem = getUpgradeItemWithConfiguredShape(false, 3, 8);

		Optional<CompactingUpgradeWrapper.CompactingDefinition> compactingDefinition = CompactingUpgradeWrapper.getCompactingDefinition(stack, upgradeItem,
				false);

		verify(upgradeItem).getConfiguredCompactingResult(any(ItemStack.class), eq(2), eq(2));
		assertFalse(compactingDefinition.isPresent(), "Basic compacting upgrade should not compact configured 3x3 shape");
	}

	@Test
	void advancedUpgradeCompactsConfiguredThreeByThreeShape() {
		ItemStack stack = new ItemStack(Items.CHARCOAL);
		CompactingUpgradeItem upgradeItem = getUpgradeItemWithConfiguredShape(true, 3, 8);

		Optional<CompactingUpgradeWrapper.CompactingDefinition> compactingDefinition = CompactingUpgradeWrapper.getCompactingDefinition(stack, upgradeItem,
				false);

		verify(upgradeItem).getConfiguredCompactingResult(any(ItemStack.class), eq(3), eq(3));
		assertTrue(compactingDefinition.isPresent(), "Advanced compacting upgrade should compact configured 3x3 shape");
		CompactingUpgradeWrapper.CompactingDefinition result = compactingDefinition.orElseThrow();
		assertEquals(8, result.count());
		assertStackEquals(new ItemStack(Items.COAL), result.result().getResult(), "Advanced compacting upgrade should compact configured 3x3 shape");
	}

	@Test
	void onAfterInsertProcessesReentrantInsertCallbacksWithoutNestedCompacting() {
		Map<Item, CompactingUpgradeConfig.CompactingDefinition> compactingDefinitions = Map.of(Items.DIAMOND,
				getConfiguredCompactingDefinition(new ItemStack(Items.DIAMOND_BLOCK), 9), Items.DIAMOND_BLOCK,
				getConfiguredCompactingDefinition(new ItemStack(Items.NETHERITE_BLOCK), 9));
		Map<Integer, ItemStack> initialSlotStacks = Map.of(0, new ItemStack(Items.DIAMOND, 91), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY);
		Map<Item, Integer> insertSlots = Map.of(Items.DIAMOND_BLOCK, 1, Items.NETHERITE_BLOCK, 2);
		CompactingUpgradeWrapper wrapper = getCompactingWrapperWithConfiguredCompacting(compactingDefinitions);
		ReentrantCompactingInventory inventory = getReentrantCompactingInventory(wrapper, initialSlotStacks, insertSlots);

		wrapper.onAfterInsert(inventory.handler(), 0);

		assertEquals(1, inventory.getCount(0));
		assertEquals(1, inventory.getCount(1));
		assertEquals(1, inventory.getCount(2));
		assertEquals(10, inventory.getInsertions(Items.DIAMOND_BLOCK));
		assertEquals(1, inventory.getInsertions(Items.NETHERITE_BLOCK));
		assertEquals(1, inventory.maxInsertDepth().get(), "Compaction recursively inserted results from insert callbacks");
	}

	@Test
	void tickDefersSlotsQueuedBySynchronousExtractionCallbacks() {
		Map<Item, CompactingUpgradeConfig.CompactingDefinition> compactingDefinitions = Map.of(Items.DIAMOND,
				getConfiguredCompactingDefinition(new ItemStack(Items.DIAMOND_BLOCK), 9), Items.DIAMOND_BLOCK,
				getConfiguredCompactingDefinition(new ItemStack(Items.NETHERITE_BLOCK), 9));
		IStorageWrapper storageWrapper = mock(IStorageWrapper.class);
		SettingsHandler settingsHandler = mock(SettingsHandler.class);
		UpgradeHandler upgradeHandler = mock(UpgradeHandler.class);
		MemorySettingsCategory memorySettings = new MemorySettingsCategory(storageWrapper::getInventoryHandler, new CompoundTag(), tag -> {
		});
		StackUpgradeConfig stackUpgradeConfig = mock(StackUpgradeConfig.class);
		when(storageWrapper.getSettingsHandler()).thenReturn(settingsHandler);
		when(storageWrapper.getUpgradeHandler()).thenReturn(upgradeHandler);
		when(settingsHandler.getTypeCategory(MemorySettingsCategory.class)).thenReturn(memorySettings);
		when(stackUpgradeConfig.canStackItem(any(Item.class))).thenReturn(true);
		InventoryHandler inventory = new InventoryHandler(3, storageWrapper, new CompoundTag(), () -> {
		}, 64, stackUpgradeConfig) {
			@Override
			protected boolean isAllowed(ItemStack stack) {
				return true;
			}
		};
		inventory.setPersistent(false);
		when(storageWrapper.getInventoryHandler()).thenReturn(inventory);
		CompactingUpgradeWrapper wrapper = getCompactingWrapperWithConfiguredCompacting(storageWrapper, compactingDefinitions);
		when(upgradeHandler.getWrappersThatImplementFromMainStorage(IExtractResponseUpgrade.class)).thenReturn(List.of(wrapper));
		when(upgradeHandler.getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class)).thenReturn(List.of());

		inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 64));
		inventory.setStackInSlot(1, new ItemStack(Items.DIAMOND_BLOCK, 64));
		inventory.setStackInSlot(2, new ItemStack(Items.NETHERITE_BLOCK, 64));
		wrapper.onSlotChange(inventory, 0);
		inventory.setStackInSlot(1, new ItemStack(Items.DIAMOND_BLOCK, 9));
		inventory.setStackInSlot(2, ItemStack.EMPTY);
		wrapper.setShouldWorkdInGUI(true);
		wrapper.onSlotChange(inventory, 1);

		assertDoesNotThrow(() -> wrapper.tick(null, null, null));
		assertEquals(64, inventory.getStackInSlot(0).getCount(), "Slots queued during a tick must wait for the next tick");

		wrapper.tick(null, null, null);

		assertEquals(1, inventory.getStackInSlot(0).getCount(), "The slot queued by extraction should be compacted on the next tick");
	}

	private static CompactingUpgradeWrapper getCompactingWrapperWithConfiguredCompacting(
			Map<Item, CompactingUpgradeConfig.CompactingDefinition> compactingDefinitions) {
		return getCompactingWrapperWithConfiguredCompacting(mock(IStorageWrapper.class), compactingDefinitions);
	}

	private static CompactingUpgradeWrapper getCompactingWrapperWithConfiguredCompacting(IStorageWrapper storageWrapper,
			Map<Item, CompactingUpgradeConfig.CompactingDefinition> compactingDefinitions) {
		CompactingUpgradeItem upgradeItem = mock(CompactingUpgradeItem.class);
		when(upgradeItem.getFilterSlotCount()).thenReturn(0);
		when(upgradeItem.shouldCompactThreeByThree()).thenReturn(true);
		when(upgradeItem.getConfiguredCompactingResult(any(ItemStack.class), eq(3), eq(3)))
				.thenAnswer(invocation -> Optional.ofNullable(compactingDefinitions.get(invocation.<ItemStack>getArgument(0).getItem())));
		ItemStack upgrade = mock(ItemStack.class);
		Map<Object, Object> components = new HashMap<>();
		when(upgrade.getItem()).thenReturn(upgradeItem);
		when(upgrade.getOrDefault(anyDataComponentSupplier(), any()))
				.thenAnswer(invocation -> components.getOrDefault(invocation.getArgument(0), invocation.getArgument(1)));
		doAnswer(invocation -> components.put(invocation.getArgument(0), invocation.getArgument(1))).when(upgrade).set(anySetDataComponentSupplier(), any());

		return new CompactingUpgradeWrapper(storageWrapper, upgrade, stack -> {
		});
	}

	private static ReentrantCompactingInventory getReentrantCompactingInventory(CompactingUpgradeWrapper wrapper, Map<Integer, ItemStack> initialSlotStacks,
			Map<Item, Integer> insertSlots) {
		Map<Integer, ItemStack> slotStacks = new HashMap<>();
		initialSlotStacks.forEach((slot, stack) -> slotStacks.put(slot, stack.copy()));
		Map<Item, AtomicInteger> insertions = new HashMap<>();
		IItemHandlerSimpleInserter inventoryHandler = mock(IItemHandlerSimpleInserter.class);
		AtomicInteger insertDepth = new AtomicInteger(0);
		AtomicInteger maxInsertDepth = new AtomicInteger(0);

		when(inventoryHandler.getSlots()).thenReturn(slotStacks.size());
		when(inventoryHandler.getStackInSlot(anyInt())).thenAnswer(invocation -> slotStacks.getOrDefault(invocation.getArgument(0), ItemStack.EMPTY));
		when(inventoryHandler.extractItem(anyInt(), anyInt(), anyBoolean())).thenAnswer(invocation -> {
			int slot = invocation.getArgument(0);
			ItemStack slotStack = slotStacks.getOrDefault(slot, ItemStack.EMPTY);
			if (slotStack.isEmpty()) {
				return ItemStack.EMPTY;
			}

			int count = Math.min(invocation.getArgument(1), slotStack.getCount());
			if (!invocation.getArgument(2, Boolean.class)) {
				slotStack.shrink(count);
				if (slotStack.isEmpty()) {
					slotStacks.put(slot, ItemStack.EMPTY);
				}
			}
			return slotStack.copyWithCount(count);
		});
		when(inventoryHandler.insertItem(any(ItemStack.class), anyBoolean())).thenAnswer(invocation -> {
			ItemStack stack = invocation.getArgument(0);
			Integer slot = insertSlots.get(stack.getItem());
			if (slot == null) {
				return stack;
			}

			if (!invocation.getArgument(1, Boolean.class)) {
				ItemStack slotStack = slotStacks.getOrDefault(slot, ItemStack.EMPTY);
				slotStacks.put(slot, slotStack.isEmpty() ? stack.copy() : slotStack.copyWithCount(slotStack.getCount() + stack.getCount()));
				insertions.computeIfAbsent(stack.getItem(), item -> new AtomicInteger()).incrementAndGet();
				int currentDepth = insertDepth.incrementAndGet();
				maxInsertDepth.updateAndGet(depth -> Math.max(depth, currentDepth));
				wrapper.onAfterInsert(inventoryHandler, slot);
				insertDepth.decrementAndGet();
			}
			return ItemStack.EMPTY;
		});

		return new ReentrantCompactingInventory(inventoryHandler, slotStacks, insertions, maxInsertDepth);
	}

	private record ReentrantCompactingInventory(IItemHandlerSimpleInserter handler, Map<Integer, ItemStack> slotStacks, Map<Item, AtomicInteger> insertions,
			AtomicInteger maxInsertDepth) {
		int getInsertions(Item item) {
			return insertions.getOrDefault(item, new AtomicInteger()).get();
		}

		int getCount(int slot) {
			return slotStacks.getOrDefault(slot, ItemStack.EMPTY).getCount();
		}
	}

	private static CompactingUpgradeItem getUpgradeItemWithConfiguredShape(boolean shouldCompactThreeByThree, int shapeSize, int count) {
		CompactingUpgradeItem upgradeItem = mock(CompactingUpgradeItem.class);
		when(upgradeItem.shouldCompactThreeByThree()).thenReturn(shouldCompactThreeByThree);
		when(upgradeItem.getConfiguredCompactingResult(any(ItemStack.class), anyInt(), anyInt())).thenReturn(Optional.empty());
		when(upgradeItem.getConfiguredCompactingResult(any(ItemStack.class), eq(shapeSize), eq(shapeSize))).thenReturn(getConfiguredCompactingResult(count));
		return upgradeItem;
	}

	private static Optional<CompactingUpgradeConfig.CompactingDefinition> getConfiguredCompactingResult(int count) {
		return Optional.of(getConfiguredCompactingDefinition(new ItemStack(Items.COAL), count));
	}

	private static CompactingUpgradeConfig.CompactingDefinition getConfiguredCompactingDefinition(ItemStack result, int count) {
		return new CompactingUpgradeConfig.CompactingDefinition(new RecipeHelper.CompactingResult(result, Collections.emptyList()), count);
	}

	@SuppressWarnings("unchecked")
	private static <T> Supplier<? extends DataComponentType<? extends T>> anyDataComponentSupplier() {
		return (Supplier<? extends DataComponentType<? extends T>>) any(Supplier.class);
	}

	@SuppressWarnings("unchecked")
	private static <T> Supplier<? extends DataComponentType<T>> anySetDataComponentSupplier() {
		return (Supplier<? extends DataComponentType<T>>) any(Supplier.class);
	}
}
