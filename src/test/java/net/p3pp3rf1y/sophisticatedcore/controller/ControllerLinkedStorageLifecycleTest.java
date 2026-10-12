package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ControllerLinkedStorageLifecycleTest {
	private static final BlockPos FIRST = new BlockPos(1, 0, 0);
	private static final BlockPos SECOND = new BlockPos(0, 0, 1);

	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void loadRetainsPersistedMemberOrderAcrossRepeatedSaves() {
		TestController controller = new TestController();
		CompoundTag saved = savedGroup(UUID.randomUUID(), SECOND, List.of(FIRST, SECOND), 4);
		controller.load(saved);
		CompoundTag resaved = controller.getUpdateTag();
		controller.load(resaved);

		assertEquals(saved.get("storageMemberKeys"), resaved.get("storageMemberKeys"));
		assertEquals(List.of(FIRST, SECOND), controller.members(SECOND));
		assertEquals(4, controller.getSlots());
	}

	@Test
	void onLoadRestoresAllMembershipsButOnlyOneListenerAndReconcilesOfflineCapacity() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		controller.load(savedGroup(group, FIRST, List.of(FIRST, SECOND), 4));
		IControllableStorage first = storage(FIRST, group, 9);
		IControllableStorage second = storage(SECOND, group, 9);
		try (var world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllableStorage.class)).thenReturn(Optional.of(second));
			controller.onLoad();
			assertEquals(9, controller.getSlots());
			verify(first).registerControllerMembership(controller);
			verify(second).registerControllerMembership(controller);
			verify(first).registerController(controller);
			verify(second, never()).registerController(controller);
			assertEquals(List.of(FIRST), controller.getStoragePositions());
		}
	}

	@Test
	void onChunkUnloadedPreservesMembershipAndDisablesCachedAccessWithoutPromotingSecondary() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		controller.load(savedGroup(group, FIRST, List.of(FIRST, SECOND), 4));
		IControllableStorage first = storage(FIRST, group, 4);
		IControllableStorage second = storage(SECOND, group, 4);
		try (var world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllableStorage.class)).thenReturn(Optional.of(second));
			controller.onLoad();
			assertEquals(4, controller.handlerSlots());
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.empty());
			assertEquals(0, controller.handlerSlots());
			assertEquals(List.of(FIRST, SECOND), controller.members(FIRST));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			controller.onChunkUnloaded();
			assertEquals(0, controller.handlerSlots());
			assertEquals(List.of(FIRST, SECOND), controller.members(FIRST));
			verify(first).unregisterControllerListeners();
			verify(first, never()).unregisterController();
			verify(second, never()).unregisterControllerMembership();
		}
	}

	@Test
	void removeStoragePromotesSurvivingMemberAtTheExistingLogicalIndex() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		controller.load(savedGroup(group, FIRST, List.of(FIRST, SECOND), 4));
		IControllableStorage first = storage(FIRST, group, 4);
		IControllableStorage second = storage(SECOND, group, 4);
		try (var world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllableStorage.class)).thenReturn(Optional.of(second));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllerBoundable.class)).thenReturn(Optional.of(second));
			controller.onLoad();
			controller.removeStorage(FIRST);
			assertEquals(List.of(SECOND), controller.getStoragePositions());
			assertEquals(List.of(SECOND), controller.members(SECOND));
			assertEquals(4, controller.getSlots());
			verify(first).unregisterController();
			verify(second).registerController(controller);
		}
	}

	@Test
	void removeStorageDoesNotReplaceListenerOrLogicalIndexForNonRepresentativeMember() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		controller.load(savedGroup(group, FIRST, List.of(FIRST, SECOND), 4));
		IControllableStorage first = storage(FIRST, group, 4);
		IControllableStorage second = storage(SECOND, group, 4);
		try (var world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllableStorage.class)).thenReturn(Optional.of(second));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllerBoundable.class)).thenReturn(Optional.of(first));
			controller.onLoad();
			controller.removeStorage(SECOND);
			assertEquals(List.of(FIRST), controller.getStoragePositions());
			assertEquals(List.of(FIRST), controller.members(FIRST));
			assertEquals(4, controller.getSlots());
			verify(first, times(1)).registerController(controller);
			verify(first, never()).unregisterControllerListeners();
			verify(second).unregisterControllerMembership();
			verify(second, never()).unregisterController();
		}
	}

	@Test
	void removeStorageVerificationDropsLoadedMissingMemberButRetainsUnloadedSurvivor() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		controller.load(savedGroup(UUID.randomUUID(), FIRST, List.of(FIRST, SECOND), 4));
		when(level.hasChunkAt(FIRST)).thenReturn(true);
		when(level.hasChunkAt(SECOND)).thenReturn(false);
		try (var world = mockStatic(WorldHelper.class)) {
			controller.removeStorage(new BlockPos(8, 0, 8));
			assertEquals(List.of(SECOND), controller.members(SECOND));
			assertEquals(List.of(SECOND), controller.getStoragePositions());
			assertEquals(4, controller.getSlots());
			verify(level, never()).getChunkSource();
		}
	}

	@Test
	void loadedSecondaryRestoresRoutingWhenPersistedAnchorIsOffline() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		controller.load(savedGroup(group, FIRST, List.of(FIRST, SECOND), 4));
		IControllableStorage second = storage(SECOND, group, 9);
		try (var world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.empty());
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllableStorage.class)).thenReturn(Optional.of(second));
			controller.onLoad();
			assertEquals(List.of(FIRST), controller.getStoragePositions());
			assertEquals(List.of(FIRST, SECOND), controller.members(FIRST));
			assertEquals(9, controller.getSlots());
			assertEquals(9, controller.handlerSlots());
			verify(second).registerController(controller);
			verify(level, never()).getChunkSource();
		}
	}

	@Test
	void newlyDiscoveredMemberTakesListenerWhenPersistedAnchorIsOffline() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		controller.load(savedGroup(group, FIRST, List.of(FIRST), 4));
		IControllableStorage second = storage(SECOND, group, 9);
		when(second.canBeConnected()).thenReturn(true);
		when(second.hasStorageData()).thenReturn(true);
		try (var world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.empty());
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllableStorage.class)).thenReturn(Optional.of(second));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllerBoundable.class)).thenReturn(Optional.of(second));
			controller.onLoad();
			controller.addStorage(SECOND);
			assertEquals(List.of(FIRST), controller.getStoragePositions());
			assertEquals(List.of(FIRST, SECOND), controller.members(FIRST));
			assertEquals(9, controller.getSlots());
			assertEquals(9, controller.handlerSlots());
			verify(second).registerController(controller);
			verify(level, never()).getChunkSource();
		}
	}

	@Test
	void onLoadAfterControllerUnloadRestoresEveryMemberAndRebuildsOneListener() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		controller.load(savedGroup(group, FIRST, List.of(FIRST, SECOND), 4));
		IControllableStorage first = storage(FIRST, group, 4);
		IControllableStorage second = storage(SECOND, group, 4);
		try (var world = mockStatic(WorldHelper.class)) {
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

	@Test
	void onLoadReconcilesOfflineCapacityBeforeRoutingSlotsOfTheFollowingStorage() {
		TestController controller = new TestController();
		ServerLevel level = mock(ServerLevel.class);
		controller.setLevel(level);
		UUID group = UUID.randomUUID();
		BlockPos following = new BlockPos(-1, 0, 0);
		CompoundTag saved = savedGroup(group, FIRST, List.of(FIRST, SECOND), 4);
		saved.getList("storageKeys", 10).add(new ControllerStorageKey(following, null).save());
		saved.getList("storageMemberKeys", 10).add(new ControllerStorageKey(following, null).save());
		saved.getList("baseIndexes", 3).add(IntTag.valueOf(7));
		saved.putInt("totalSlots", 7);
		controller.load(saved);
		IControllableStorage first = storage(FIRST, group, 9);
		IControllableStorage second = storage(SECOND, group, 9);
		IControllableStorage next = storage(following, null, 3);
		ItemStack marker = new ItemStack(Items.DIAMOND, 7);
		when(next.getStorageWrapper().getInventoryForInputOutput().getStackInSlot(0)).thenReturn(marker);
		try (var world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, FIRST, IControllableStorage.class)).thenReturn(Optional.of(first));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, SECOND, IControllableStorage.class)).thenReturn(Optional.of(second));
			world.when(() -> WorldHelper.getLoadedBlockEntity(level, following, IControllableStorage.class)).thenReturn(Optional.of(next));
			controller.onLoad();
			assertEquals(12, controller.getSlots());
			assertEquals(marker, controller.getStackInSlot(9));
			assertEquals(9, controller.getUpdateTag().getList("baseIndexes", 3).getInt(0));
			assertEquals(12, controller.getUpdateTag().getList("baseIndexes", 3).getInt(1));
			verify(second, never()).registerController(controller);
		}
	}

	private static IControllableStorage storage(BlockPos pos, @Nullable UUID group, int slots) {
		IControllableStorage storage = mock(IControllableStorage.class);
		IStorageWrapper wrapper = mock(IStorageWrapper.class);
		ITrackedContentsItemHandler inventory = mock(ITrackedContentsItemHandler.class);
		SettingsHandler settings = mock(SettingsHandler.class);
		when(storage.getStorageWrapper()).thenReturn(wrapper);
		when(storage.getControlledStorageBlockPos()).thenReturn(pos);
		when(storage.getControllerStorageKey()).thenReturn(new ControllerStorageKey(pos, group));
		when(wrapper.getInventoryForInputOutput()).thenReturn(inventory);
		when(inventory.getSlots()).thenReturn(slots);
		when(wrapper.getInventoryHandler()).thenReturn(mock(InventoryHandler.class));
		when(wrapper.getUpgradeHandler()).thenReturn(mock(UpgradeHandler.class));
		when(wrapper.getSettingsHandler()).thenReturn(settings);
		when(settings.getTypeCategory(MemorySettingsCategory.class)).thenReturn(mock(MemorySettingsCategory.class));
		return storage;
	}

	private static CompoundTag savedGroup(UUID group, BlockPos anchor, List<BlockPos> members, int slots) {
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
			return getHandlerFromIndex(0).getSlots();
		}
	}
}
