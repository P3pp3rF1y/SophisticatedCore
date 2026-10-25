package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
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
		ItemStack mainHand = mock(ItemStack.class);
		ItemStack offHand = mock(ItemStack.class);
		when(mainHand.getCount()).thenReturn(1);
		when(offHand.getCount()).thenReturn(1);
		when(mainHand.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)).thenReturn(new LinkedStorageEndpointData(groupId, UUID.randomUUID()));
		when(offHand.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)).thenReturn(new LinkedStorageEndpointData(groupId, UUID.randomUUID()));
		when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(mainHand);
		when(player.getItemInHand(InteractionHand.OFF_HAND)).thenReturn(offHand);
		Level level = mock(Level.class);
		doReturn(List.of(player)).when(level).players();

		try (MockedStatic<CapabilityHelper> capability = mockStatic(CapabilityHelper.class)) {
			wrapper.tick(placed ? null : player, level, BlockPos.ZERO);
			capability.verifyNoInteractions();
		}
		verify(player).getItemInHand(InteractionHand.MAIN_HAND);
		verify(player).getItemInHand(InteractionHand.OFF_HAND);
		verify(player, never()).setItemInHand(any(), any());
	}

	@ParameterizedTest
	@CsvSource({"true, true", "true, false", "false, true", "false, false"})
	void tickTransfersOtherLinkedGroupsAndOrdinaryContainers(boolean input, boolean linked) throws ReflectiveOperationException {
		FluidStacksResourceHandler storageTank = new FluidStacksResourceHandler(1, 4000);
		FluidStacksResourceHandler heldTank = new FluidStacksResourceHandler(1, 4000);
		try (Transaction tx = Transaction.openRoot()) {
			(input ? heldTank : storageTank).insert(FluidResource.of(Fluids.WATER), 1000, tx);
			tx.commit();
		}
		IStorageWrapper storage = mock(IStorageWrapper.class);
		when(storage.getContentsUuid()).thenReturn(Optional.of(UUID.randomUUID()));
		when(storage.getFluidHandler()).thenReturn(Optional.of(mock(IStorageFluidHandler.class, delegatesTo(storageTank))));
		PumpUpgradeWrapper wrapper = createWrapper(storage, true, input);
		ItemStack held = mock(ItemStack.class);
		when(held.getCount()).thenReturn(1);
		if (linked) {
			when(held.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)).thenReturn(new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));
		}
		Player player = mock(Player.class);
		when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(held);
		Level level = mock(Level.class);

		try (MockedStatic<CapabilityHelper> capability = mockStatic(CapabilityHelper.class)) {
			capability.when(() -> CapabilityHelper.getFromFluidHandler(any(ItemAccess.class), any(), eq(false)))
					.thenAnswer(invocation -> ((Function<ResourceHandler<FluidResource>, Boolean>) invocation.getArgument(1)).apply(heldTank));
			wrapper.tick(player, level, BlockPos.ZERO);
		}

		assertEquals(input ? 1000 : 0, storageTank.getAmountAsInt(0));
		assertEquals(input ? 0 : 1000, heldTank.getAmountAsInt(0));
		verify(player).setItemInHand(InteractionHand.MAIN_HAND, held);
	}

	@Test
	void tickSkipsDeniedNeighborAndChecksLaterNeighbor() throws ReflectiveOperationException {
		PumpUpgradeWrapper wrapper = createWrapper();
		Player player = mock(Player.class);
		Level level = mock(Level.class);
		BlockPos pumpPos = new BlockPos(12, 34, 56);
		BlockPos deniedPos = pumpPos.offset(Direction.NORTH.getUnitVec3i());

		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.playerMayInteract(eq(player), eq(level), any(BlockPos.class))).thenReturn(true);
			world.when(() -> WorldHelper.playerMayInteract(player, level, deniedPos)).thenReturn(false);
			world.when(() -> WorldHelper.getBlockEntity(eq(level), any(BlockPos.class))).thenReturn(Optional.empty());

			wrapper.tick(player, level, pumpPos);

			world.verify(() -> WorldHelper.getBlockEntity(level, deniedPos), never());
			world.verify(() -> WorldHelper.getBlockEntity(level, pumpPos.offset(Direction.SOUTH.getUnitVec3i())));
		}
	}

	@Test
	void tickScansAttachedHandlersWithoutPlayerPermissionWhenHostIsPlayerless() throws ReflectiveOperationException {
		assertUngatedAdjacentAccess(null);
	}

	@Test
	void tickScansAttachedHandlersWithoutPlayerPermissionWhenHostIsNotPlayer() throws ReflectiveOperationException {
		assertUngatedAdjacentAccess(mock(Entity.class));
	}

	private static void assertUngatedAdjacentAccess(@Nullable Entity entity) throws ReflectiveOperationException {
		PumpUpgradeWrapper wrapper = createWrapper();
		Level level = mock(Level.class);
		BlockPos pumpPos = new BlockPos(12, 34, 56);

		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getBlockEntity(eq(level), any(BlockPos.class))).thenReturn(Optional.empty());

			wrapper.tick(entity, level, pumpPos);

			world.verify(() -> WorldHelper.playerMayInteract(any(Player.class), eq(level), any(BlockPos.class)), never());
			world.verify(() -> WorldHelper.getBlockEntity(level, pumpPos.offset(Direction.NORTH.getUnitVec3i())));
		}
	}

	private static PumpUpgradeWrapper createWrapper() throws ReflectiveOperationException {
		IStorageWrapper storageWrapper = mock(IStorageWrapper.class);
		when(storageWrapper.getFluidHandler()).thenReturn(Optional.of(mock(IStorageFluidHandler.class)));
		return createWrapper(storageWrapper, false, true);
	}

	private static PumpUpgradeWrapper createWrapper(IStorageWrapper storageWrapper, boolean interactWithHand, boolean input)
			throws ReflectiveOperationException {
		ModConfigSpec.Builder configBuilder = new ModConfigSpec.Builder();
		PumpUpgradeConfig config = new PumpUpgradeConfig(configBuilder);
		loadDefaultConfig(configBuilder.build());
		PumpUpgradeItem upgradeItem = mock(PumpUpgradeItem.class);
		when(upgradeItem.getPumpUpgradeConfig()).thenReturn(config);
		when(upgradeItem.getInteractWithHandDefault()).thenReturn(interactWithHand);
		when(upgradeItem.getInteractWithWorldDefault()).thenReturn(false);
		when(upgradeItem.getInteractWithFluidHandlersDefault()).thenReturn(!interactWithHand);

		ItemStack upgrade = mock(ItemStack.class);
		when(upgrade.getItem()).thenReturn(upgradeItem);
		when(upgrade.getOrDefault(anyDataComponentSupplier(), any())).thenAnswer(invocation -> invocation.getArgument(1));
		when(upgrade.getOrDefault(ModCoreDataComponents.IS_INPUT, true)).thenReturn(input);

		return new PumpUpgradeWrapper(storageWrapper, upgrade, stack -> {
		});
	}

	private static void loadDefaultConfig(ModConfigSpec configSpec) throws ReflectiveOperationException {
		CommentedConfig config = CommentedConfig.inMemory();
		configSpec.correct(config);
		Class<?> loadedConfigClass = Class.forName("net.neoforged.fml.config.LoadedConfig");
		Constructor<?> constructor = loadedConfigClass.getDeclaredConstructor(CommentedConfig.class, Path.class, ModConfig.class);
		constructor.setAccessible(true);
		configSpec.acceptConfig((IConfigSpec.ILoadedConfig) constructor.newInstance(config, null, null));
	}

	@SuppressWarnings("unchecked")
	private static <T> Supplier<? extends DataComponentType<? extends T>> anyDataComponentSupplier() {
		return (Supplier<? extends DataComponentType<? extends T>>) any(Supplier.class);
	}
}
