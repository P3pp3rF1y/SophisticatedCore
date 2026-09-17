package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
	void clearControllerStateForRefreshClearsAndRebuildsLinkedStackRouting() throws ReflectiveOperationException {
		TestControllerBlockEntity controller = new TestControllerBlockEntity();
		BlockPos memberPos = BlockPos.ZERO.above();
		BlockPos staleAnchorPos = memberPos.east();
		BlockPos rebuiltAnchorPos = memberPos.west();
		UUID groupId = UUID.randomUUID();
		ControllerStorageKey staleStorageKey = new ControllerStorageKey(staleAnchorPos, groupId);
		ItemStackKey stackKey = ItemStackKey.of(new ItemStack(Items.DIAMOND));
		Map<BlockPos, ControllerStorageKey> storageKeysByPosition = getMapField(controller, "storageKeysByPosition");
		Map<ItemStackKey, Set<ControllerStorageKey>> stackStorageKeys = getMapField(controller, "stackStorageKeys");
		storageKeysByPosition.put(memberPos, staleStorageKey);
		controller.addStorageStack(memberPos, stackKey);
		assertEquals(staleAnchorPos, stackStorageKeys.get(stackKey).iterator().next().position());

		Method clearControllerStateForRefresh = ControllerBlockEntityBase.class.getDeclaredMethod("clearControllerStateForRefresh");
		clearControllerStateForRefresh.setAccessible(true);
		clearControllerStateForRefresh.invoke(controller);

		assertFalse(stackStorageKeys.containsKey(stackKey));
		ControllerStorageKey rebuiltStorageKey = new ControllerStorageKey(rebuiltAnchorPos, groupId);
		storageKeysByPosition.put(memberPos, rebuiltStorageKey);
		controller.addStorageStack(memberPos, stackKey);
		assertEquals(rebuiltAnchorPos, stackStorageKeys.get(stackKey).iterator().next().position());
	}

	@SuppressWarnings("unchecked")
	private static <K, V> Map<K, V> getMapField(ControllerBlockEntityBase controller, String fieldName) throws ReflectiveOperationException {
		Field field = ControllerBlockEntityBase.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (Map<K, V>) field.get(controller);
	}

	private static class TestControllerBlockEntity extends ControllerBlockEntityBase {
		private final InsertJournal insertJournal = new InsertJournal();
		private int insertedAmount = 0;

		private TestControllerBlockEntity() {
			super(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
		}

		private void addFilteredStorage(BlockPos storagePos) {
			ControllerStorageKey storageKey = new ControllerStorageKey(storagePos);
			filteredInputStorageKeys.add(storageKey);
			emptySlotStorageKeys.add(storageKey);
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
