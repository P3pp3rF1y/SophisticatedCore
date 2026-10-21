package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class PumpUpgradeWrapperTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
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
		when(upgradeItem.getInteractWithHandDefault()).thenReturn(false);
		when(upgradeItem.getInteractWithWorldDefault()).thenReturn(false);
		when(upgradeItem.getInteractWithFluidHandlersDefault()).thenReturn(true);

		ItemStack upgrade = mock(ItemStack.class);
		when(upgrade.getItem()).thenReturn(upgradeItem);
		IStorageWrapper storageWrapper = mock(IStorageWrapper.class);
		when(storageWrapper.getFluidHandler()).thenReturn(Optional.of(mock(IStorageFluidHandler.class)));
		return new PumpUpgradeWrapper(storageWrapper, upgrade, stack -> {
		});
	}
}
