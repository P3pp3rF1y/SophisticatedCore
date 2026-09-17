package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerBlockEntityBaseTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
	}

	@Test
	void hasMatchingFilterUsesNestedTransactionWhenParentIsOpen() {
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
	void legacyStoragePositionsPopulateStorageKeysAndIndexesWhenStorageKeysAreAbsent() {
		BlockPos firstPosition = new BlockPos(3, 64, -2);
		BlockPos secondPosition = new BlockPos(-7, 80, 11);
		CompoundTag legacyData = new CompoundTag();
		ListTag storagePositions = new ListTag();
		storagePositions.add(LongTag.valueOf(firstPosition.asLong()));
		storagePositions.add(LongTag.valueOf(secondPosition.asLong()));
		legacyData.put("storagePositions", storagePositions);
		TestControllerBlockEntity controller = new TestControllerBlockEntity();

		controller.loadAdditional(ValueIOHelper.inputFromCompoundTag(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), legacyData));

		assertEquals(java.util.List.of(firstPosition, secondPosition), controller.getStoragePositions());
		assertTrue(controller.isStorageConnected(firstPosition));
		assertTrue(controller.isStorageConnected(secondPosition));

		CompoundTag explicitEmptyStorageKeys = legacyData.copy();
		explicitEmptyStorageKeys.put("storageKeys", new ListTag());
		TestControllerBlockEntity controllerWithExplicitEmptyKeys = new TestControllerBlockEntity();

		controllerWithExplicitEmptyKeys.loadAdditional(
				ValueIOHelper.inputFromCompoundTag(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), explicitEmptyStorageKeys));

		assertTrue(controllerWithExplicitEmptyKeys.getStoragePositions().isEmpty(),
				"Legacy storagePositions must not migrate when storageKeys is explicitly empty");
		assertTrue(!controllerWithExplicitEmptyKeys.isStorageConnected(firstPosition));
		assertTrue(!controllerWithExplicitEmptyKeys.isStorageConnected(secondPosition));
	}

	private static class TestControllerBlockEntity extends ControllerBlockEntityBase {
		private final InsertJournal insertJournal = new InsertJournal();
		private int insertedAmount = 0;

		private TestControllerBlockEntity() {
			super(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
		}

		private void addFilteredStorage(BlockPos storagePos) {
			filteredInputStorages.add(storagePos);
			emptySlotsStorages.add(storagePos);
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
