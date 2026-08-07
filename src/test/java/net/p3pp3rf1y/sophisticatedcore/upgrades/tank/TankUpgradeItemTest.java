package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TankUpgradeItemTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void canSwapUpgradeForRejectsSameTankWithPersistedContentsAboveTargetCapacity() {
		TankUpgradeItem tankUpgrade = mock(TankUpgradeItem.class, CALLS_REAL_METHODS);
		ItemStack upgradeStack = mock(ItemStack.class);
		CompoundTag tag = new CompoundTag();
		tag.put("contents", new FluidStack(Fluids.WATER, 40_001).writeToNBT(new CompoundTag()));
		when(upgradeStack.getItem()).thenReturn(tankUpgrade);
		when(upgradeStack.getTag()).thenReturn(tag);
		doReturn(40_000).when(tankUpgrade).getTankCapacity(any());

		UpgradeSlotChangeResult result = tankUpgrade.canSwapUpgradeFor(upgradeStack, 0, mock(IStorageWrapper.class), false);

		assertFalse(result.isSuccessful());
		TranslatableContents errorContents = assertInstanceOf(TranslatableContents.class, result.getErrorMessage().orElseThrow().getContents());
		assertArrayEquals(new Object[]{"1.1"}, errorContents.getArgs());
	}
}
