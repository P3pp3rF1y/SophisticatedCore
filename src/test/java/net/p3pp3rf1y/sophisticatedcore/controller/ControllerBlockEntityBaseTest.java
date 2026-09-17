package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerBlockEntityBaseTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
		bindTestComponents(Items.DIAMOND);
	}

	private static void bindTestComponents(Item... items) {
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		for (Item item : items) {
			item.builtInRegistryHolder().bindComponents(components);
		}
	}

	@Test
	void hasMatchingFilterUsesNestedTransactionWhenParentIsOpen() throws ReflectiveOperationException {
		// init
		TestControllerBlockEntity controller = new TestControllerBlockEntity();
		controller.addFilteredStorage(BlockPos.ZERO.above());
		boolean matchesFilter;
		int insertedAmountAfterAction;

		// action
		try (Transaction tx = Transaction.openRoot()) {
			matchesFilter = controller.hasMatchingFilter(new ItemStack(Items.DIAMOND), tx);
			insertedAmountAfterAction = controller.getInsertedAmount();
		}

		// assert
		assertTrue(matchesFilter);
		assertEquals(0, insertedAmountAfterAction, "Filter matching probe should roll back its simulated insert");
	}

	@Test
	void controllerStorageKeysDeduplicateOnlyMatchingLinkedGroups() {
		java.util.UUID groupId = java.util.UUID.randomUUID();
		ControllerStorageKey linkedFirst = new ControllerStorageKey(BlockPos.ZERO, groupId);
		ControllerStorageKey linkedSecond = new ControllerStorageKey(BlockPos.ZERO.above(), groupId);
		ControllerStorageKey regular = new ControllerStorageKey(BlockPos.ZERO.above(), null);

		assertEquals(linkedFirst, linkedSecond);
		assertNotEquals(linkedFirst, regular);
	}

	@Test
	void changeSlotsUsesCanonicalIndexForLinkedMemberCallback() throws ReflectiveOperationException {
		// init
		TestControllerBlockEntity controller = new TestControllerBlockEntity();
		BlockPos canonicalPos = BlockPos.ZERO.above();
		BlockPos secondaryPos = canonicalPos.east();
		ControllerStorageKey storageKey = new ControllerStorageKey(canonicalPos, java.util.UUID.randomUUID());
		controller.getStoragePositions().add(canonicalPos);
		getPrivateList(controller, "storageKeys").add(storageKey);
		getPrivateMap(controller, "storageKeyIndexes").put(storageKey, 0);
		getPrivateMap(controller, "storageKeysByPosition").put(canonicalPos, storageKey);
		getPrivateMap(controller, "storageKeysByPosition").put(secondaryPos, storageKey);
		setPrivateField(controller, "baseIndexes", new java.util.ArrayList<>(java.util.List.of(5)));
		setPrivateField(controller, "totalSlots", 5);

		// action
		controller.changeSlots(secondaryPos, 8, false);

		// assert
		assertEquals(8, controller.size());
		assertEquals(8, controller.getSlots(0));
	}

	@Test
	void removeStoragePromotesUnloadedLinkedMemberWithoutRemovingSlots() throws ReflectiveOperationException {
		// init
		TestControllerBlockEntity controller = new TestControllerBlockEntity();
		BlockPos canonicalPos = BlockPos.ZERO.above();
		BlockPos unloadedMemberPos = canonicalPos.east();
		ControllerStorageKey storageKey = new ControllerStorageKey(canonicalPos, java.util.UUID.randomUUID());
		controller.getStoragePositions().add(canonicalPos);
		getPrivateList(controller, "storageKeys").add(storageKey);
		getPrivateMap(controller, "storageKeyIndexes").put(storageKey, 0);
		getPrivateMap(controller, "storageKeysByPosition").put(canonicalPos, storageKey);
		getPrivateMap(controller, "storageKeysByPosition").put(unloadedMemberPos, storageKey);
		setPrivateField(controller, "baseIndexes", new java.util.ArrayList<>(java.util.List.of(5)));
		setPrivateField(controller, "totalSlots", 5);

		// action
		invokePrivateMethod(controller, "removeStorageInventoryDataAndUnregisterController", canonicalPos);

		// assert
		assertEquals(java.util.List.of(unloadedMemberPos), controller.getStoragePositions());
		assertEquals(5, controller.size());
	}

	@Test
	void loadAdditionalMigratesLegacyStoragePositionsWhenStorageKeysAreAbsent() throws ReflectiveOperationException {
		BlockPos legacyPosition = BlockPos.ZERO.above();
		CompoundTag tag = ValueIOHelper.collectOutputToTag(RegistryAccess.EMPTY,
				out -> ValueIOHelper.saveList(out, "storagePositions", java.util.List.of(legacyPosition), BlockPos.CODEC));
		TestControllerBlockEntity controller = new TestControllerBlockEntity();

		controller.loadAdditional(ValueIOHelper.inputFromCompoundTag(RegistryAccess.EMPTY, tag));

		assertEquals(java.util.List.of(legacyPosition), controller.getStoragePositions());
		assertEquals(new ControllerStorageKey(legacyPosition), getPrivateMap(controller, "storageKeysByPosition").get(legacyPosition));
	}

	@Test
	void loadAdditionalPreservesPresentEmptyStorageKeysInsteadOfMigratingLegacyStoragePositions() {
		BlockPos legacyPosition = BlockPos.ZERO.above();
		CompoundTag tag = ValueIOHelper.collectOutputToTag(RegistryAccess.EMPTY,
				out -> ValueIOHelper.saveList(out, "storagePositions", java.util.List.of(legacyPosition), BlockPos.CODEC));
		tag.put("storageKeys", new ListTag());
		TestControllerBlockEntity controller = new TestControllerBlockEntity();

		controller.loadAdditional(ValueIOHelper.inputFromCompoundTag(RegistryAccess.EMPTY, tag));

		assertTrue(controller.getStoragePositions().isEmpty());
	}

	private static void setPrivateField(Object target, String name, Object value) throws ReflectiveOperationException {
		java.lang.reflect.Field field = ControllerBlockEntityBase.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	@SuppressWarnings("unchecked")
	private static <K, V> java.util.Map<K, V> getPrivateMap(Object target, String name) throws ReflectiveOperationException {
		java.lang.reflect.Field field = ControllerBlockEntityBase.class.getDeclaredField(name);
		field.setAccessible(true);
		return (java.util.Map<K, V>) field.get(target);
	}

	@SuppressWarnings("unchecked")
	private static <T> java.util.List<T> getPrivateList(Object target, String name) throws ReflectiveOperationException {
		java.lang.reflect.Field field = ControllerBlockEntityBase.class.getDeclaredField(name);
		field.setAccessible(true);
		return (java.util.List<T>) field.get(target);
	}

	private static void invokePrivateMethod(Object target, String name, BlockPos storagePos) throws ReflectiveOperationException {
		java.lang.reflect.Method method = ControllerBlockEntityBase.class.getDeclaredMethod(name, BlockPos.class);
		method.setAccessible(true);
		method.invoke(target, storagePos);
	}

	private static class TestControllerBlockEntity extends ControllerBlockEntityBase {
		private final InsertJournal insertJournal = new InsertJournal();
		private int insertedAmount = 0;

		private TestControllerBlockEntity() {
			super(BlockEntityTypes.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
		}

		private void addFilteredStorage(BlockPos storagePos) throws ReflectiveOperationException {
			filteredInputStorages.add(storagePos);
			emptySlotsStorages.add(storagePos);
			ControllerStorageKey storageKey = new ControllerStorageKey(storagePos);
			filteredInputStorageKeys.add(storageKey);
			emptySlotStorageKeys.add(storageKey);
			getStoragePositions().add(storagePos);
			getPrivateList(this, "storageKeys").add(storageKey);
			getPrivateMap(this, "storageKeyIndexes").put(storageKey, 0);
		}

		private int getInsertedAmount() {
			return insertedAmount;
		}

		@Override
		protected int getSearchRange() {
			return 0;
		}

		@Override
		protected int insertIntoStorage(BlockPos storagePos, ItemResource resource, int amount, TransactionContext tx) {
			insertJournal.updateSnapshots(tx);
			insertedAmount += amount;
			return amount;
		}

		private class InsertJournal extends SnapshotJournal<Integer> {
			@Override
			protected Integer createSnapshot() {
				return insertedAmount;
			}

			@Override
			protected void revertToSnapshot(Integer snapshot) {
				insertedAmount = snapshot;
			}
		}
	}
}
