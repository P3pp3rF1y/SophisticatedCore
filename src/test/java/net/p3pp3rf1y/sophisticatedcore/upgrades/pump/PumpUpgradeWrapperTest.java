package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class PumpUpgradeWrapperTest {
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
		PumpUpgradeWrapper wrapper = createWrapper();
		Level level = mock(Level.class);
		BlockPos pumpPos = new BlockPos(12, 34, 56);

		try (MockedStatic<WorldHelper> world = mockStatic(WorldHelper.class)) {
			world.when(() -> WorldHelper.getBlockEntity(eq(level), any(BlockPos.class))).thenReturn(Optional.empty());

			wrapper.tick(null, level, pumpPos);

			world.verify(() -> WorldHelper.playerMayInteract(any(Player.class), eq(level), any(BlockPos.class)), never());
			world.verify(() -> WorldHelper.getBlockEntity(level, pumpPos.offset(Direction.NORTH.getUnitVec3i())));
		}
	}

	private static PumpUpgradeWrapper createWrapper() throws ReflectiveOperationException {
		ModConfigSpec.Builder configBuilder = new ModConfigSpec.Builder();
		PumpUpgradeConfig config = new PumpUpgradeConfig(configBuilder);
		loadDefaultConfig(configBuilder.build());
		PumpUpgradeItem upgradeItem = mock(PumpUpgradeItem.class);
		when(upgradeItem.getPumpUpgradeConfig()).thenReturn(config);
		when(upgradeItem.getInteractWithHandDefault()).thenReturn(false);
		when(upgradeItem.getInteractWithWorldDefault()).thenReturn(false);
		when(upgradeItem.getInteractWithFluidHandlersDefault()).thenReturn(true);

		ItemStack upgrade = mock(ItemStack.class);
		when(upgrade.getItem()).thenReturn(upgradeItem);
		when(upgrade.getOrDefault(anyDataComponentSupplier(), any())).thenAnswer(invocation -> invocation.getArgument(1));

		IStorageWrapper storageWrapper = mock(IStorageWrapper.class);
		when(storageWrapper.getFluidHandler()).thenReturn(Optional.of(mock(IStorageFluidHandler.class)));
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
