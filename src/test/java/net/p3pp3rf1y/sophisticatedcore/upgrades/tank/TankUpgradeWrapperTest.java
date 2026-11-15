package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class TankUpgradeWrapperTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		Items.STICK.builtInRegistryHolder().bindComponents(components);
		Items.DIRT.builtInRegistryHolder().bindComponents(components);
		Items.STONE.builtInRegistryHolder().bindComponents(components);
		Items.EXPERIENCE_BOTTLE.builtInRegistryHolder().bindComponents(components);
		Items.GLASS_BOTTLE.builtInRegistryHolder().bindComponents(components);
		Items.POTION.builtInRegistryHolder().bindComponents(components);
		Items.BUCKET.builtInRegistryHolder().bindComponents(components);
		Items.WATER_BUCKET.builtInRegistryHolder().bindComponents(components);
		Items.ENDER_PEARL.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 16).build());
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@Test
	void insertRollbacksAndNestedCommitsDoNotSaveOrRender() {
		List<ItemStack> saves = new ArrayList<>();
		List<RenderData.TankRenderData> renders = new ArrayList<>();
		TankUpgradeWrapper wrapper = createWrapper(new ItemStack(Items.STICK), saves, renders);
		FluidResource water = FluidResource.of(new FluidStack(Fluids.WATER, 1000));

		try (Transaction outer = Transaction.openRoot()) {
			assertEquals(500, wrapper.insert(water, 500, outer, true));
			try (Transaction nested = Transaction.open(outer)) {
				assertEquals(250, wrapper.insert(water, 250, nested, true));
				nested.commit();
			}
			assertEquals(750, wrapper.getAmount());
			assertTrue(saves.isEmpty());
			assertTrue(renders.isEmpty());
		}

		assertEquals(0, wrapper.getAmount());
		assertTrue(saves.isEmpty());
		assertTrue(renders.isEmpty());
	}

	@Test
	void insertRootCommitSavesOnceWithFinalFluidAndNestedRollback() {
		List<ItemStack> saves = new ArrayList<>();
		List<RenderData.TankRenderData> renders = new ArrayList<>();
		TankUpgradeWrapper wrapper = createWrapper(new ItemStack(Items.STICK), saves, renders);
		FluidResource water = FluidResource.of(new FluidStack(Fluids.WATER, 1000));

		try (Transaction outer = Transaction.openRoot()) {
			wrapper.insert(water, 500, outer, true);
			try (Transaction nested = Transaction.open(outer)) {
				wrapper.extract(water, 200, nested, true);
			}
			assertEquals(500, wrapper.getAmount());
			assertTrue(saves.isEmpty());
			outer.commit();
		}

		assertEquals(1, saves.size());
		assertEquals(1, renders.size());
		assertEquals(500, TankUpgradeWrapper.getContents(wrapper.getUpgradeStack()).getAmount());
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

		createWrapper(upgrade, saves, new ArrayList<>());

		assertEquals(1, saves.size());
		assertEquals(expected.get(DataComponents.CONTAINER), saves.get(saves.size() - 1).get(ModCoreDataComponents.LENIENT_CONTAINER));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1})
	void migrateLegacyContentsDoesNotSaveWhenNoItemsCanMove(int sourceSlot) {
		ItemStack upgrade = upgradeWithContents(sourceSlot, new ItemStack(Items.DIRT, 4), new ItemStack(Items.DIRT, 64));
		List<ItemStack> saves = new ArrayList<>();

		createWrapper(upgrade, saves, new ArrayList<>());

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
		return createWrapper(upgrade, new ArrayList<>(), new ArrayList<>());
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("transactionalSetterAndIndexedTransferCases")
	void setStackInSlotWithTransactionThenIndexedTransferKeepsRollbackAndCommitState(boolean insert, TransactionOutcome outcome) {
		List<ItemStack> saves = new ArrayList<>();
		List<RenderData.TankRenderData> renders = new ArrayList<>();
		TankUpgradeWrapper wrapper = createWrapper(new ItemStack(Items.STICK), saves, renders);
		ItemContainerContents originalContents = wrapper.getUpgradeStack().getOrDefault(ModCoreDataComponents.LENIENT_CONTAINER, ItemContainerContents.EMPTY);

		if (outcome == TransactionOutcome.ROOT_ABORT) {
			try (Transaction tx = Transaction.openRoot()) {
				setThenTransfer(wrapper, insert, tx);
			}
		} else if (outcome == TransactionOutcome.ROOT_COMMIT) {
			try (Transaction tx = Transaction.openRoot()) {
				setThenTransfer(wrapper, insert, tx);
				tx.commit();
			}
		} else {
			try (Transaction outer = Transaction.openRoot()) {
				try (Transaction nested = Transaction.open(outer)) {
					setThenTransfer(wrapper, insert, nested);
					if (outcome == TransactionOutcome.NESTED_COMMIT_ROOT_ABORT) {
						nested.commit();
					}
				}
				if (outcome == TransactionOutcome.NESTED_ABORT_OUTER_COMMIT) {
					outer.commit();
				}
			}
		}

		if (outcome == TransactionOutcome.ROOT_COMMIT) {
			int expectedCount = insert ? 5 : 1;
			assertEquals(expectedCount, wrapper.getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT).getCount());
			assertEquals(1, saves.size());
			assertEquals(expectedCount, createWrapper(saves.getLast()).getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT).getCount());
		} else {
			assertTrue(wrapper.getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT).isEmpty());
			assertEquals(originalContents, wrapper.getUpgradeStack().getOrDefault(ModCoreDataComponents.LENIENT_CONTAINER, ItemContainerContents.EMPTY));
			assertTrue(saves.isEmpty());
		}
		assertTrue(renders.isEmpty());
	}

	private static Stream<Arguments> transactionalSetterAndIndexedTransferCases() {
		return Stream.of(Arguments.of(true, TransactionOutcome.ROOT_ABORT), Arguments.of(false, TransactionOutcome.ROOT_ABORT),
				Arguments.of(true, TransactionOutcome.NESTED_ABORT_OUTER_COMMIT), Arguments.of(false, TransactionOutcome.NESTED_ABORT_OUTER_COMMIT),
				Arguments.of(true, TransactionOutcome.NESTED_COMMIT_ROOT_ABORT), Arguments.of(false, TransactionOutcome.NESTED_COMMIT_ROOT_ABORT),
				Arguments.of(true, TransactionOutcome.ROOT_COMMIT), Arguments.of(false, TransactionOutcome.ROOT_COMMIT));
	}

	private static void setThenTransfer(TankUpgradeWrapper wrapper, boolean insert, Transaction tx) {
		ItemResource dirt = ItemResource.of(new ItemStack(Items.DIRT));
		wrapper.getInventory().setStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT, new ItemStack(Items.DIRT, 3), tx);
		if (insert) {
			wrapper.getInventory().insert(TankUpgradeWrapper.INPUT_RESULT_SLOT, dirt, 2, tx);
		} else {
			wrapper.getInventory().extract(TankUpgradeWrapper.INPUT_RESULT_SLOT, dirt, 2, tx);
		}
	}

	private enum TransactionOutcome {
		ROOT_ABORT, NESTED_ABORT_OUTER_COMMIT, NESTED_COMMIT_ROOT_ABORT, ROOT_COMMIT
	}

	@Test
	void setPersistsDirectItemWritesAndRetainsSuperclassValidation() {
		List<ItemStack> saves = new ArrayList<>();
		TankUpgradeWrapper wrapper = createWrapper(new ItemStack(Items.STICK), saves, new ArrayList<>());
		ItemResource dirt = ItemResource.of(new ItemStack(Items.DIRT));

		wrapper.getInventory().set(TankUpgradeWrapper.INPUT_RESULT_SLOT, dirt, 2);
		wrapper.getInventory().set(TankUpgradeWrapper.INPUT_RESULT_SLOT, dirt, 2);

		assertEquals(1, saves.size());
		assertEquals(2, createWrapper(saves.getLast()).getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT).getCount());
		assertThrows(IllegalArgumentException.class, () -> wrapper.getInventory().set(TankUpgradeWrapper.INPUT_RESULT_SLOT, ItemResource.EMPTY, 1));
		assertThrows(IllegalArgumentException.class, () -> wrapper.getInventory().set(TankUpgradeWrapper.INPUT_RESULT_SLOT, dirt, -1));
		assertEquals(1, saves.size());
	}

	@Test
	void mixedFluidAndItemCommitPersistsOnceAndReconstructs() {
		List<ItemStack> saves = new ArrayList<>();
		TankUpgradeWrapper wrapper = createWrapper(new ItemStack(Items.STICK), saves, new ArrayList<>());
		FluidResource water = FluidResource.of(new FluidStack(Fluids.WATER, 1000));

		try (Transaction tx = Transaction.openRoot()) {
			wrapper.insert(water, 1000, tx, true);
			wrapper.getInventory().insert(TankUpgradeWrapper.INPUT_RESULT_SLOT, ItemResource.of(new ItemStack(Items.DIRT)), 2, tx);
			tx.commit();
		}

		assertEquals(1, saves.size());
		TankUpgradeWrapper reconstructed = createWrapper(saves.getLast());
		assertEquals(1000, reconstructed.getAmount());
		assertTrue(ItemStack.matches(new ItemStack(Items.DIRT, 2), reconstructed.getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT)));
	}

	@Test
	void nestedItemCommitRollsBackWithRootFluidAbort() {
		TankUpgradeWrapper wrapper = createWrapper(new ItemStack(Items.STICK));
		FluidResource water = FluidResource.of(new FluidStack(Fluids.WATER, 1000));

		try (Transaction outer = Transaction.openRoot()) {
			wrapper.insert(water, 1000, outer, true);
			try (Transaction nested = Transaction.open(outer)) {
				wrapper.getInventory().insert(TankUpgradeWrapper.INPUT_RESULT_SLOT, ItemResource.of(new ItemStack(Items.DIRT)), 1, nested);
				nested.commit();
			}
		}

		assertEquals(0, wrapper.getAmount());
		assertTrue(wrapper.getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT).isEmpty());
	}

	@Test
	void nestedItemAbortDoesNotPreventOuterFluidCommit() {
		List<ItemStack> saves = new ArrayList<>();
		TankUpgradeWrapper wrapper = createWrapper(new ItemStack(Items.STICK), saves, new ArrayList<>());
		FluidResource water = FluidResource.of(new FluidStack(Fluids.WATER, 1000));

		try (Transaction outer = Transaction.openRoot()) {
			wrapper.insert(water, 1000, outer, true);
			try (Transaction nested = Transaction.open(outer)) {
				wrapper.getInventory().insert(TankUpgradeWrapper.INPUT_RESULT_SLOT, ItemResource.of(new ItemStack(Items.DIRT)), 1, nested);
			}
			outer.commit();
		}

		assertEquals(1, saves.size());
		assertEquals(1000, wrapper.getAmount());
		assertTrue(wrapper.getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT).isEmpty());
	}

	@Test
	void directItemWritesSaveOnlyChanges() {
		List<ItemStack> saves = new ArrayList<>();
		TankUpgradeWrapper wrapper = createWrapper(new ItemStack(Items.STICK), saves, new ArrayList<>());
		ItemStack dirt = new ItemStack(Items.DIRT);

		wrapper.getInventory().setStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT, dirt);
		wrapper.getInventory().setStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT, dirt);

		assertEquals(1, saves.size());
	}

	@Test
	void waterBucketProcessingConsumesOnceAndCreatesOneResult() {
		TankUpgradeWrapper wrapper = createWrapper(new ItemStack(Items.STICK));
		wrapper.getInventory().setStackInSlot(TankUpgradeWrapper.INPUT_SLOT, new ItemStack(Items.WATER_BUCKET));

		assertTrue(wrapper.getContainerHandler().processContainers());
		assertEquals(1000, wrapper.getAmount());
		assertTrue(wrapper.getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_SLOT).isEmpty());
		assertTrue(ItemStack.matches(new ItemStack(Items.BUCKET), wrapper.getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT)));
		assertTrue(!wrapper.getContainerHandler().processContainers());
		assertEquals(1000, wrapper.getAmount());
		assertTrue(ItemStack.matches(new ItemStack(Items.BUCKET), wrapper.getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT)));
	}

	@Test
	void stackedWaterBucketsWithBlockedResultRollBackWithoutSaving() {
		List<ItemStack> saves = new ArrayList<>();
		TankUpgradeWrapper wrapper = createWrapper(new ItemStack(Items.STICK), saves, new ArrayList<>());
		wrapper.getInventory().setStackInSlot(TankUpgradeWrapper.INPUT_SLOT, new ItemStack(Items.WATER_BUCKET, 2));
		wrapper.getInventory().setStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT, new ItemStack(Items.BUCKET, 64));
		saves.clear();

		assertTrue(!wrapper.getContainerHandler().processContainers());
		assertEquals(0, wrapper.getAmount());
		assertTrue(ItemStack.matches(new ItemStack(Items.WATER_BUCKET, 2), wrapper.getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_SLOT)));
		assertTrue(ItemStack.matches(new ItemStack(Items.BUCKET, 64), wrapper.getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT)));
		assertTrue(saves.isEmpty());
		assertTrue(!wrapper.getContainerHandler().processContainers());
		assertTrue(saves.isEmpty());
	}

	@Test
	void singleWaterBucketWithBlockedResultCommitsFallbackAndReconstructs() {
		List<ItemStack> saves = new ArrayList<>();
		TankUpgradeWrapper wrapper = createWrapper(new ItemStack(Items.STICK), saves, new ArrayList<>());
		wrapper.getInventory().setStackInSlot(TankUpgradeWrapper.INPUT_SLOT, new ItemStack(Items.WATER_BUCKET));
		wrapper.getInventory().setStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT, new ItemStack(Items.BUCKET, 64));
		saves.clear();

		assertTrue(wrapper.getContainerHandler().processContainers());
		assertEquals(1000, wrapper.getAmount());
		assertTrue(ItemStack.matches(new ItemStack(Items.BUCKET), wrapper.getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_SLOT)));
		assertTrue(ItemStack.matches(new ItemStack(Items.BUCKET, 64), wrapper.getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT)));
		assertEquals(1, saves.size());
		TankUpgradeWrapper reconstructed = createWrapper(saves.getLast());
		assertEquals(1000, reconstructed.getAmount());
		assertTrue(ItemStack.matches(new ItemStack(Items.BUCKET), reconstructed.getInventory().getStackInSlot(TankUpgradeWrapper.INPUT_SLOT)));
		assertTrue(!reconstructed.getContainerHandler().processContainers());
	}

	@Test
	void itemWritesPreserveTrailingContainerContents() {
		ItemStack upgrade = new ItemStack(Items.STICK);
		upgrade.set(ModCoreDataComponents.LENIENT_CONTAINER,
				ItemContainerContents.fromItems(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.STICK, 7))));
		List<ItemStack> saves = new ArrayList<>();
		TankUpgradeWrapper wrapper = createWrapper(upgrade, saves, new ArrayList<>());

		wrapper.getInventory().setStackInSlot(TankUpgradeWrapper.INPUT_RESULT_SLOT, new ItemStack(Items.DIRT));

		NonNullList<ItemStack> persisted = NonNullList.withSize(5, ItemStack.EMPTY);
		saves.getLast().get(ModCoreDataComponents.LENIENT_CONTAINER).copyInto(persisted);
		assertTrue(ItemStack.matches(new ItemStack(Items.STICK, 7), persisted.get(4)));
	}

	private static TankUpgradeWrapper createWrapper(ItemStack upgrade, List<ItemStack> saves, List<RenderData.TankRenderData> renders) {
		TankUpgradeItem item = mock(TankUpgradeItem.class);
		when(item.getTankCapacity(org.mockito.ArgumentMatchers.any())).thenReturn(4000);
		ItemStack realStack = new ItemStack(Items.STICK);
		ItemStack tankStack = mock(ItemStack.class, delegatesTo(realStack));
		when(tankStack.getItem()).thenReturn(item);
		if (upgrade.has(DataComponents.CONTAINER)) {
			tankStack.set(DataComponents.CONTAINER, upgrade.get(DataComponents.CONTAINER));
		}
		if (upgrade.has(ModCoreDataComponents.LENIENT_CONTAINER)) {
			tankStack.set(ModCoreDataComponents.LENIENT_CONTAINER, upgrade.get(ModCoreDataComponents.LENIENT_CONTAINER));
		}
		if (upgrade.has(ModCoreDataComponents.FLUID_CONTENTS)) {
			tankStack.set(ModCoreDataComponents.FLUID_CONTENTS, upgrade.get(ModCoreDataComponents.FLUID_CONTENTS));
		}
		ItemAccess realAccess = ItemAccess.forStack(realStack);
		try (MockedStatic<ItemAccess> accesses = mockStatic(ItemAccess.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			accesses.when(() -> ItemAccess.forStack(tankStack)).thenReturn(realAccess);
			TankUpgradeWrapper wrapper = new TankUpgradeWrapper(mock(IStorageWrapper.class), tankStack, saves::add);
			wrapper.setTankRenderDataUpdateCallback(renders::add);
			return wrapper;
		}
	}
}
