package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IInsertResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IOverflowResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.SlotValueMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.ToIntBiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class InventoryHandlerSlotTrackerStorageOverflowTest {
	static {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		Items.DIAMOND.builtInRegistryHolder().bindComponents(components);
		Items.GOLD_INGOT.builtInRegistryHolder().bindComponents(components);
		Items.ENDER_PEARL.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 16).build());
		Items.DIAMOND_SWORD.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 1).build());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerVoidsWithEmptySlotsMemorizedForAnotherItem(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.memoryData.slotFilterItems().put(1, Items.GOLD_INGOT);
		fixture.memory.reloadFrom(fixture.memoryData);
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationVoidsWithEmptySlotsMemorizedForAnotherItem(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.memoryData.slotFilterItems().put(1, Items.GOLD_INGOT);
		fixture.memory.reloadFrom(fixture.memoryData);
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerVoidsWhenUnreservedEmptySlotsAreDisabled(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.inventory.setShouldInsertIntoEmpty(() -> false);
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationVoidsWhenUnreservedEmptySlotsAreDisabled(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.inventory.setShouldInsertIntoEmpty(() -> false);
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerFillsPartialStackBeforeVoidingRemainder(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 63), ItemStack.EMPTY);
		fixture.memoryData.slotFilterItems().put(1, Items.GOLD_INGOT);
		fixture.memory.reloadFrom(fixture.memoryData);
		ItemStack remainder = mode.insert(fixture, 0, 2);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationCountsPartialStackBeforeVoidingRemainder(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 63), ItemStack.EMPTY);
		fixture.memoryData.slotFilterItems().put(1, Items.GOLD_INGOT);
		fixture.memory.reloadFrom(fixture.memoryData);
		ItemStack remainder = mode.insert(fixture, 0, 2);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(63, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerDoesNotVoidWhileAnotherSlotHasCapacity(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 63), ItemStack.EMPTY);
		fixture.memoryData.slotFilterItems().put(1, Items.DIAMOND);
		fixture.memory.reloadFrom(fixture.memoryData);
		ItemStack remainder = mode.insert(fixture, 0, 2);
		assertEquals(0, remainder.getCount());
		assertEquals(0, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertEquals(1, fixture.inventory.getStackInSlot(1).getCount());
	}

	@Test
	void insertItemIntoHandlerSimulationUsesAnotherSlotWithoutVoidingWhileItHasCapacity() {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 63), ItemStack.EMPTY);
		fixture.memoryData.slotFilterItems().put(1, Items.DIAMOND);
		fixture.memory.reloadFrom(fixture.memoryData);
		ItemStack remainder = InsertionMode.HANDLER_SIMULATE.insert(fixture, 0, 2);
		assertTrue(remainder.isEmpty());
		assertEquals(0, fixture.voided);
		assertEquals(63, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@Test
	void insertItemIntoHandlerSlotSimulationReturnsRemainderWithoutVoidingWhileAnotherSlotHasCapacity() {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 63), ItemStack.EMPTY);
		fixture.memoryData.slotFilterItems().put(1, Items.DIAMOND);
		fixture.memory.reloadFrom(fixture.memoryData);
		ItemStack remainder = InsertionMode.SLOT_SIMULATE.insert(fixture, 0, 2);
		assertEquals(1, remainder.getCount());
		assertEquals(0, fixture.voided);
		assertEquals(63, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerCountsOverlappingMemoryAndPartitionCapacityOnce(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.memoryData.slotFilterItems().put(1, Items.DIAMOND);
		fixture.memory.reloadFrom(fixture.memoryData);
		ItemStack remainder = mode.insert(fixture, 1, 65);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(1).getCount());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationCountsOverlappingMemoryAndPartitionCapacityOnce(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.memoryData.slotFilterItems().put(1, Items.DIAMOND);
		fixture.memory.reloadFrom(fixture.memoryData);
		ItemStack remainder = mode.insert(fixture, 1, 65);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@Test
	void insertItemIntoHandlerFillsBothOrdinaryEmptySlotsBeforeVoidingExcess() {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY, ItemStack.EMPTY);
		ItemStack remainder = InsertionMode.HANDLER_INSERT.insert(fixture, 1, 129);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(1).getCount());
		assertEquals(64, fixture.inventory.getStackInSlot(2).getCount());
		assertEquals(List.of(1, 2), fixture.insertionAttempts);
	}

	@Test
	void insertItemIntoHandlerSimulationCountsBothOrdinaryEmptySlotsBeforeVoidingExcess() {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY, ItemStack.EMPTY);
		ItemStack remainder = InsertionMode.HANDLER_SIMULATE.insert(fixture, 1, 129);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
		assertTrue(fixture.inventory.getStackInSlot(2).isEmpty());
		assertEquals(List.of(1, 2), fixture.insertionAttempts);
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerVoidsWithEmptySlotsFilteredForAnotherItem(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.filter(1, Items.GOLD_INGOT);
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationVoidsWithEmptySlotsFilteredForAnotherItem(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.filter(1, Items.GOLD_INGOT);
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerFillsReservedEmptySlotBeforeVoidingRemainder(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.DIAMOND));
		fixture.inventory.setShouldInsertIntoEmpty(() -> false);
		ItemStack remainder = mode.insert(fixture, 1, 65);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(1).getCount());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationCountsReservedEmptySlotBeforeVoidingRemainder(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.DIAMOND));
		fixture.inventory.setShouldInsertIntoEmpty(() -> false);
		ItemStack remainder = mode.insert(fixture, 1, 65);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerChecksPartitionValidityEvenWithMatchingMemory(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.DIAMOND));
		fixture.partitionValidity = (slot, resource) -> slot != 1;
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationChecksPartitionValidityEvenWithMatchingMemory(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.DIAMOND));
		fixture.partitionValidity = (slot, resource) -> slot != 1;
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerChecksComponentsOfPartitionFilter(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		ItemStack namedDiamond = namedDiamond();
		fixture.filter(1, Items.DIAMOND);
		fixture.partitionValidity = (slot, resource) -> slot != 1 || resource.matches(namedDiamond);
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationChecksComponentsOfPartitionFilter(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		ItemStack namedDiamond = namedDiamond();
		fixture.filter(1, Items.DIAMOND);
		fixture.partitionValidity = (slot, resource) -> slot != 1 || resource.matches(namedDiamond);
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerChecksComponentsOfMemoryFilter(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.memoryData.setIgnoreNbt(false);
		fixture.remember(1, namedDiamond());
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationChecksComponentsOfMemoryFilter(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.memoryData.setIgnoreNbt(false);
		fixture.remember(1, namedDiamond());
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerDoesNotVoidItemAbsentFromStorage(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.GOLD_INGOT, 64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertEquals(1, remainder.getCount());
		assertEquals(0, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationDoesNotVoidItemAbsentFromStorage(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.GOLD_INGOT, 64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertEquals(1, remainder.getCount());
		assertEquals(0, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerPreservesItemsExcludedByVoidFilter(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		fixture.voidMatches = false;
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertEquals(1, remainder.getCount());
		assertEquals(0, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationPreservesItemsExcludedByVoidFilter(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		fixture.voidMatches = false;
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertEquals(1, remainder.getCount());
		assertEquals(0, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@Test
	void insertItemIntoHandlerCountsEachPartialSlotCapacityOnce() {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 63), new ItemStack(Items.DIAMOND, 62));
		fixture.voidMatches = false;
		ItemStack remainder = InsertionMode.HANDLER_INSERT.insert(fixture, 0, 4);
		assertEquals(1, remainder.getCount());
		assertEquals(0, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertEquals(64, fixture.inventory.getStackInSlot(1).getCount());
	}

	@Test
	void insertItemIntoHandlerSimulationCountsEachPartialSlotCapacityOnce() {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 63), new ItemStack(Items.DIAMOND, 62));
		fixture.voidMatches = false;
		ItemStack remainder = InsertionMode.HANDLER_SIMULATE.insert(fixture, 0, 4);
		assertEquals(1, remainder.getCount());
		assertEquals(0, fixture.voided);
		assertEquals(63, fixture.inventory.getStackInSlot(0).getCount());
		assertEquals(62, fixture.inventory.getStackInSlot(1).getCount());
	}

	@ParameterizedTest(name = "incoming count {0}")
	@org.junit.jupiter.params.provider.ValueSource(ints = {127, 128})
	void insertItemIntoHandlerUsesBothOrdinaryEmptySlotsWithoutOverflow(int incomingCount) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY, ItemStack.EMPTY);
		ItemStack remainder = InsertionMode.HANDLER_INSERT.insert(fixture, 1, incomingCount);
		assertTrue(remainder.isEmpty());
		assertEquals(0, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertEquals(64, fixture.inventory.getStackInSlot(1).getCount());
		assertEquals(incomingCount - 64, fixture.inventory.getStackInSlot(2).getCount());
		assertEquals(List.of(1, 2), fixture.insertionAttempts);
	}

	@ParameterizedTest(name = "incoming count {0}")
	@org.junit.jupiter.params.provider.ValueSource(ints = {127, 128})
	void insertItemIntoHandlerSimulationUsesBothOrdinaryEmptySlotsWithoutOverflow(int incomingCount) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY, ItemStack.EMPTY);
		ItemStack remainder = InsertionMode.HANDLER_SIMULATE.insert(fixture, 1, incomingCount);
		assertTrue(remainder.isEmpty());
		assertEquals(0, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
		assertTrue(fixture.inventory.getStackInSlot(2).isEmpty());
		assertEquals(List.of(1, 2), fixture.insertionAttempts);
	}

	@Test
	void insertItemIntoHandlerReturnsExcessAfterFillingBothOrdinaryEmptySlotsWhenVoidFilterRejectsIt() {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY, ItemStack.EMPTY);
		fixture.voidMatches = false;
		ItemStack remainder = InsertionMode.HANDLER_INSERT.insert(fixture, 1, 129);
		assertEquals(1, remainder.getCount());
		assertEquals(0, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(1).getCount());
		assertEquals(64, fixture.inventory.getStackInSlot(2).getCount());
		assertEquals(List.of(1, 2), fixture.insertionAttempts);
	}

	@Test
	void insertItemIntoHandlerSimulationReturnsExcessAfterCountingBothOrdinaryEmptySlotsWhenVoidFilterRejectsIt() {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY, ItemStack.EMPTY);
		fixture.voidMatches = false;
		ItemStack remainder = InsertionMode.HANDLER_SIMULATE.insert(fixture, 1, 129);
		assertEquals(1, remainder.getCount());
		assertEquals(0, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
		assertTrue(fixture.inventory.getStackInSlot(2).isEmpty());
		assertEquals(List.of(1, 2), fixture.insertionAttempts);
	}

	private static ItemStack namedDiamond() {
		ItemStack stack = new ItemStack(Items.DIAMOND);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("reserved"));
		return stack;
	}

	@Test
	void insertItemIntoHandlerPrioritizesPartialThenMemoryThenFilterThenOrdinarySlots() {
		Fixture fixture = mixedReservationInventory();
		ItemStack remainder = InsertionMode.HANDLER_INSERT.insert(fixture, 0, 130);
		assertTrue(remainder.isEmpty());
		assertEquals(0, fixture.voided);
		assertEquals(List.of(0, 3, 2, 1), fixture.insertionAttempts);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertEquals(1, fixture.inventory.getStackInSlot(1).getCount());
		assertEquals(64, fixture.inventory.getStackInSlot(2).getCount());
		assertEquals(64, fixture.inventory.getStackInSlot(3).getCount());
		assertTrue(fixture.inventory.getStackInSlot(4).isEmpty());
		assertTrue(fixture.inventory.getStackInSlot(5).isEmpty());
	}

	@Test
	void insertItemIntoHandlerSimulationPrioritizesPartialThenMemoryThenFilterThenOrdinarySlots() {
		Fixture fixture = mixedReservationInventory();
		ItemStack remainder = InsertionMode.HANDLER_SIMULATE.insert(fixture, 0, 130);
		assertTrue(remainder.isEmpty());
		assertEquals(0, fixture.voided);
		assertEquals(List.of(0, 3, 2, 1), fixture.insertionAttempts);
		assertEquals(63, fixture.inventory.getStackInSlot(0).getCount());
		for (int slot = 1; slot < fixture.inventory.size(); slot++) {
			assertTrue(fixture.inventory.getStackInSlot(slot).isEmpty());
		}
	}

	@Test
	void insertItemIntoHandlerFillsAllMatchingSlotKindsBeforeVoidingExcess() {
		Fixture fixture = mixedReservationInventory();
		ItemStack remainder = InsertionMode.HANDLER_INSERT.insert(fixture, 0, 195);
		assertTrue(remainder.isEmpty());
		assertEquals(2, fixture.voided);
		assertEquals(List.of(0, 3, 2, 1), fixture.insertionAttempts);
		for (int slot = 0; slot < 4; slot++) {
			assertEquals(64, fixture.inventory.getStackInSlot(slot).getCount());
		}
		assertTrue(fixture.inventory.getStackInSlot(4).isEmpty());
		assertTrue(fixture.inventory.getStackInSlot(5).isEmpty());
	}

	@Test
	void insertItemIntoHandlerSimulationCountsAllMatchingSlotKindsBeforeVoidingExcess() {
		Fixture fixture = mixedReservationInventory();
		ItemStack remainder = InsertionMode.HANDLER_SIMULATE.insert(fixture, 0, 195);
		assertTrue(remainder.isEmpty());
		assertEquals(2, fixture.voided);
		assertEquals(List.of(0, 3, 2, 1), fixture.insertionAttempts);
		assertEquals(63, fixture.inventory.getStackInSlot(0).getCount());
		for (int slot = 1; slot < fixture.inventory.size(); slot++) {
			assertTrue(fixture.inventory.getStackInSlot(slot).isEmpty());
		}
	}

	private static Fixture mixedReservationInventory() {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 63), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
		fixture.remember(3, new ItemStack(Items.DIAMOND));
		fixture.remember(4, new ItemStack(Items.GOLD_INGOT));
		fixture.filter(2, Items.DIAMOND);
		fixture.filter(5, Items.GOLD_INGOT);
		return fixture;
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerEnablesOverflowWhenInitiallyAbsentItemFillsFirstStack(InsertionMode mode) {
		Fixture fixture = new Fixture(ItemStack.EMPTY);
		ItemStack remainder = mode.insert(fixture, 0, 65);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationEnablesOverflowWhenInitiallyAbsentItemWouldFillFirstStack(InsertionMode mode) {
		Fixture fixture = new Fixture(ItemStack.EMPTY);
		ItemStack remainder = mode.insert(fixture, 0, 65);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(0).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerDoesNotTriggerStorageOverflowWhileSpaceRemains(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(0, fixture.voided);
		assertEquals(0, fixture.slotVoided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertEquals(1, fixture.inventory.getStackInSlot(1).getCount());
		assertTrue(fixture.overflowCalls.stream().noneMatch("storage"::equals));
	}

	@Test
	void insertItemIntoHandlerSimulationUsesOtherSlotsWithoutTriggeringStorageOverflow() {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		ItemStack remainder = InsertionMode.HANDLER_SIMULATE.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(0, fixture.voided);
		assertEquals(0, fixture.slotVoided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
		assertTrue(fixture.overflowCalls.stream().noneMatch("storage"::equals));
	}

	@Test
	void insertItemIntoHandlerSlotSimulationReturnsRemainderWithoutStorageOverflowWhileAnotherSlotHasSpace() {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		ItemStack remainder = InsertionMode.SLOT_SIMULATE.insert(fixture, 0, 1);
		assertEquals(1, remainder.getCount());
		assertEquals(0, fixture.voided);
		assertEquals(0, fixture.slotVoided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
		assertTrue(fixture.overflowCalls.stream().noneMatch("storage"::equals));
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerTriggersSlotOverflowWhileOtherSlotsHaveSpace(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.slotVoidMatches = true;
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.slotVoided);
		assertEquals(0, fixture.voided);
		assertEquals(List.of("slot"), fixture.overflowCalls);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationTriggersSlotOverflowWhileOtherSlotsHaveSpace(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.slotVoidMatches = true;
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.slotVoided);
		assertEquals(0, fixture.voided);
		assertEquals(List.of("slot"), fixture.overflowCalls);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	private static Fixture fullInventoryWithUnusableEmptySlot() {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		return fixture;
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerTriggersStorageOverflowWhenNoSpaceRemains(InsertionMode mode) {
		Fixture fixture = fullInventoryWithUnusableEmptySlot();
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(0, fixture.slotVoided);
		assertEquals(List.of("storage"), fixture.overflowCalls);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationTriggersStorageOverflowWhenNoSpaceRemains(InsertionMode mode) {
		Fixture fixture = fullInventoryWithUnusableEmptySlot();
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(0, fixture.slotVoided);
		assertEquals(List.of("storage"), fixture.overflowCalls);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerSkipsSlotOverflowWhenStorageOverflowConsumesRemainder(InsertionMode mode) {
		Fixture fixture = fullInventoryWithUnusableEmptySlot();
		fixture.slotVoidMatches = true;
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(0, fixture.slotVoided);
		assertEquals(List.of("storage"), fixture.overflowCalls);
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationSkipsSlotOverflowWhenStorageOverflowConsumesRemainder(InsertionMode mode) {
		Fixture fixture = fullInventoryWithUnusableEmptySlot();
		fixture.slotVoidMatches = true;
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(0, fixture.slotVoided);
		assertEquals(List.of("storage"), fixture.overflowCalls);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerFallsBackToSlotOverflowWhenStorageOverflowRejectsRemainder(InsertionMode mode) {
		Fixture fixture = fullInventoryWithUnusableEmptySlot();
		fixture.voidMatches = false;
		fixture.slotVoidMatches = true;
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(0, fixture.voided);
		assertEquals(1, fixture.slotVoided);
		assertEquals(List.of("storage", "slot"), fixture.overflowCalls);
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationFallsBackToSlotOverflowWhenStorageOverflowRejectsRemainder(InsertionMode mode) {
		Fixture fixture = fullInventoryWithUnusableEmptySlot();
		fixture.voidMatches = false;
		fixture.slotVoidMatches = true;
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(0, fixture.voided);
		assertEquals(1, fixture.slotVoided);
		assertEquals(List.of("storage", "slot"), fixture.overflowCalls);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerReturnsRemainderWhenBothOverflowHandlersRejectIt(InsertionMode mode) {
		Fixture fixture = fullInventoryWithUnusableEmptySlot();
		fixture.voidMatches = false;
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertEquals(1, remainder.getCount());
		assertEquals(0, fixture.voided);
		assertEquals(0, fixture.slotVoided);
		assertEquals("storage", fixture.overflowCalls.getFirst());
		assertTrue(fixture.overflowCalls.contains("slot"));
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationReturnsRemainderWhenBothOverflowHandlersRejectIt(InsertionMode mode) {
		Fixture fixture = fullInventoryWithUnusableEmptySlot();
		fixture.voidMatches = false;
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertEquals(1, remainder.getCount());
		assertEquals(0, fixture.voided);
		assertEquals(0, fixture.slotVoided);
		assertEquals("storage", fixture.overflowCalls.getFirst());
		assertTrue(fixture.overflowCalls.contains("slot"));
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerDoesNotTreatDifferentComponentsAsAnExistingFullStack(InsertionMode mode) {
		ItemStack namedDiamond = namedDiamond();
		Fixture fixture = new Fixture(namedDiamond.copyWithCount(64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertEquals(1, remainder.getCount());
		assertTrue(ItemStack.isSameItemSameComponents(new ItemStack(Items.DIAMOND), remainder));
		assertEquals(0, fixture.voided);
		assertTrue(fixture.overflowCalls.isEmpty());
		assertTrue(ItemStack.isSameItemSameComponents(namedDiamond, fixture.inventory.getStackInSlot(0)));
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationDoesNotTreatDifferentComponentsAsAnExistingFullStack(InsertionMode mode) {
		ItemStack namedDiamond = namedDiamond();
		Fixture fixture = new Fixture(namedDiamond.copyWithCount(64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertEquals(1, remainder.getCount());
		assertTrue(ItemStack.isSameItemSameComponents(new ItemStack(Items.DIAMOND), remainder));
		assertEquals(0, fixture.voided);
		assertTrue(fixture.overflowCalls.isEmpty());
		assertTrue(ItemStack.isSameItemSameComponents(namedDiamond, fixture.inventory.getStackInSlot(0)));
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerRecognizesExistingFullStackWithMatchingComponents(InsertionMode mode) {
		ItemStack namedDiamond = namedDiamond();
		Fixture fixture = new Fixture(namedDiamond.copyWithCount(64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		ItemStack remainder = mode.insert(fixture, 1, namedDiamond);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationRecognizesExistingFullStackWithMatchingComponents(InsertionMode mode) {
		ItemStack namedDiamond = namedDiamond();
		Fixture fixture = new Fixture(namedDiamond.copyWithCount(64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		ItemStack remainder = mode.insert(fixture, 1, namedDiamond);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerDoesNotUseMatchingMemorySlotRejectedByPartitionFilterWhileLocked(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.inventory.setShouldInsertIntoEmpty(() -> false);
		fixture.remember(1, new ItemStack(Items.DIAMOND));
		fixture.filter(1, Items.GOLD_INGOT);
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationDoesNotCountMatchingMemorySlotRejectedByPartitionFilterWhileLocked(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.inventory.setShouldInsertIntoEmpty(() -> false);
		fixture.remember(1, new ItemStack(Items.DIAMOND));
		fixture.filter(1, Items.GOLD_INGOT);
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerDoesNotUseMatchingPartitionSlotRejectedByMemoryWhileLocked(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.inventory.setShouldInsertIntoEmpty(() -> false);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		fixture.filter(1, Items.DIAMOND);
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationDoesNotCountMatchingPartitionSlotRejectedByMemoryWhileLocked(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.inventory.setShouldInsertIntoEmpty(() -> false);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		fixture.filter(1, Items.DIAMOND);
		ItemStack remainder = mode.insert(fixture, 1, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerFillsExactComponentMemoryReservationWhileLocked(InsertionMode mode) {
		ItemStack namedDiamond = namedDiamond();
		Fixture fixture = new Fixture(namedDiamond.copyWithCount(64), ItemStack.EMPTY);
		fixture.inventory.setShouldInsertIntoEmpty(() -> false);
		fixture.memoryData.setIgnoreNbt(false);
		fixture.remember(1, namedDiamond);
		ItemStack remainder = mode.insert(fixture, 1, namedDiamond.copyWithCount(65));
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(1).getCount());
		assertTrue(ItemStack.isSameItemSameComponents(namedDiamond, fixture.inventory.getStackInSlot(1)));
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationCountsExactComponentMemoryReservationWhileLocked(InsertionMode mode) {
		ItemStack namedDiamond = namedDiamond();
		Fixture fixture = new Fixture(namedDiamond.copyWithCount(64), ItemStack.EMPTY);
		fixture.inventory.setShouldInsertIntoEmpty(() -> false);
		fixture.memoryData.setIgnoreNbt(false);
		fixture.remember(1, namedDiamond);
		ItemStack remainder = mode.insert(fixture, 1, namedDiamond.copyWithCount(65));
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("insertionCapacityCases")
	void insertItemIntoHandlerStoresItemsBelowCapacity(CapacityCase params) {
		Fixture fixture = new Fixture(ItemStack.EMPTY);
		fixture.stackLimit = (slot, stack) -> params.capacity;
		int incomingCount = params.capacity - 1;
		ItemStack remainder = params.mode.insert(fixture, 0, new ItemStack(params.item, incomingCount));
		assertTrue(remainder.isEmpty());
		assertEquals(incomingCount, fixture.inventory.getStackInSlot(0).getCount());
		assertEquals(0, fixture.voided);
		assertTrue(fixture.overflowCalls.isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("insertionCapacityCases")
	void insertItemIntoHandlerFillsSlotExactlyToCapacity(CapacityCase params) {
		Fixture fixture = new Fixture(ItemStack.EMPTY);
		fixture.stackLimit = (slot, stack) -> params.capacity;
		ItemStack input = new ItemStack(params.item, params.capacity);
		ItemStack remainder = params.mode.insert(fixture, 0, input);
		assertTrue(remainder.isEmpty());
		assertEquals(params.capacity, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(ItemStack.isSameItemSameComponents(input, fixture.inventory.getStackInSlot(0)));
		assertEquals(0, fixture.voided);
		assertTrue(fixture.overflowCalls.isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("insertionCapacityCases")
	void insertItemIntoHandlerReturnsExcessAboveCapacityWhenVoidFilterRejectsIt(CapacityCase params) {
		Fixture fixture = new Fixture(ItemStack.EMPTY);
		fixture.stackLimit = (slot, stack) -> params.capacity;
		fixture.voidMatches = false;
		ItemStack input = new ItemStack(params.item, params.capacity + 1);
		ItemStack remainder = params.mode.insert(fixture, 0, input);
		assertEquals(1, remainder.getCount());
		assertTrue(ItemStack.isSameItemSameComponents(input, remainder));
		assertEquals(params.capacity, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(ItemStack.isSameItemSameComponents(input, fixture.inventory.getStackInSlot(0)));
		assertEquals(0, fixture.voided);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("insertionCapacityCases")
	void insertItemIntoHandlerVoidsExcessAboveCapacityWhenVoidFilterMatches(CapacityCase params) {
		Fixture fixture = new Fixture(ItemStack.EMPTY);
		fixture.stackLimit = (slot, stack) -> params.capacity;
		ItemStack input = new ItemStack(params.item, params.capacity + 1);
		ItemStack remainder = params.mode.insert(fixture, 0, input);
		assertTrue(remainder.isEmpty());
		assertEquals(params.capacity, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(ItemStack.isSameItemSameComponents(input, fixture.inventory.getStackInSlot(0)));
		assertEquals(1, fixture.voided);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("simulationCapacityCases")
	void insertItemIntoHandlerSimulationAcceptsItemsBelowCapacityWithoutChangingContents(CapacityCase params) {
		Fixture fixture = new Fixture(ItemStack.EMPTY);
		fixture.stackLimit = (slot, stack) -> params.capacity;
		ItemStack remainder = params.mode.insert(fixture, 0, new ItemStack(params.item, params.capacity - 1));
		assertTrue(remainder.isEmpty());
		assertTrue(fixture.inventory.getStackInSlot(0).isEmpty());
		assertEquals(0, fixture.voided);
		assertTrue(fixture.overflowCalls.isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("simulationCapacityCases")
	void insertItemIntoHandlerSimulationAcceptsItemsAtCapacityWithoutChangingContents(CapacityCase params) {
		Fixture fixture = new Fixture(ItemStack.EMPTY);
		fixture.stackLimit = (slot, stack) -> params.capacity;
		ItemStack remainder = params.mode.insert(fixture, 0, new ItemStack(params.item, params.capacity));
		assertTrue(remainder.isEmpty());
		assertTrue(fixture.inventory.getStackInSlot(0).isEmpty());
		assertEquals(0, fixture.voided);
		assertTrue(fixture.overflowCalls.isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("simulationCapacityCases")
	void insertItemIntoHandlerSimulationReturnsExcessWhenVoidFilterRejectsIt(CapacityCase params) {
		Fixture fixture = new Fixture(ItemStack.EMPTY);
		fixture.stackLimit = (slot, stack) -> params.capacity;
		fixture.voidMatches = false;
		ItemStack input = new ItemStack(params.item, params.capacity + 1);
		ItemStack remainder = params.mode.insert(fixture, 0, input);
		assertEquals(1, remainder.getCount());
		assertTrue(ItemStack.isSameItemSameComponents(input, remainder));
		assertTrue(fixture.inventory.getStackInSlot(0).isEmpty());
		assertEquals(0, fixture.voided);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("simulationCapacityCases")
	void insertItemIntoHandlerSimulationReportsExcessAsVoidedWhenVoidFilterMatches(CapacityCase params) {
		Fixture fixture = new Fixture(ItemStack.EMPTY);
		fixture.stackLimit = (slot, stack) -> params.capacity;
		ItemStack remainder = params.mode.insert(fixture, 0, new ItemStack(params.item, params.capacity + 1));
		assertTrue(remainder.isEmpty());
		assertTrue(fixture.inventory.getStackInSlot(0).isEmpty());
		assertEquals(1, fixture.voided);
	}

	private record CapacityCase(InsertionMode mode, Item item, int capacity) {
		@Override
		public String toString() {
			return mode + ", " + item + ", slot capacity " + capacity;
		}
	}

	private static Stream<CapacityCase> insertionCapacityCases() {
		return capacityCases(InsertionMode.HANDLER_INSERT, InsertionMode.SLOT_INSERT);
	}

	private static Stream<CapacityCase> simulationCapacityCases() {
		return capacityCases(InsertionMode.HANDLER_SIMULATE, InsertionMode.SLOT_SIMULATE);
	}

	private static Stream<CapacityCase> capacityCases(InsertionMode... modes) {
		return Stream.of(modes)
				.flatMap(mode -> Stream.of(Items.DIAMOND, Items.ENDER_PEARL, Items.DIAMOND_SWORD)
						.flatMap(item -> (item == Items.DIAMOND ? Stream.of(8, 64, 256) : Stream.of(new ItemStack(item).getMaxStackSize()))
								.map(capacity -> new CapacityCase(mode, item, capacity))));
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerDoesNotCountZeroCapacityEmptySlotAsSpace(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.stackLimit = (slot, stack) -> slot == 1 ? 0 : 64;
		fixture.remember(1, new ItemStack(Items.DIAMOND));
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationDoesNotCountZeroCapacityEmptySlotAsSpace(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.stackLimit = (slot, stack) -> slot == 1 ? 0 : 64;
		fixture.remember(1, new ItemStack(Items.DIAMOND));
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerDoesNotCountInaccessibleReservedSlotAsSpace(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.DIAMOND));
		fixture.accessible = slot -> slot != 1;
		fixture.inventory.getSlotTracker().refreshSlotIndexesFrom(fixture.inventory);
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
		assertTrue(fixture.insertionAttempts.isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationDoesNotCountInaccessibleReservedSlotAsSpace(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 64), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.DIAMOND));
		fixture.accessible = slot -> slot != 1;
		fixture.inventory.getSlotTracker().refreshSlotIndexesFrom(fixture.inventory);
		ItemStack remainder = mode.insert(fixture, 0, 1);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertTrue(fixture.insertionAttempts.isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerInsertsAndOverflowsOnlyRemainderFromBeforeInsert(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 63), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		int incomingCount = 4;
		int consumedBeforeInsertion = 2;
		List<Boolean> simulationFlags = new ArrayList<>();
		fixture.beforeInsert = (resource, amount) -> {
			simulationFlags.add(fixture.simulateCall);
			return consumedBeforeInsertion;
		};
		ItemStack remainder = mode.insert(fixture, 0, incomingCount);
		assertTrue(remainder.isEmpty());
		assertEquals(List.of(false), simulationFlags);
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertEquals(List.of(0), fixture.insertionAttempts);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationProcessesOnlyRemainderFromBeforeInsert(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 63), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		int incomingCount = 4;
		int consumedBeforeInsertion = 2;
		List<Boolean> simulationFlags = new ArrayList<>();
		fixture.beforeInsert = (resource, amount) -> {
			simulationFlags.add(fixture.simulateCall);
			return consumedBeforeInsertion;
		};
		ItemStack remainder = mode.insert(fixture, 0, incomingCount);
		assertTrue(remainder.isEmpty());
		assertEquals(List.of(true), simulationFlags);
		assertEquals(1, fixture.voided);
		assertEquals(63, fixture.inventory.getStackInSlot(0).getCount());
		assertEquals(List.of(0), fixture.insertionAttempts);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerSkipsInsertionAndOverflowWhenBeforeInsertConsumesEverything(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 63), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		List<Boolean> simulationFlags = new ArrayList<>();
		fixture.beforeInsert = (resource, amount) -> {
			simulationFlags.add(fixture.simulateCall);
			return amount;
		};
		ItemStack remainder = mode.insert(fixture, 0, 4);
		assertTrue(remainder.isEmpty());
		assertEquals(List.of(false), simulationFlags);
		assertEquals(63, fixture.inventory.getStackInSlot(0).getCount());
		assertEquals(0, fixture.voided);
		assertTrue(fixture.insertionAttempts.isEmpty());
		assertTrue(fixture.overflowCalls.isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationSkipsInsertionAndOverflowWhenBeforeInsertConsumesEverything(InsertionMode mode) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 63), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		List<Boolean> simulationFlags = new ArrayList<>();
		fixture.beforeInsert = (resource, amount) -> {
			simulationFlags.add(fixture.simulateCall);
			return amount;
		};
		ItemStack remainder = mode.insert(fixture, 0, 4);
		assertTrue(remainder.isEmpty());
		assertEquals(List.of(true), simulationFlags);
		assertEquals(63, fixture.inventory.getStackInSlot(0).getCount());
		assertEquals(0, fixture.voided);
		assertTrue(fixture.insertionAttempts.isEmpty());
		assertTrue(fixture.overflowCalls.isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_SIMULATE", "SLOT_SIMULATE"})
	void insertItemIntoHandlerSimulationKeepsCapacityLocalToEachCall(InsertionMode simulation) {
		Fixture fixture = new Fixture(new ItemStack(Items.DIAMOND, 63), ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		ItemStack first = simulation.insert(fixture, 0, 2);
		ItemStack second = simulation.insert(fixture, 0, 2);
		int countAfterSimulations = fixture.inventory.getStackInSlot(0).getCount();
		ItemStack actual = simulation.actualInsertion().insert(fixture, 0, 2);
		assertAll(() -> assertTrue(first.isEmpty()), () -> assertTrue(second.isEmpty()), () -> assertTrue(actual.isEmpty()));
		assertEquals(63, countAfterSimulations);
		assertEquals(64, fixture.inventory.getStackInSlot(0).getCount());
		assertEquals(3, fixture.voided);
		assertEquals(List.of(0, 0, 0), fixture.insertionAttempts);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = InsertionMode.class, names = {"HANDLER_INSERT", "SLOT_INSERT"})
	void insertItemIntoHandlerUsesCapacityReopenedByExtractionOnLaterCalls(InsertionMode mode) {
		Fixture fixture = new Fixture(ItemStack.EMPTY, ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		ItemStack initial = mode.insert(fixture, 0, 65);
		ItemStack extracted = fixture.extract(0, 1);
		ItemStack refilled = mode.insert(fixture, 0, 2);
		ItemStack emptied = fixture.extract(0, 64);
		ItemStack restarted = mode.insert(fixture, 0, 1);
		assertAll(() -> assertTrue(initial.isEmpty()), () -> assertTrue(refilled.isEmpty()), () -> assertTrue(restarted.isEmpty()));
		assertEquals(1, extracted.getCount());
		assertEquals(64, emptied.getCount());
		assertEquals(1, fixture.inventory.getStackInSlot(0).getCount());
		assertEquals(2, fixture.voided);
		assertTrue(fixture.inventory.getStackInSlot(1).isEmpty());
	}

	@Test
	void insertItemIntoHandlerReturnsRemainderWithoutRevisitingSlotReopenedByCompaction() {
		Fixture fixture = new Fixture(ItemStack.EMPTY, ItemStack.EMPTY);
		fixture.remember(1, new ItemStack(Items.GOLD_INGOT));
		fixture.afterInsert = slot -> {
			if (slot == 0 && fixture.inventory.getStackInSlot(0).getCount() >= 4) {
				int crafts = fixture.inventory.getStackInSlot(0).getCount() / 4;
				fixture.extract(0, crafts * 4);
				assertTrue(InsertionMode.HANDLER_INSERT.insert(fixture, 1, new ItemStack(Items.GOLD_INGOT, crafts)).isEmpty());
			}
		};
		ItemStack remainder = InsertionMode.HANDLER_INSERT.insert(fixture, 0, 65);
		assertEquals(1, remainder.getCount());
		assertTrue(remainder.is(Items.DIAMOND));
		assertTrue(fixture.inventory.getStackInSlot(0).isEmpty());
		assertEquals(16, fixture.inventory.getStackInSlot(1).getCount());
		assertTrue(fixture.inventory.getStackInSlot(1).is(Items.GOLD_INGOT));
		assertEquals(0, fixture.voided);
		assertEquals(List.of(0, 1), fixture.insertionAttempts);
		assertEquals(65, remainder.getCount() + fixture.inventory.getStackInSlot(1).getCount() * 4);
	}

	private enum InsertionMode {
		HANDLER_INSERT(false), HANDLER_SIMULATE(true), SLOT_INSERT(false), SLOT_SIMULATE(true);
		private final boolean simulate;

		InsertionMode(boolean simulate) {
			this.simulate = simulate;
		}

		private InsertionMode actualInsertion() {
			return switch (this) {
				case HANDLER_INSERT, HANDLER_SIMULATE -> HANDLER_INSERT;
				case SLOT_INSERT, SLOT_SIMULATE -> SLOT_INSERT;
			};
		}

		private ItemStack insert(Fixture fixture, int slot, int count) {
			return insert(fixture, slot, new ItemStack(Items.DIAMOND, count));
		}

		private ItemStack insert(Fixture fixture, int slot, ItemStack stack) {
			boolean previousSimulation = fixture.simulateCall;
			Transaction previousTx = fixture.activeTx;
			fixture.simulateCall = simulate;
			int inserted;
			try (Transaction tx = Transaction.open(previousTx)) {
				fixture.activeTx = tx;
				ItemResource resource = ItemResource.of(stack);
				inserted = this == SLOT_SIMULATE
						? fixture.inventory.insert(slot, resource, stack.getCount(), tx)
						: fixture.inventory.insert(resource, stack.getCount(), tx);
				if (!simulate) {
					tx.commit();
				}
			} finally {
				fixture.activeTx = previousTx;
				fixture.simulateCall = previousSimulation;
			}
			return inserted == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
		}
	}

	private static class Fixture {
		private final IStorageWrapper wrapper = Mockito.mock(IStorageWrapper.class);
		private final SettingsHandler settings = Mockito.mock(SettingsHandler.class);
		private final UpgradeHandler upgrades = Mockito.mock(UpgradeHandler.class);
		private final MemorySettingsCategoryData memoryData = new MemorySettingsCategoryData();
		private InventoryHandler inventory;
		private final MemorySettingsCategory memory = new MemorySettingsCategory(() -> inventory, memoryData, () -> {
		});
		private final List<Integer> insertionAttempts = new ArrayList<>();
		private final SlotValueMap<Item> filters = new SlotValueMap<>();
		private final List<String> overflowCalls = new ArrayList<>();
		private BiPredicate<Integer, ItemResource> partitionValidity = (slot, resource) -> true;
		private BiFunction<ItemResource, Integer, Integer> beforeInsert = (resource, amount) -> 0;
		private ToIntBiFunction<Integer, ItemResource> stackLimit = (slot, resource) -> resource.getMaxStackSize();
		private IntPredicate accessible = slot -> true;
		private boolean simulateCall;
		private Transaction activeTx;
		private IntConsumer afterInsert = slot -> {
		};
		private boolean voidMatches = true;
		private boolean slotVoidMatches;
		private int voided;
		private int slotVoided;

		private void remember(int slot, ItemStack stack) {
			if (memoryData.ignoreNbt()) {
				memoryData.slotFilterItems().put(slot, stack.getItem());
			} else {
				memoryData.slotFilterStacks().put(slot, ItemStackKey.of(stack));
			}
			memory.reloadFrom(memoryData);
		}

		private void filter(int slot, Item item) {
			filters.add(slot, item);
			try {
				java.lang.reflect.Field field = InventoryHandler.class.getDeclaredField("filterItemSlots");
				field.setAccessible(true);
				((SlotValueMap<Item>) field.get(inventory)).add(slot, item);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}

		private ItemStack extract(int slot, int count) {
			ItemResource resource = inventory.getResource(slot);
			int extracted;
			try (Transaction tx = Transaction.open(activeTx)) {
				extracted = inventory.extract(slot, resource, count, tx);
				tx.commit();
			}
			return resource.toStack(extracted);
		}

		private Fixture(ItemStack... initialStacks) {
			when(wrapper.getSettingsHandler()).thenReturn(settings);
			when(wrapper.getUpgradeHandler()).thenReturn(upgrades);
			when(settings.getTypeCategory(MemorySettingsCategory.class)).thenReturn(memory);
			when(upgrades.getTypeWrappers(VoidUpgradeItem.TYPE)).thenReturn(List.of(Mockito.mock(VoidUpgradeWrapper.class)));
			IOverflowResponseUpgrade overflow = Mockito.mock(IOverflowResponseUpgrade.class);
			when(upgrades.getWrappersThatImplementFromMainStorage(IOverflowResponseUpgrade.class)).thenReturn(List.of(overflow));
			when(overflow.onStorageOverflow(any(ItemResource.class), anyInt())).thenAnswer(invocation -> {
				overflowCalls.add("storage");
				int amount = invocation.getArgument(1);
				if (voidMatches) {
					voided += amount;
					return amount;
				}
				return 0;
			});
			when(overflow.onSlotOverflow(any(ItemResource.class), anyInt())).thenAnswer(invocation -> {
				overflowCalls.add("slot");
				int amount = invocation.getArgument(1);
				if (slotVoidMatches) {
					slotVoided += amount;
					return amount;
				}
				return 0;
			});
			IInsertResponseUpgrade insertUpgrade = Mockito.mock(IInsertResponseUpgrade.class);
			when(upgrades.getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class)).thenReturn(List.of(insertUpgrade));
			when(insertUpgrade.onBeforeInsert(any(InventoryHandler.class), any(ItemResource.class), anyInt()))
					.thenAnswer(invocation -> beforeInsert.apply(invocation.getArgument(1), invocation.getArgument(2)));
			Mockito.doAnswer(invocation -> {
				int slot = invocation.getArgument(1);
				insertionAttempts.add(slot);
				afterInsert.accept(slot);
				return null;
			}).when(insertUpgrade).onAfterInsert(any(InventoryHandler.class), anyInt(), any());
			ContainerContents contents = new ContainerContents();
			NonNullList<ItemStack> stacks = NonNullList.create();
			for (ItemStack stack : initialStacks) {
				stacks.add(stack.copy());
			}
			contents.inventory().reloadFrom(new ContainerContents.InventoryData(stacks));
			StackUpgradeConfig config = Mockito.mock(StackUpgradeConfig.class);
			when(config.canStackItem(any(Item.class))).thenReturn(true);
			inventory = new InventoryHandler(initialStacks.length, wrapper, contents, () -> {
			}, 64, config) {
				@Override
				protected boolean isAllowed(ItemResource resource) {
					return true;
				}

				@Override
				public boolean isValid(int slot, ItemResource resource) {
					return accessible.test(slot) && super.isValid(slot, resource) && !filters.containsSlotAndDoesNotMatch(slot, resource.getItem())
							&& partitionValidity.test(slot, resource);
				}

				@Override
				public long getCapacityAsLong(int slot, ItemResource resource) {
					return stackLimit.applyAsInt(slot, resource);
				}

				@Override
				protected int getCapacity(int slot, ItemResource resource) {
					return stackLimit.applyAsInt(slot, resource);
				}

				@Override
				protected int getCapacityNoInit(int slot, ItemResource resource) {
					return stackLimit.applyAsInt(slot, resource);
				}

				@Override
				public boolean isSlotAccessible(int slot) {
					return accessible.test(slot) && super.isSlotAccessible(slot);
				}
			};
		}
	}
}
