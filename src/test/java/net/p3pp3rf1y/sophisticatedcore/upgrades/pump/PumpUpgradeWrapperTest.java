package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PumpUpgradeWrapperTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@ParameterizedTest
	@CsvSource({"true, true", "true, false", "false, true", "false, false"})
	void tickSkipsOwnLinkedGroupInBothHands(boolean input, boolean placed) throws ReflectiveOperationException {
		UUID groupId = UUID.randomUUID();
		IStorageWrapper storage = mock(IStorageWrapper.class);
		when(storage.getContentsUuid()).thenReturn(Optional.of(groupId));
		when(storage.getFluidHandler()).thenReturn(Optional.of(mock(IStorageFluidHandler.class)));
		PumpUpgradeWrapper wrapper = createWrapper(storage, true, input);
		Player player = mock(Player.class);
		ItemStack mainHand = linkedHand(groupId);
		ItemStack offHand = linkedHand(groupId);
		when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(mainHand);
		when(player.getItemInHand(InteractionHand.OFF_HAND)).thenReturn(offHand);
		Level level = mock(Level.class);
		doReturn(List.of(player)).when(level).players();

		wrapper.tick(placed ? null : player, level, BlockPos.ZERO);

		verify(player).getItemInHand(InteractionHand.MAIN_HAND);
		verify(player).getItemInHand(InteractionHand.OFF_HAND);
		verify(mainHand, never()).getCapability(any());
		verify(offHand, never()).getCapability(any());
		verify(player, never()).setItemInHand(any(), any());
	}

	private static ItemStack linkedHand(UUID groupId) {
		ItemStack hand = mock(ItemStack.class);
		when(hand.getCount()).thenReturn(1);
		CompoundTag linked = new CompoundTag();
		linked.put("endpoint", new LinkedStorageEndpointData(groupId, UUID.randomUUID()).save());
		when(hand.getTagElement("linked_storage")).thenReturn(linked);
		return hand;
	}

	@Test
	void deniedClaimSkipsAttachedFluidHandlerButScansLaterNeighbors() throws ReflectiveOperationException {
		PumpUpgradeWrapper wrapper = createWrapper();
		Player player = mock(Player.class);
		Level level = mock(Level.class);
		BlockPos pumpPos = new BlockPos(12, 34, 56);
		BlockPos deniedPos = pumpPos.relative(Direction.NORTH);

		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.playerMayInteract(eq(player), eq(level), any(BlockPos.class))).thenReturn(true);
			world.when(() -> WorldHelper.playerMayInteract(player, level, deniedPos)).thenReturn(false);
			world.when(() -> WorldHelper.getBlockEntity(eq(level), any(BlockPos.class))).thenReturn(Optional.empty());

			wrapper.tick(player, level, pumpPos);

			for (Direction dir : Direction.values()) {
				world.verify(() -> WorldHelper.playerMayInteract(player, level, pumpPos.relative(dir)));
			}
			world.verify(() -> WorldHelper.getBlockEntity(level, deniedPos), never());
			world.verify(() -> WorldHelper.getBlockEntity(level, pumpPos.relative(Direction.SOUTH)));
		}
	}

	@Test
	void placedBackpackUsesAttachedFluidHandlersWithoutPlayerCheck() throws ReflectiveOperationException {
		assertPlayerlessScan(null);
	}

	@Test
	void nonPlayerHostUsesAttachedFluidHandlersWithoutPlayerCheck() throws ReflectiveOperationException {
		assertPlayerlessScan(mock(Entity.class));
	}

	private static void assertPlayerlessScan(Entity entity) throws ReflectiveOperationException {
		PumpUpgradeWrapper wrapper = createWrapper();
		Level level = mock(Level.class);
		BlockPos pumpPos = new BlockPos(12, 34, 56);

		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getBlockEntity(eq(level), any(BlockPos.class))).thenReturn(Optional.empty());

			wrapper.tick(entity, level, pumpPos);

			world.verify(() -> WorldHelper.playerMayInteract(nullable(Player.class), eq(level), any(BlockPos.class)), never());
			world.verify(() -> WorldHelper.getBlockEntity(level, pumpPos.relative(Direction.NORTH)));
		}
	}

	private static PumpUpgradeWrapper createWrapper() throws ReflectiveOperationException {
		IStorageWrapper storageWrapper = mock(IStorageWrapper.class);
		when(storageWrapper.getFluidHandler()).thenReturn(Optional.of(mock(IStorageFluidHandler.class)));
		return createWrapper(storageWrapper, false, true);
	}

	private static PumpUpgradeWrapper createWrapper(IStorageWrapper storageWrapper, boolean interactWithHand, boolean input)
			throws ReflectiveOperationException {
		ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
		PumpUpgradeConfig config = new PumpUpgradeConfig(configBuilder);
		configBuilder.build();
		ForgeConfigSpec.IntValue filterSlots = mock(ForgeConfigSpec.IntValue.class);
		when(filterSlots.get()).thenReturn(4);
		java.lang.reflect.Field filterSlotsField = PumpUpgradeConfig.class.getDeclaredField("filterSlots");
		filterSlotsField.setAccessible(true);
		filterSlotsField.set(config, filterSlots);
		PumpUpgradeItem upgradeItem = mock(PumpUpgradeItem.class);
		when(upgradeItem.getPumpUpgradeConfig()).thenReturn(config);
		when(upgradeItem.getInteractWithHandDefault()).thenReturn(interactWithHand);
		when(upgradeItem.getInteractWithWorldDefault()).thenReturn(false);
		when(upgradeItem.getInteractWithFluidHandlersDefault()).thenReturn(!interactWithHand);

		ItemStack upgrade = mock(ItemStack.class);
		when(upgrade.getItem()).thenReturn(upgradeItem);
		CompoundTag upgradeTag = new CompoundTag();
		upgradeTag.putBoolean("input", input);
		when(upgrade.getTag()).thenReturn(upgradeTag);
		return new PumpUpgradeWrapper(storageWrapper, upgrade, stack -> {
		});
	}
}
