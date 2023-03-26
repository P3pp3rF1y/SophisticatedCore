package net.p3pp3rf1y.sophisticatedcore.util;

import com.google.common.collect.ImmutableMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nonnull;
import java.util.Map;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;

class InventoryHelperTest {

	@BeforeAll
	public static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private IItemHandler getItemHandler(NonNullList<ItemStack> stacks, int stackLimitMultiplier) {
		IItemHandler handler = new ItemStackHandler(stacks) {
			@Override
			public int getSlotLimit(int slot) {
				return super.getSlotLimit(slot) * stackLimitMultiplier;
			}

			@Override
			protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
				return super.getStackLimit(slot, stack) * stackLimitMultiplier;
			}
		};
		return handler;
	}

	@ParameterizedTest
	@MethodSource("transferMovesStacksCorrectly")
	void transferMovesStacksCorrectly(NonNullList<ItemStack> stacksHandlerA, int limitMultiplierA, NonNullList<ItemStack> stacksHandlerB, int limitMultiplierB, Map<Integer, ItemStack> stacksAfterTransferA, Map<Integer, ItemStack> stacksAfterTransferB) {
		IItemHandler handlerA = getItemHandler(stacksHandlerA, limitMultiplierA);
		IItemHandler handlerB = getItemHandler(stacksHandlerB, limitMultiplierB);

		InventoryHelper.transfer(handlerA, handlerB, s -> {}, s -> true);

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
		};
	}

	private static NonNullList<ItemStack> stacks(ItemStack... stacks) {
		return NonNullList.of(ItemStack.EMPTY, stacks);
	}
}