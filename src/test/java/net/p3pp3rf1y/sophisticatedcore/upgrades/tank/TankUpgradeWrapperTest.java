package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TankUpgradeWrapperTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1})
	void migrateLegacyContentsMovesTheStackIntoAnEmptyResult(int sourceSlot) {
		ItemStack upgrade = upgradeWithContents(sourceSlot, new ItemStack(Items.DIRT, 4), ItemStack.EMPTY);

		TankUpgradeWrapper wrapper = createWrapper(upgrade);

		assertTrue(wrapper.getInventory().getStackInSlot(sourceSlot).isEmpty());
		assertTrue(ItemStack.matches(new ItemStack(Items.DIRT, 4), wrapper.getInventory().getStackInSlot(sourceSlot + 2)));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1})
	void migrateLegacyContentsMergesMatchingResultStacks(int sourceSlot) {
		ItemStack upgrade = upgradeWithContents(sourceSlot, new ItemStack(Items.DIRT, 4), new ItemStack(Items.DIRT, 60));

		TankUpgradeWrapper wrapper = createWrapper(upgrade);

		assertTrue(wrapper.getInventory().getStackInSlot(sourceSlot).isEmpty());
		assertTrue(ItemStack.matches(new ItemStack(Items.DIRT, 64), wrapper.getInventory().getStackInSlot(sourceSlot + 2)));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1})
	void migrateLegacyContentsRetainsTheRemainderWhenTheResultHasOneFreeSpace(int sourceSlot) {
		ItemStack upgrade = upgradeWithContents(sourceSlot, new ItemStack(Items.DIRT, 4), new ItemStack(Items.DIRT, 63));

		TankUpgradeWrapper wrapper = createWrapper(upgrade);

		assertTrue(ItemStack.matches(new ItemStack(Items.DIRT, 3), wrapper.getInventory().getStackInSlot(sourceSlot)));
		assertTrue(ItemStack.matches(new ItemStack(Items.DIRT, 64), wrapper.getInventory().getStackInSlot(sourceSlot + 2)));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1})
	void migrateLegacyContentsLeavesStacksUnchangedWhenTheResultIsFull(int sourceSlot) {
		ItemStack source = new ItemStack(Items.DIRT, 4);
		ItemStack result = new ItemStack(Items.DIRT, 64);
		ItemStack upgrade = upgradeWithContents(sourceSlot, source, result);

		TankUpgradeWrapper wrapper = createWrapper(upgrade);

		assertTrue(ItemStack.matches(source, wrapper.getInventory().getStackInSlot(sourceSlot)));
		assertTrue(ItemStack.matches(result, wrapper.getInventory().getStackInSlot(sourceSlot + 2)));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1})
	void migrateLegacyContentsDoesNotReplaceADifferentResultItem(int sourceSlot) {
		ItemStack source = new ItemStack(Items.DIRT, 4);
		ItemStack result = new ItemStack(Items.STICK, 12);
		ItemStack upgrade = upgradeWithContents(sourceSlot, source, result);

		TankUpgradeWrapper wrapper = createWrapper(upgrade);

		assertTrue(ItemStack.matches(source, wrapper.getInventory().getStackInSlot(sourceSlot)));
		assertTrue(ItemStack.matches(result, wrapper.getInventory().getStackInSlot(sourceSlot + 2)));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1})
	void migrateLegacyContentsDoesNotMergeDifferentItemComponents(int sourceSlot) {
		ItemStack source = new ItemStack(Items.DIRT, 4);
		source.set(DataComponents.CUSTOM_NAME, Component.literal("Source"));
		ItemStack result = new ItemStack(Items.DIRT, 60);
		result.set(DataComponents.CUSTOM_NAME, Component.literal("Destination"));
		ItemStack upgrade = upgradeWithContents(sourceSlot, source, result);

		TankUpgradeWrapper wrapper = createWrapper(upgrade);

		assertTrue(ItemStack.matches(source, wrapper.getInventory().getStackInSlot(sourceSlot)));
		assertTrue(ItemStack.matches(result, wrapper.getInventory().getStackInSlot(sourceSlot + 2)));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1})
	void migrateLegacyContentsPreservesComponentsWhenMergingMatchingStacks(int sourceSlot) {
		ItemStack source = new ItemStack(Items.DIRT, 4);
		source.set(DataComponents.CUSTOM_NAME, Component.literal("Source"));
		ItemStack upgrade = upgradeWithContents(sourceSlot, source, source.copyWithCount(60));

		TankUpgradeWrapper wrapper = createWrapper(upgrade);

		assertTrue(wrapper.getInventory().getStackInSlot(sourceSlot).isEmpty());
		assertTrue(ItemStack.matches(source.copyWithCount(64), wrapper.getInventory().getStackInSlot(sourceSlot + 2)));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1})
	void migrateLegacyContentsRespectsTheItemSpecificStackLimit(int sourceSlot) {
		ItemStack upgrade = upgradeWithContents(sourceSlot, new ItemStack(Items.ENDER_PEARL, 4), new ItemStack(Items.ENDER_PEARL, 15));

		TankUpgradeWrapper wrapper = createWrapper(upgrade);

		assertTrue(ItemStack.matches(new ItemStack(Items.ENDER_PEARL, 3), wrapper.getInventory().getStackInSlot(sourceSlot)));
		assertTrue(ItemStack.matches(new ItemStack(Items.ENDER_PEARL, 16), wrapper.getInventory().getStackInSlot(sourceSlot + 2)));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1})
	void migrateLegacyContentsDoesNotChangeTheOtherSlotPair(int sourceSlot) {
		ItemStack upgrade = upgradeWithContents(sourceSlot, new ItemStack(Items.DIRT, 4), ItemStack.EMPTY);
		int otherSourceSlot = 1 - sourceSlot;

		TankUpgradeWrapper wrapper = createWrapper(upgrade);

		assertTrue(wrapper.getInventory().getStackInSlot(otherSourceSlot).isEmpty());
		assertTrue(ItemStack.matches(new ItemStack(Items.STICK, 7), wrapper.getInventory().getStackInSlot(otherSourceSlot + 2)));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1})
	void migrateLegacyContentsPersistsTheMergedResultAndRemainingSource(int sourceSlot) {
		ItemStack upgrade = upgradeWithContents(sourceSlot, new ItemStack(Items.DIRT, 4), new ItemStack(Items.DIRT, 63));
		List<ItemStack> saves = new ArrayList<>();
		ItemStack expected = upgradeWithContents(sourceSlot, new ItemStack(Items.DIRT, 3), new ItemStack(Items.DIRT, 64));

		createWrapper(upgrade, saves);

		assertEquals(2, saves.size());
		assertEquals(expected.get(DataComponents.CONTAINER), saves.get(saves.size() - 1).get(ModCoreDataComponents.LENIENT_CONTAINER));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1})
	void migrateLegacyContentsDoesNotSaveWhenNoItemsCanMove(int sourceSlot) {
		ItemStack upgrade = upgradeWithContents(sourceSlot, new ItemStack(Items.DIRT, 4), new ItemStack(Items.DIRT, 64));
		List<ItemStack> saves = new ArrayList<>();

		createWrapper(upgrade, saves);

		assertTrue(saves.isEmpty());
	}

	private static ItemStack upgradeWithContents(int sourceSlot, ItemStack source, ItemStack result) {
		List<ItemStack> contents = new ArrayList<>(List.of(ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.STICK, 7), new ItemStack(Items.STICK, 7)));
		contents.set(sourceSlot, source);
		contents.set(sourceSlot + 2, result);
		ItemStack upgrade = new ItemStack(Items.STICK);
		upgrade.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
		return upgrade;
	}

	private static TankUpgradeWrapper createWrapper(ItemStack upgrade) {
		return createWrapper(upgrade, new ArrayList<>());
	}

	private static TankUpgradeWrapper createWrapper(ItemStack upgrade, List<ItemStack> saves) {
		ItemStack realStack = new ItemStack(Items.STICK);
		realStack.set(DataComponents.CONTAINER, upgrade.get(DataComponents.CONTAINER));
		ItemStack tankStack = mock(ItemStack.class, delegatesTo(realStack));
		when(tankStack.getItem()).thenReturn(mock(TankUpgradeItem.class));
		return new TankUpgradeWrapper(mock(IStorageWrapper.class), tankStack, stack -> saves.add(stack.copy()));
	}
}
