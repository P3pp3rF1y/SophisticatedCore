package net.p3pp3rf1y.sophisticatedcore.upgrades.compacting;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static net.p3pp3rf1y.sophisticatedcore.HelperAssertions.assertStackEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompactingUpgradeWrapperTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
		bindTestComponents(Items.CHARCOAL, Items.COAL, Items.DIAMOND, Items.DIAMOND_BLOCK, Items.NETHERITE_BLOCK);
	}

	private static void bindTestComponents(Item... items) {
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		for (Item item : items) {
			item.builtInRegistryHolder().bindComponents(components);
		}
	}

	@Test
	void basicUpgradeCompactsConfiguredTwoByTwoShape() {
		ItemStack stack = new ItemStack(Items.CHARCOAL);
		CompactingUpgradeItem upgradeItem = getUpgradeItemWithConfiguredShape(false, 2, 4);

		Optional<CompactingUpgradeWrapper.CompactingDefinition> compactingDefinition = CompactingUpgradeWrapper.getCompactingDefinition(stack, upgradeItem, false);

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

		Optional<CompactingUpgradeWrapper.CompactingDefinition> compactingDefinition = CompactingUpgradeWrapper.getCompactingDefinition(stack, upgradeItem, false);

		verify(upgradeItem).getConfiguredCompactingResult(any(ItemStack.class), eq(2), eq(2));
		assertFalse(compactingDefinition.isPresent(), "Basic compacting upgrade should not compact configured 3x3 shape");
	}

	@Test
	void advancedUpgradeCompactsConfiguredThreeByThreeShape() {
		ItemStack stack = new ItemStack(Items.CHARCOAL);
		CompactingUpgradeItem upgradeItem = getUpgradeItemWithConfiguredShape(true, 3, 8);

		Optional<CompactingUpgradeWrapper.CompactingDefinition> compactingDefinition = CompactingUpgradeWrapper.getCompactingDefinition(stack, upgradeItem, false);

		verify(upgradeItem).getConfiguredCompactingResult(any(ItemStack.class), eq(3), eq(3));
		assertTrue(compactingDefinition.isPresent(), "Advanced compacting upgrade should compact configured 3x3 shape");
		CompactingUpgradeWrapper.CompactingDefinition result = compactingDefinition.orElseThrow();
		assertEquals(8, result.count());
		assertStackEquals(new ItemStack(Items.COAL), result.result().getResult(), "Advanced compacting upgrade should compact configured 3x3 shape");
	}

	@Test
	void onAfterInsertProcessesReentrantInsertCallbacksWithoutNestedCompacting() {
		Map<Item, CompactingUpgradeConfig.CompactingDefinition> compactingDefinitions = Map.of(
				Items.DIAMOND, getConfiguredCompactingDefinition(new ItemStack(Items.DIAMOND_BLOCK), 9),
				Items.DIAMOND_BLOCK, getConfiguredCompactingDefinition(new ItemStack(Items.NETHERITE_BLOCK), 9)
		);
		Map<Integer, ItemStack> initialSlotStacks = Map.of(
				0, new ItemStack(Items.DIAMOND, 91),
				1, ItemStack.EMPTY,
				2, ItemStack.EMPTY
		);
		Map<Item, Integer> insertSlots = Map.of(
				Items.DIAMOND_BLOCK, 1,
				Items.NETHERITE_BLOCK, 2
		);
		CompactingUpgradeWrapper wrapper = getCompactingWrapperWithConfiguredCompacting(compactingDefinitions);
		ReentrantCompactingInventory inventory = getReentrantCompactingInventory(wrapper, initialSlotStacks, insertSlots);

		try (Transaction tx = Transaction.openRoot()) {
			wrapper.onAfterInsert(inventory.handler(), 0, tx);
			tx.commit();
		}

		assertEquals(1, inventory.getCount(0));
		assertEquals(1, inventory.getCount(1));
		assertEquals(1, inventory.getCount(2));
		assertEquals(10, inventory.getInsertions(Items.DIAMOND_BLOCK));
		assertEquals(1, inventory.getInsertions(Items.NETHERITE_BLOCK));
		assertEquals(1, inventory.maxInsertDepth().get(), "Compaction recursively inserted results from insert callbacks");
	}

	private static CompactingUpgradeWrapper getCompactingWrapperWithConfiguredCompacting(Map<Item, CompactingUpgradeConfig.CompactingDefinition> compactingDefinitions) {
		CompactingUpgradeItem upgradeItem = mock(CompactingUpgradeItem.class);
		when(upgradeItem.getFilterSlotCount()).thenReturn(0);
		when(upgradeItem.shouldCompactThreeByThree()).thenReturn(true);
		when(upgradeItem.getConfiguredCompactingResult(any(ItemStack.class), eq(3), eq(3)))
				.thenAnswer(invocation -> Optional.ofNullable(compactingDefinitions.get(invocation.<ItemStack>getArgument(0).getItem())));
		ItemStack upgrade = mock(ItemStack.class);
		when(upgrade.getItem()).thenReturn(upgradeItem);
		when(upgrade.getOrDefault(anyDataComponentSupplier(), any())).thenAnswer(invocation -> invocation.getArgument(1));

		return new CompactingUpgradeWrapper(mock(IStorageWrapper.class), upgrade, stack -> {});
	}

	private static ReentrantCompactingInventory getReentrantCompactingInventory(CompactingUpgradeWrapper wrapper, Map<Integer, ItemStack> initialSlotStacks, Map<Item, Integer> insertSlots) {
		Map<Integer, ItemStack> slotStacks = new HashMap<>();
		initialSlotStacks.forEach((slot, stack) -> slotStacks.put(slot, stack.copy()));
		Map<Item, AtomicInteger> insertions = new HashMap<>();
		InventoryHandler inventoryHandler = mock(InventoryHandler.class);
		AtomicInteger insertDepth = new AtomicInteger(0);
		AtomicInteger maxInsertDepth = new AtomicInteger(0);

		when(inventoryHandler.size()).thenReturn(slotStacks.size());
		when(inventoryHandler.getStackInSlot(anyInt())).thenAnswer(invocation -> slotStacks.getOrDefault(invocation.getArgument(0), ItemStack.EMPTY));
		when(inventoryHandler.extract(any(ItemResource.class), anyInt(), any(TransactionContext.class))).thenAnswer(invocation -> extractFromFirstMatchingSlot(slotStacks, invocation.getArgument(0), invocation.getArgument(1)));
		when(inventoryHandler.extract(anyInt(), any(ItemResource.class), anyInt(), any(TransactionContext.class))).thenAnswer(invocation -> extractFromSlot(slotStacks, invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
		when(inventoryHandler.insert(any(ItemResource.class), anyInt(), any(TransactionContext.class))).thenAnswer(invocation -> {
			ItemResource resource = invocation.getArgument(0);
			int amount = invocation.getArgument(1);
			Integer slot = insertSlots.get(resource.getItem());
			if (slot == null) {
				return 0;
			}

			ItemStack slotStack = slotStacks.getOrDefault(slot, ItemStack.EMPTY);
			slotStacks.put(slot, slotStack.isEmpty() ? resource.toStack(amount) : slotStack.copyWithCount(slotStack.getCount() + amount));
			insertions.computeIfAbsent(resource.getItem(), item -> new AtomicInteger()).incrementAndGet();
			int currentDepth = insertDepth.incrementAndGet();
			maxInsertDepth.updateAndGet(depth -> Math.max(depth, currentDepth));
			wrapper.onAfterInsert(inventoryHandler, slot, invocation.getArgument(2));
			insertDepth.decrementAndGet();
			return amount;
		});
		when(inventoryHandler.insert(anyInt(), any(ItemResource.class), anyInt(), any(TransactionContext.class))).thenAnswer(invocation -> {
			int slot = invocation.getArgument(0);
			ItemResource resource = invocation.getArgument(1);
			int amount = invocation.getArgument(2);
			ItemStack slotStack = slotStacks.getOrDefault(slot, ItemStack.EMPTY);
			slotStacks.put(slot, slotStack.isEmpty() ? resource.toStack(amount) : slotStack.copyWithCount(slotStack.getCount() + amount));
			return amount;
		});

		return new ReentrantCompactingInventory(inventoryHandler, slotStacks, insertions, maxInsertDepth);
	}

	private static int extractFromFirstMatchingSlot(Map<Integer, ItemStack> slotStacks, ItemResource resource, int amount) {
		for (Integer slot : slotStacks.keySet()) {
			int extracted = extractFromSlot(slotStacks, slot, resource, amount);
			if (extracted > 0) {
				return extracted;
			}
		}
		return 0;
	}

	private static int extractFromSlot(Map<Integer, ItemStack> slotStacks, int slot, ItemResource resource, int amount) {
		ItemStack slotStack = slotStacks.getOrDefault(slot, ItemStack.EMPTY);
		if (slotStack.isEmpty() || slotStack.getItem() != resource.getItem()) {
			return 0;
		}

		int count = Math.min(amount, slotStack.getCount());
		slotStack.shrink(count);
		if (slotStack.isEmpty()) {
			slotStacks.put(slot, ItemStack.EMPTY);
		}
		return count;
	}

	private record ReentrantCompactingInventory(
			InventoryHandler handler,
			Map<Integer, ItemStack> slotStacks,
			Map<Item, AtomicInteger> insertions,
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
}
