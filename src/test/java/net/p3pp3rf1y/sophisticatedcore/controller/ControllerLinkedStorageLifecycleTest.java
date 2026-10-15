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
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
		Items.DIAMOND.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build());
		Items.COBBLESTONE.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build());
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
			originalSlots = controller.size();
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
		assertEquals(10, controller.size());
		assertEquals(0, extractedBeforeRefresh);
		assertEquals(10, extractedAfterRefresh);
		verify(wrapper, times(2)).registerOnInventoryInputOutputHandlerRefreshListener(any());
		verify(resizedInventory).registerTrackingListeners(any(), any(), any(), any());
	}

	@Test
	void persistedGroupRetainsMemberOrderAndOfflineCapacity() {
		TestController controller = new TestController();
		load(controller, savedGroup(UUID.randomUUID(), FIRST, List.of(FIRST, SECOND), 4));
		CompoundTag saved = controller.getUpdateTag(REGISTRIES);
		load(controller, saved);
		assertEquals(List.of(FIRST, SECOND), controller.members(FIRST));
		assertEquals(saved.get("storageMemberKeys"), controller.getUpdateTag(REGISTRIES).get("storageMemberKeys"));
		assertEquals(4, controller.size());
	}

	@Test
	void reloadReconcilesCapacityBeforeTheNextLogicalStorageAndRegistersOneListener() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		BlockPos next = new BlockPos(-1, 0, 0);
		CompoundTag saved = savedGroup(group, FIRST, List.of(FIRST, SECOND), 4);
		saved.getListOrEmpty("storageKeys").add(encode(new ControllerStorageKey(next)));
		saved.getListOrEmpty("baseIndexes").add(IntTag.valueOf(7));
		saved.putInt("totalSlots", 7);
		load(controller, saved);
		IControllableStorage first = storage(FIRST, group, 9);
		IControllableStorage second = storage(SECOND, group, 9);
		IControllableStorage following = storage(next, null, 3);
		ItemResource marker = ItemResource.of(Items.DIAMOND);
		when(following.getStorageWrapper().getInventoryForInputOutput().getResource(0)).thenReturn(marker);
		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllableStorage.class)).thenReturn(Optional.of(second));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, next, IControllableStorage.class)).thenReturn(Optional.of(following));
			controller.onLoad();
			assertEquals(12, controller.size());
			assertSame(marker, controller.getResource(9));
			verify(first).registerController(controller);
			verify(second, never()).registerController(controller);
		}
	}

	@Test
	void removingRepresentativeKeepsSurvivingLogicalIndexAndMembership() {
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
			controller.removeStorage(FIRST);
			assertEquals(List.of(SECOND), controller.getStoragePositions());
			assertEquals(List.of(SECOND), controller.members(SECOND));
			assertEquals(4, controller.size());
			verify(second).registerController(controller);
		}
	}

	@Test
	void removingOnlyLogicalStorageShiftsFollowingPositionAndSlots() {
		TestController controller = new TestController();
		load(controller, savedGroup(UUID.randomUUID(), FIRST, List.of(FIRST), 4));
		controller.removeStorage(FIRST);
		assertEquals(List.of(), controller.getStoragePositions());
		assertEquals(0, controller.size());
	}

	@Test
	void unloadingControllerRetainsDurableMembershipAndRestoresOnlyOneRuntimeListener() {
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
			assertEquals(List.of(FIRST, SECOND), controller.members(FIRST));
			verify(first).unregisterControllerListeners();
			verify(second, never()).registerController(controller);
			controller.onLoad();
			assertEquals(List.of(FIRST, SECOND), controller.members(FIRST));
			verify(first, times(2)).registerController(controller);
			verify(second, never()).registerController(controller);
		}
	}

	@Test
	void removalVerificationPromotesOnlyVerifiedMissingMemberWithoutLoadingOtherChunks() {
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

	private static void load(TestController controller, CompoundTag tag) {
		controller.loadAdditional(ValueIOHelper.inputFromCompoundTag(REGISTRIES, tag));
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

	private static CompoundTag savedGroup(UUID group, BlockPos anchor, List<BlockPos> members, int slots) {
		CompoundTag tag = new CompoundTag();
		ListTag keys = new ListTag();
		keys.add(encode(new ControllerStorageKey(anchor, group)));
		tag.put("storageKeys", keys);
		ListTag memberKeys = new ListTag();
		members.forEach(pos -> memberKeys.add(encode(new ControllerStorageKey(pos, group))));
		tag.put("storageMemberKeys", memberKeys);
		ListTag indexes = new ListTag();
		indexes.add(IntTag.valueOf(slots));
		tag.put("baseIndexes", indexes);
		tag.putInt("totalSlots", slots);
		return tag;
	}

	private static net.minecraft.nbt.Tag encode(ControllerStorageKey key) {
		return ControllerStorageKey.CODEC.encodeStart(NbtOps.INSTANCE, key).getOrThrow();
	}

	private static IControllableStorage storage(BlockPos pos, UUID group, int slots) {
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

	private static class TestController extends ControllerBlockEntityBase {
		private TestController() {
			super(BlockEntityTypes.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
		}

		@Override
		protected int getSearchRange() {
			return 16;
		}

		private List<BlockPos> members(BlockPos pos) {
			return List.copyOf(getStorageMemberPositions(pos));
		}
	}
}
