package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ControllerLinkedStorageLifecycleTest {
	private static final BlockPos FIRST = new BlockPos(1, 0, 0);
	private static final BlockPos SECOND = new BlockPos(0, 0, 1);
	private static final HolderLookup.Provider REGISTRIES = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		for (Item item : List.of(Items.DIAMOND, Items.GOLD_INGOT, Items.IRON_INGOT, Items.COBBLESTONE)) {
			item.builtInRegistryHolder().bindComponents(components);
		}
	}

	@Test
	void onInventoryInputOutputHandlerRefreshRebindsControllerToReplacementHandler() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		load(controller, savedGroup(null, FIRST, List.of(FIRST), 1));
		IStorageWrapper wrapper = mock(IStorageWrapper.class);
		ITrackedContentsItemResourceHandler originalInventory = mock(ITrackedContentsItemResourceHandler.class);
		ITrackedContentsItemResourceHandler resizedInventory = mock(ITrackedContentsItemResourceHandler.class);
		AtomicReference<ITrackedContentsItemResourceHandler> inventory = new AtomicReference<>(originalInventory);
		IControllableStorage storage = refreshingStorage(level, wrapper);
		when(wrapper.getInventoryForInputOutput()).thenAnswer(invocation -> inventory.get());
		when(originalInventory.size()).thenReturn(1);
		when(resizedInventory.size()).thenReturn(10);
		ItemResource cobblestone = ItemResource.of(Items.COBBLESTONE);
		when(resizedInventory.getTrackedStacks()).thenReturn(Set.of(ItemStackKey.of(cobblestone)));
		when(resizedInventory.extract(eq(cobblestone), eq(10), any())).thenReturn(10);
		AtomicReference<Runnable> refreshListener = new AtomicReference<>();
		doAnswer(invocation -> {
			refreshListener.set(invocation.getArgument(0));
			return null;
		}).when(wrapper).registerOnInventoryInputOutputHandlerRefreshListener(any());
		int originalSlots;
		int extractedBeforeRefresh;
		int extractedAfterRefresh;

		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(storage));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, BlockPos.ZERO, ControllerBlockEntityBase.class)).thenReturn(Optional.of(controller));

			controller.onLoad();
			originalSlots = controller.handlerSlots();
			try (Transaction tx = Transaction.openRoot()) {
				extractedBeforeRefresh = controller.extract(cobblestone, 10, tx);
			}
			inventory.set(resizedInventory);
			refreshListener.get().run();
			try (Transaction tx = Transaction.openRoot()) {
				extractedAfterRefresh = controller.extract(cobblestone, 10, tx);
			}
		}

		assertEquals(1, originalSlots);
		assertEquals(0, extractedBeforeRefresh);
		assertEquals(10, extractedAfterRefresh);
		verify(wrapper, times(2)).registerOnInventoryInputOutputHandlerRefreshListener(any());
		verify(resizedInventory).registerTrackingListeners(any(), any(), any(), any());
	}

	@Test
	void loadAdditionalRetainsPersistedMemberOrderAcrossRepeatedSaves() {
		TestController controller = new TestController();
		CompoundTag saved = savedGroup(UUID.randomUUID(), SECOND, List.of(FIRST, SECOND), 4);
		load(controller, saved);
		CompoundTag resaved = controller.getUpdateTag(REGISTRIES);
		load(controller, resaved);
		assertEquals(saved.get("storageMemberKeys"), resaved.get("storageMemberKeys"));
		assertEquals(List.of(FIRST, SECOND), controller.members(SECOND));
		assertEquals(4, controller.size());
	}

	@Test
	void onLoadReconcilesOfflineCapacityBeforeRoutingSlotsOfTheFollowingStorage() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		BlockPos following = new BlockPos(-1, 0, 0);
		CompoundTag saved = savedGroup(group, FIRST, List.of(FIRST, SECOND), 4);
		saved.getListOrEmpty("storageKeys").add(new ControllerStorageKey(following).save());
		saved.getListOrEmpty("storageMemberKeys").add(new ControllerStorageKey(following).save());
		saved.getListOrEmpty("baseIndexes").add(IntTag.valueOf(7));
		saved.putInt("totalSlots", 7);
		load(controller, saved);
		IControllableStorage first = storage(FIRST, group, 9);
		IControllableStorage second = storage(SECOND, group, 9);
		IControllableStorage next = storage(following, null, 3);
		ItemResource marker = ItemResource.of(Items.DIAMOND);
		when(next.getStorageWrapper().getInventoryForInputOutput().getResource(0)).thenReturn(marker);
		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllableStorage.class)).thenReturn(Optional.of(second));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, following, IControllableStorage.class)).thenReturn(Optional.of(next));
			controller.onLoad();
			assertEquals(12, controller.size());
			assertSame(marker, controller.getResource(9));
			assertEquals(9, controller.getUpdateTag(REGISTRIES).getListOrEmpty("baseIndexes").getInt(0).orElseThrow());
			assertEquals(12, controller.getUpdateTag(REGISTRIES).getListOrEmpty("baseIndexes").getInt(1).orElseThrow());
			verify(second, never()).registerController(controller);
		}
	}

	@Test
	void onLoadRestoresAllMembershipsButOnlyOneListenerAndReconcilesOfflineCapacity() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		load(controller, savedGroup(group, FIRST, List.of(FIRST, SECOND), 4));
		IControllableStorage first = storage(FIRST, group, 9);
		IControllableStorage second = storage(SECOND, group, 9);
		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllableStorage.class)).thenReturn(Optional.of(second));
			controller.onLoad();
			assertEquals(9, controller.size());
			verify(first).registerControllerMembership(controller);
			verify(second).registerControllerMembership(controller);
			verify(first).registerController(controller);
			verify(second, never()).registerController(controller);
			assertEquals(List.of(FIRST), controller.getStoragePositions());
		}
	}

	@Test
	void removeStoragePromotesSurvivingMemberAtTheExistingLogicalIndex() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		load(controller, savedGroup(group, FIRST, List.of(FIRST, SECOND), 4));
		IControllableStorage first = storage(FIRST, group, 4);
		IControllableStorage second = storage(SECOND, group, 4);
		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllableStorage.class)).thenReturn(Optional.of(second));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllerBoundable.class)).thenReturn(Optional.of(second));
			controller.onLoad();
			controller.removeStorage(FIRST);
			assertEquals(List.of(SECOND), controller.getStoragePositions());
			assertEquals(List.of(SECOND), controller.members(SECOND));
			assertEquals(4, controller.size());
			verify(first).unregisterController();
			verify(second).registerController(controller);
		}
	}

	@Test
	void removeStorageKeepsOfflineAnchorButRegistersLoadedSurvivorForInsertion() {
		verifyMixedLoadSurvivor(false);
	}

	@Test
	void onLoadRebuildsLoadedSurvivorRoutesWithoutReplacingOfflineAnchor() {
		verifyMixedLoadSurvivor(true);
	}

	private void verifyMixedLoadSurvivor(boolean reload) {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		BlockPos third = new BlockPos(-1, 0, 0);
		load(controller, savedGroup(group, FIRST, List.of(FIRST, SECOND, third), 4));
		IControllableStorage first = storage(FIRST, group, 4);
		IControllableStorage last = storage(third, group, 4);
		ITrackedContentsItemResourceHandler inventory = last.getStorageWrapper().getInventoryForInputOutput();
		when(inventory.hasEmptySlots()).thenReturn(true);
		ItemResource marker = ItemResource.of(Items.DIAMOND);
		ItemStackKey tracked = ItemStackKey.of(marker);
		when(inventory.getTrackedStacks()).thenReturn(Set.of(tracked));
		MemorySettingsCategory memory = last.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
		when(memory.getFilterItemSlots()).thenReturn(Map.of(Items.GOLD_INGOT, Set.of(1)));
		when(memory.getFilterStackSlots()).thenReturn(Map.of(123, Set.of(2)));
		when(last.getStorageWrapper().getInventoryHandler().getFilterItems()).thenReturn(Set.of(Items.IRON_INGOT));
		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, third, IControllableStorage.class)).thenReturn(Optional.of(last));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, third, IControllerBoundable.class)).thenReturn(Optional.of(last));
			controller.onLoad();
			controller.removeStorage(FIRST);
			if (reload) {
				controller.onChunkUnloaded();
				controller.onLoad();
			}
			controller.assertRoutes(third, tracked);
			assertSame(inventory, controller.getHandlerFromIndex(0));
			try (Transaction tx = Transaction.openRoot()) {
				when(inventory.insert(marker, 7, tx)).thenReturn(7);
				int inserted = controller.insert(marker, 7, tx);
				verify(inventory).insert(marker, 7, tx);
				assertEquals(7, inserted);
			}
			assertEquals(List.of(SECOND), controller.getStoragePositions());
			assertEquals(List.of(SECOND, third), controller.members(SECOND));
			assertEquals(4, controller.size());
			verify(last, times(reload ? 2 : 1)).registerController(controller);
			controller.assertRoutes(third, tracked);
		}
	}

	@Test
	void onChunkUnloadedPreservesMembershipAndDisablesCachedAccessWithoutPromotingSecondary() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		load(controller, savedGroup(group, FIRST, List.of(FIRST, SECOND), 4));
		IControllableStorage first = storage(FIRST, group, 4);
		IControllableStorage second = storage(SECOND, group, 4);
		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllableStorage.class)).thenReturn(Optional.of(second));
			controller.onLoad();
			int availableSlots = controller.handlerSlots();
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.empty());
			int unloadedSourceSlots = controller.handlerSlots();
			List<BlockPos> membersWithUnloadedSource = controller.members(FIRST);
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			controller.onChunkUnloaded();
			assertEquals(4, availableSlots);
			assertEquals(0, unloadedSourceSlots);
			assertEquals(List.of(FIRST, SECOND), membersWithUnloadedSource);
			assertEquals(0, controller.handlerSlots());
			assertEquals(List.of(FIRST, SECOND), controller.members(FIRST));
			verify(first).unregisterControllerListeners();
			verify(first, never()).unregisterController();
			verify(second, never()).unregisterControllerMembership();
			verify(second, never()).registerController(controller);
		}
	}

	@Test
	void removeStorageDoesNotReplaceListenerOrLogicalIndexForNonRepresentativeMember() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		load(controller, savedGroup(group, FIRST, List.of(FIRST, SECOND), 4));
		IControllableStorage first = storage(FIRST, group, 4);
		IControllableStorage second = storage(SECOND, group, 4);
		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllableStorage.class)).thenReturn(Optional.of(second));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllerBoundable.class)).thenReturn(Optional.of(first));
			controller.onLoad();
			controller.removeStorage(SECOND);
			assertEquals(List.of(FIRST), controller.getStoragePositions());
			assertEquals(List.of(FIRST), controller.members(FIRST));
			assertEquals(4, controller.size());
			verify(first, times(1)).registerController(controller);
			verify(first, never()).unregisterControllerListeners();
			verify(second).unregisterControllerMembership();
			verify(second, never()).unregisterController();
		}
	}

	@Test
	void removeStorageVerificationRetainsUnloadedGroupMembersWithoutLoadingChunks() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		load(controller, savedGroup(UUID.randomUUID(), FIRST, List.of(FIRST, SECOND), 4));
		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			controller.removeStorage(new BlockPos(8, 0, 8));
			assertEquals(List.of(FIRST, SECOND), controller.members(FIRST));
			assertEquals(List.of(FIRST), controller.getStoragePositions());
			assertEquals(4, controller.size());
			verify(level, never()).getChunkSource();
		}
	}

	@Test
	void removeStorageVerificationDropsLoadedMissingMemberButRetainsUnloadedSurvivor() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		load(controller, savedGroup(UUID.randomUUID(), FIRST, List.of(FIRST, SECOND), 4));
		when(level.hasChunkAt(FIRST)).thenReturn(true);
		when(level.hasChunkAt(SECOND)).thenReturn(false);
		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			controller.removeStorage(new BlockPos(8, 0, 8));
			assertEquals(List.of(SECOND), controller.members(SECOND));
			assertEquals(List.of(SECOND), controller.getStoragePositions());
			assertEquals(4, controller.size());
			verify(level, never()).getChunkSource();
		}
	}

	@Test
	void onLoadAfterControllerUnloadRestoresEveryMemberAndRebuildsOneListener() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		load(controller, savedGroup(group, FIRST, List.of(FIRST, SECOND), 4));
		IControllableStorage first = storage(FIRST, group, 4);
		IControllableStorage second = storage(SECOND, group, 4);
		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllableStorage.class)).thenReturn(Optional.of(second));
			controller.onLoad();
			controller.onChunkUnloaded();
			controller.onLoad();
			assertEquals(4, controller.handlerSlots());
			assertEquals(List.of(FIRST, SECOND), controller.members(FIRST));
			verify(first, times(2)).registerControllerMembership(controller);
			verify(second, times(2)).registerControllerMembership(controller);
			verify(first, times(2)).registerController(controller);
			verify(second, never()).registerController(controller);
		}
	}

	private static void load(TestController controller, CompoundTag tag) {
		controller.loadAdditional(ValueIOHelper.inputFromCompoundTag(REGISTRIES, tag));
	}

	private static IControllableStorage storage(BlockPos pos, @Nullable UUID group, int slots) {
		IControllableStorage storage = mock(IControllableStorage.class);
		IStorageWrapper wrapper = mock(IStorageWrapper.class);
		ITrackedContentsItemResourceHandler inventory = mock(ITrackedContentsItemResourceHandler.class);
		SettingsHandler settings = mock(SettingsHandler.class);
		when(storage.getStorageWrapper()).thenReturn(wrapper);
		when(storage.getControlledStorageBlockPos()).thenReturn(pos);
		when(storage.getControllerStorageKey()).thenReturn(new ControllerStorageKey(pos, group));
		when(wrapper.getInventoryForInputOutput()).thenReturn(inventory);
		when(inventory.size()).thenReturn(slots);
		when(wrapper.getInventoryHandler()).thenReturn(mock(InventoryHandler.class));
		when(wrapper.getUpgradeHandler()).thenReturn(mock(UpgradeHandler.class));
		when(wrapper.getSettingsHandler()).thenReturn(settings);
		when(settings.getTypeCategory(MemorySettingsCategory.class)).thenReturn(mock(MemorySettingsCategory.class));
		return storage;
	}

	private static IControllableStorage refreshingStorage(ServerLevel level, IStorageWrapper wrapper) {
		IControllableStorage storage = mock(IControllableStorage.class, CALLS_REAL_METHODS);
		when(storage.getStorageWrapper()).thenReturn(wrapper);
		when(storage.getStorageBlockPos()).thenReturn(FIRST);
		when(storage.getControlledStorageBlockPos()).thenReturn(FIRST);
		when(storage.getControllerStorageKey()).thenReturn(new ControllerStorageKey(FIRST));
		when(storage.getStorageBlockLevel()).thenReturn(level);
		when(storage.getControllerPos()).thenReturn(Optional.of(BlockPos.ZERO));
		InventoryHandler inventoryHandler = mock(InventoryHandler.class);
		when(wrapper.getInventoryHandler()).thenReturn(inventoryHandler);
		when(wrapper.getUpgradeHandler()).thenReturn(mock(UpgradeHandler.class));
		SettingsHandler settings = mock(SettingsHandler.class);
		when(wrapper.getSettingsHandler()).thenReturn(settings);
		when(settings.getTypeCategory(MemorySettingsCategory.class)).thenReturn(mock(MemorySettingsCategory.class));
		return storage;
	}

	private static CompoundTag savedGroup(@Nullable UUID group, BlockPos anchor, List<BlockPos> members, int slots) {
		CompoundTag tag = new CompoundTag();
		ListTag keys = new ListTag();
		keys.add(new ControllerStorageKey(anchor, group).save());
		tag.put("storageKeys", keys);
		ListTag memberKeys = new ListTag();
		members.forEach(pos -> memberKeys.add(new ControllerStorageKey(pos, group).save()));
		tag.put("storageMemberKeys", memberKeys);
		ListTag indexes = new ListTag();
		indexes.add(IntTag.valueOf(slots));
		tag.put("baseIndexes", indexes);
		tag.putInt("totalSlots", slots);
		return tag;
	}

	private static class TestController extends ControllerBlockEntityBase {
		private TestController() {
			super(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
		}

		@Override
		protected int getSearchRange() {
			return 16;
		}

		private List<BlockPos> members(BlockPos pos) {
			return List.copyOf(getStorageMemberPositions(pos));
		}

		private int handlerSlots() {
			return getHandlerFromIndex(0).size();
		}

		private void assertRoutes(BlockPos source, ItemStackKey tracked) {
			assertEquals(Set.of(source), stackStorages.get(tracked));
			assertEquals(Set.of(source), emptySlotsStorages);
			assertEquals(Set.of(source), memorizedItemStorages.get(Items.GOLD_INGOT));
			assertEquals(Set.of(source), memorizedStackStorages.get(123));
			assertEquals(Set.of(source), filterItemStorages.get(Items.IRON_INGOT));
		}
	}
}
