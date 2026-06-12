package net.p3pp3rf1y.sophisticatedcore.upgrades.compacting;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static net.p3pp3rf1y.sophisticatedcore.HelperAssertions.assertStackEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompactingUpgradeWrapperTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void basicUpgradeCompactsConfiguredTwoByTwoShape() {
		ItemStack stack = new ItemStack(Items.CHARCOAL);
		CompactingUpgradeItem upgradeItem = getUpgradeItemWithConfiguredShape(false, 2, 4);

		Optional<CompactingUpgradeWrapper.CompactingDefinition> compactingDefinition = CompactingUpgradeWrapper.getCompactingDefinition(stack, upgradeItem, false);

		verify(upgradeItem).getConfiguredCompactingResult(any(ItemStack.class), eq(2), eq(2));
		assertTrue(compactingDefinition.isPresent(), "Basic compacting upgrade should compact configured 2x2-compatible shape");
		CompactingUpgradeWrapper.CompactingDefinition result = compactingDefinition.orElseThrow();
		assertEquals(4, result.count());
		assertStackEquals(new ItemStack(Items.COAL), result.result().getResult(), "Basic compacting upgrade should compact configured 2x2-compatible shape");
	}

	@Test
	void basicUpgradeDoesNotCompactConfiguredThreeByThreeShape() {
		ItemStack stack = new ItemStack(Items.CHARCOAL);
		CompactingUpgradeItem upgradeItem = getUpgradeItemWithConfiguredShape(false, 3, 8);

		Optional<CompactingUpgradeWrapper.CompactingDefinition> compactingDefinition = CompactingUpgradeWrapper.getCompactingDefinition(stack, upgradeItem, false);

		verify(upgradeItem).getConfiguredCompactingResult(any(ItemStack.class), eq(2), eq(2));
		assertFalse(compactingDefinition.isPresent(), "Basic compacting upgrade should not compact configured 3x3 shape");
	}

	@Test
	void advancedUpgradeCompactsConfiguredThreeByThreeShape() {
		ItemStack stack = new ItemStack(Items.CHARCOAL);
		CompactingUpgradeItem upgradeItem = getUpgradeItemWithConfiguredShape(true, 3, 8);

		Optional<CompactingUpgradeWrapper.CompactingDefinition> compactingDefinition = CompactingUpgradeWrapper.getCompactingDefinition(stack, upgradeItem, false);

		verify(upgradeItem).getConfiguredCompactingResult(any(ItemStack.class), eq(3), eq(3));
		assertTrue(compactingDefinition.isPresent(), "Advanced compacting upgrade should compact configured 3x3 shape");
		CompactingUpgradeWrapper.CompactingDefinition result = compactingDefinition.orElseThrow();
		assertEquals(8, result.count());
		assertStackEquals(new ItemStack(Items.COAL), result.result().getResult(), "Advanced compacting upgrade should compact configured 3x3 shape");
	}

	private static CompactingUpgradeItem getUpgradeItemWithConfiguredShape(boolean shouldCompactThreeByThree, int shapeSize, int count) {
		CompactingUpgradeItem upgradeItem = mock(CompactingUpgradeItem.class);
		when(upgradeItem.shouldCompactThreeByThree()).thenReturn(shouldCompactThreeByThree);
		when(upgradeItem.getConfiguredCompactingResult(any(ItemStack.class), anyInt(), anyInt())).thenReturn(Optional.empty());
		when(upgradeItem.getConfiguredCompactingResult(any(ItemStack.class), eq(shapeSize), eq(shapeSize))).thenReturn(getConfiguredCompactingResult(count));
		return upgradeItem;
	}

	private static Optional<CompactingUpgradeConfig.CompactingDefinition> getConfiguredCompactingResult(int count) {
		return Optional.of(new CompactingUpgradeConfig.CompactingDefinition(new RecipeHelper.CompactingResult(new ItemStack(Items.COAL), Collections.emptyList()), count));
	}
}
