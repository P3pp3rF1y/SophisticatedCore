package net.p3pp3rf1y.sophisticatedcore.util;

import com.google.common.collect.ImmutableMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;

class InventoryHelperTest {

	@BeforeAll
	public static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private IItemHandler getItemHandler(NonNullList<ItemStack> stacks, int stackLimitMultiplier) {
		return getItemHandler(stacks, stackLimitMultiplier, (slot, stack) -> true);
	}

	private IItemHandler getItemHandler(NonNullList<ItemStack> stacks, int stackLimitMultiplier, BiPredicate<Integer, ItemStack> isStackValidForSlot) {
		return new ItemStackHandler(stacks) {
			@Override
			public int getSlotLimit(int slot) {
				return super.getSlotLimit(slot) * stackLimitMultiplier;
			}

			@Override
			protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
				return super.getStackLimit(slot, stack) * stackLimitMultiplier;
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return isStackValidForSlot.test(slot, stack);
			}
		};
	}

	@ParameterizedTest
	@MethodSource("transferMovesOnlyStacksThatCanGoIntoInventory")
	void transferMovesOnlyStacksThatCanGoIntoInventory(NonNullList<ItemStack> stacksHandlerA, int limitMultiplierA, NonNullList<ItemStack> stacksHandlerB, int limitMultiplierB,
			BiPredicate<Integer, ItemStack> isStackValidInHandlerB, Map<Integer, ItemStack> stacksAfterTransferA, Map<Integer, ItemStack> stacksAfterTransferB) {
		IItemHandler handlerA = getItemHandler(stacksHandlerA, limitMultiplierA);
		IItemHandler handlerB = getItemHandler(stacksHandlerB, limitMultiplierB, isStackValidInHandlerB);

		InventoryHelper.transfer(handlerA, handlerB, s -> {});

		assertHandlerState(handlerA, stacksAfterTransferA);
		assertHandlerState(handlerB, stacksAfterTransferB);
	}

	private static Object[][] transferMovesOnlyStacksThatCanGoIntoInventory() {
		return new Object[][] {
				{
						stacks(new ItemStack(Items.IRON_INGOT, 64), new ItemStack(Items.IRON_INGOT, 64), new ItemStack(Items.GOLD_INGOT, 64)),
						1,
						stacks(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
						1,
						itemMatches(Items.IRON_INGOT),
						Map.of(
								0, ItemStack.EMPTY,
								1, ItemStack.EMPTY,
								2, new ItemStack(Items.GOLD_INGOT, 64)
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 64),
								1, new ItemStack(Items.IRON_INGOT, 64),
								2, ItemStack.EMPTY,
								3, ItemStack.EMPTY
						)
				},
				{
						stacks(new ItemStack(Items.IRON_INGOT, 64), new ItemStack(Items.GOLD_INGOT, 64), new ItemStack(Items.GOLD_INGOT, 64)),
						1,
						stacks(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
						1,
						itemAndSlotMatches(Map.of(0, Items.IRON_INGOT, 1, Items.IRON_SWORD, 2, Items.IRON_SWORD, 3, Items.GOLD_INGOT)),
						Map.of(
								0, ItemStack.EMPTY,
								1, ItemStack.EMPTY,
								2, new ItemStack(Items.GOLD_INGOT, 64)
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 64),
								1, ItemStack.EMPTY,
								2, ItemStack.EMPTY,
								3,  new ItemStack(Items.GOLD_INGOT, 64)
						)
				},
				{
						stacks(new ItemStack(Items.IRON_INGOT, 64), new ItemStack(Items.IRON_INGOT, 64), new ItemStack(Items.GOLD_INGOT, 64), new ItemStack(Items.GOLD_INGOT, 64)),
						1,
						stacks(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
						1,
						itemAndSlotMatches(Map.of(0, Items.IRON_INGOT, 1, Items.IRON_SWORD, 2, Items.IRON_SWORD, 3, Items.GOLD_INGOT)),
						Map.of(
								0, ItemStack.EMPTY,
								1, new ItemStack(Items.IRON_INGOT, 64),
								2, ItemStack.EMPTY,
								3, new ItemStack(Items.GOLD_INGOT, 64)
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 64),
								1, ItemStack.EMPTY,
								2, ItemStack.EMPTY,
								3,  new ItemStack(Items.GOLD_INGOT, 64)
						)
				},
				{
						stacks(new ItemStack(Items.IRON_INGOT, 128), new ItemStack(Items.IRON_INGOT, 97)),
						2,
						stacks(new ItemStack(Items.IRON_INGOT, 63), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
						1,
						itemAndSlotMatches(Map.of(0, Items.IRON_INGOT, 1, Items.IRON_INGOT, 2, Items.IRON_INGOT, 3, Items.GOLD_INGOT, 4, Items.IRON_INGOT)),
						Map.of(
								0, ItemStack.EMPTY,
								1, new ItemStack(Items.IRON_INGOT, 32)
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 64),
								1, new ItemStack(Items.IRON_INGOT, 64),
								2, new ItemStack(Items.IRON_INGOT, 64),
								3, ItemStack.EMPTY,
								4, new ItemStack(Items.IRON_INGOT, 64)
						)
				},
				{
						stacks(new ItemStack(Items.GOLD_BLOCK, 128), new ItemStack(Items.IRON_INGOT, 99)),
						2,
						stacks(ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT, 63), ItemStack.EMPTY, ItemStack.EMPTY),
						1,
						itemAndSlotMatches(Map.of(0, Items.GOLD_INGOT, 1, Items.IRON_INGOT, 2, Items.IRON_INGOT, 3, Items.GOLD_INGOT)),
						Map.of(
								0, new ItemStack(Items.GOLD_BLOCK, 128),
								1, new ItemStack(Items.IRON_INGOT, 34)
						),
						Map.of(
								0, ItemStack.EMPTY,
								1, new ItemStack(Items.IRON_INGOT, 64),
								2, new ItemStack(Items.IRON_INGOT, 64),
								3, ItemStack.EMPTY
						)
				},
		};
	}

	private static BiPredicate<Integer, ItemStack> itemAndSlotMatches(Map<Integer, Item> items) {
		return (slot, st) -> items.containsKey(slot) && items.get(slot) == st.getItem();
	}

	private static BiPredicate<Integer, ItemStack> itemMatches(Item... items) {
		HashSet<Item> itemsSet = new HashSet<>(items.length);
		itemsSet.addAll(Arrays.asList(items));

		return (slot, st) -> itemsSet.contains(st.getItem());
	}

	@ParameterizedTest
	@MethodSource("transferMovesStacksCorrectly")
	void transferMovesStacksCorrectly(NonNullList<ItemStack> stacksHandlerA, int limitMultiplierA, NonNullList<ItemStack> stacksHandlerB, int limitMultiplierB, Map<Integer, ItemStack> stacksAfterTransferA, Map<Integer, ItemStack> stacksAfterTransferB) {
		IItemHandler handlerA = getItemHandler(stacksHandlerA, limitMultiplierA);
		IItemHandler handlerB = getItemHandler(stacksHandlerB, limitMultiplierB);

		InventoryHelper.transfer(handlerA, handlerB, s -> {});

		assertHandlerState(handlerA, stacksAfterTransferA);
		assertHandlerState(handlerB, stacksAfterTransferB);
	}

	private static void assertHandlerState(IItemHandler handler, Map<Integer, ItemStack> expectedStacksInHandler) {
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stackInSlot = handler.getStackInSlot(slot);
			if (expectedStacksInHandler.containsKey(slot)) {
				assertStackEquals(expectedStacksInHandler.get(slot), stackInSlot, "Expected different stack in handler");
			} else if (!stackInSlot.isEmpty()) {
				Assertions.fail("Non empty stack found in slot " + slot + " where there's supposed to be empty");
			}
		}
	}

	private static void assertStackEquals(ItemStack expected, ItemStack actual, Object message) {
		if (!ItemStack.matches(expected, actual)) {
			assertionFailure().message(message)
					.expected(expected)
					.actual(actual)
					.buildAndThrow();
		}
	}

	private static Object[][] transferMovesStacksCorrectly() {
		return new Object[][] {
				{
						stacks(new ItemStack(Items.IRON_INGOT)),
						1,
						stacks(ItemStack.EMPTY),
						1,
						Map.of(
								0, ItemStack.EMPTY
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT)
						)
				},
				{
						stacks(new ItemStack(Items.IRON_INGOT, 32), new ItemStack(Items.GOLD_INGOT, 64)),
						1,
						stacks(new ItemStack(Items.IRON_INGOT, 48), ItemStack.EMPTY, ItemStack.EMPTY),
						1,
						Map.of(
								0, ItemStack.EMPTY,
								1, ItemStack.EMPTY
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 64),
								1, new ItemStack(Items.IRON_INGOT, 16),
								2, new ItemStack(Items.GOLD_INGOT, 64)
						)
				},
				{
						stacks(new ItemStack(Items.IRON_INGOT, 32), new ItemStack(Items.GOLD_INGOT, 64)),
						1,
						stacks(new ItemStack(Items.IRON_INGOT, 64), new ItemStack(Items.GOLD_INGOT, 64), new ItemStack(Items.GOLD_INGOT, 32)),
						1,
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 32),
								1, new ItemStack(Items.GOLD_INGOT, 32)
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 64),
								1, new ItemStack(Items.GOLD_INGOT, 64),
								2, new ItemStack(Items.GOLD_INGOT, 64)
						)
				},
				{
						stacks(new ItemStack(Items.IRON_INGOT, 32), new ItemStack(Items.GOLD_INGOT, 64)),
						1,
						stacks(new ItemStack(Items.IRON_INGOT, 64), new ItemStack(Items.GOLD_INGOT, 64), new ItemStack(Items.GOLD_INGOT, 64)),
						1,
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 32),
								1, new ItemStack(Items.GOLD_INGOT, 64)
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 64),
								1, new ItemStack(Items.GOLD_INGOT, 64),
								2, new ItemStack(Items.GOLD_INGOT, 64)
						)
				},
				{
						stacks(new ItemStack(Items.IRON_BLOCK, 32), new ItemStack(Items.GOLD_BLOCK, 64)),
						1,
						stacks(new ItemStack(Items.IRON_INGOT, 1), new ItemStack(Items.GOLD_INGOT, 1), new ItemStack(Items.GOLD_INGOT, 1)),
						1,
						Map.of(
								0, new ItemStack(Items.IRON_BLOCK, 32),
								1, new ItemStack(Items.GOLD_BLOCK, 64)
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 1),
								1, new ItemStack(Items.GOLD_INGOT, 1),
								2, new ItemStack(Items.GOLD_INGOT, 1)
						)
				},
				{
						stacks(new ItemStack(Items.IRON_INGOT, 64), new ItemStack(Items.IRON_INGOT, 64)),
						1,
						stacks(ItemStack.EMPTY, ItemStack.EMPTY),
						2,
						Map.of(
								0, ItemStack.EMPTY,
								1, ItemStack.EMPTY
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 128),
								1, ItemStack.EMPTY
						)
				},
				{
						stacks(new ItemStack(Items.IRON_INGOT, 128), new ItemStack(Items.IRON_INGOT, 128)),
						2,
						stacks(new ItemStack(Items.IRON_INGOT, 128), ItemStack.EMPTY),
						4,
						Map.of(
								0, ItemStack.EMPTY,
								1, ItemStack.EMPTY
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 256),
								1, new ItemStack(Items.IRON_INGOT, 128)
						)
				},
				{
						stacks(new ItemStack(Items.IRON_INGOT, 1024), ItemStack.EMPTY),
						16,
						stacks(ItemStack.EMPTY),
						1,
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 960),
								1, ItemStack.EMPTY
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 64)
						)
				},
				{
						stacks(new ItemStack(Items.IRON_INGOT, 1024), ItemStack.EMPTY),
						16,
						stacks(ItemStack.EMPTY),
						4,
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 768),
								1, ItemStack.EMPTY
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 256)
						)
				},
				{
						stacks(new ItemStack(Items.IRON_SWORD, 16)),
						16,
						stacks(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
								ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
						1,
						Map.of(
								0, ItemStack.EMPTY
						),
						ImmutableMap.builder()
								.put(0, new ItemStack(Items.IRON_SWORD))
								.put(1, new ItemStack(Items.IRON_SWORD))
								.put(2, new ItemStack(Items.IRON_SWORD))
								.put(3, new ItemStack(Items.IRON_SWORD))
								.put(4, new ItemStack(Items.IRON_SWORD))
								.put(5, new ItemStack(Items.IRON_SWORD))
								.put(6, new ItemStack(Items.IRON_SWORD))
								.put(7, new ItemStack(Items.IRON_SWORD))
								.put(8, new ItemStack(Items.IRON_SWORD))
								.put(9, new ItemStack(Items.IRON_SWORD))
								.put(10, new ItemStack(Items.IRON_SWORD))
								.put(11, new ItemStack(Items.IRON_SWORD))
								.put(12, new ItemStack(Items.IRON_SWORD))
								.put(13, new ItemStack(Items.IRON_SWORD))
								.put(14, new ItemStack(Items.IRON_SWORD))
								.put(15, new ItemStack(Items.IRON_SWORD))
								.build()
				},
				{
						stacks(new ItemStack(Items.IRON_SWORD, 16)),
						16,
						stacks(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
						2,
						Map.of(
								0, new ItemStack(Items.IRON_SWORD, 2)
						),
						ImmutableMap.builder()
								.put(0, new ItemStack(Items.IRON_SWORD, 2))
								.put(1, new ItemStack(Items.IRON_SWORD, 2))
								.put(2, new ItemStack(Items.IRON_SWORD, 2))
								.put(3, new ItemStack(Items.IRON_SWORD, 2))
								.put(4, new ItemStack(Items.IRON_SWORD, 2))
								.put(5, new ItemStack(Items.IRON_SWORD, 2))
								.put(6, new ItemStack(Items.IRON_SWORD, 2))
								.build()
				},
				{
						stacks(new ItemStack(Items.IRON_INGOT, 96)),
						2,
						stacks(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
						1,
						Map.of(
								0, ItemStack.EMPTY
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 64),
								1, new ItemStack(Items.IRON_INGOT, 32),
								2, ItemStack.EMPTY
						)
				},
				{
						stacks(new ItemStack(Items.IRON_INGOT, 256)),
						4,
						stacks(new ItemStack(Items.IRON_INGOT, 63), ItemStack.EMPTY, ItemStack.EMPTY),
						1,
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 127)
						),
						Map.of(
								0, new ItemStack(Items.IRON_INGOT, 64),
								1, new ItemStack(Items.IRON_INGOT, 64),
								2, new ItemStack(Items.IRON_INGOT, 64)
						)
				},
		};
	}

	private static NonNullList<ItemStack> stacks(ItemStack... stacks) {
		return NonNullList.of(ItemStack.EMPTY, stacks);
	}
}