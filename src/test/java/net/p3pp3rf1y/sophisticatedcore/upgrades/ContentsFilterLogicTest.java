package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentsFilterLogicTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void matchesFilterRejectsEmptyStacksWhenMatchingContentsByMod() {
		ContentsFilterLogic filter = contentsFilter(PrimaryMatch.MOD);
		ItemStack diamond = new ItemStack(Items.DIAMOND);

		boolean emptyMatches = filter.matchesFilter(ItemStack.EMPTY);
		boolean diamondMatches = filter.matchesFilter(diamond);

		assertFalse(emptyMatches);
		assertTrue(diamondMatches);
	}

	@Test
	void matchesFilterPreservesExactItemContentsMatching() {
		ContentsFilterLogic filter = contentsFilter(PrimaryMatch.ITEM);
		ItemStack diamond = new ItemStack(Items.DIAMOND);
		ItemStack stone = new ItemStack(Items.STONE);

		boolean emptyMatches = filter.matchesFilter(ItemStack.EMPTY);
		boolean diamondMatches = filter.matchesFilter(diamond);
		boolean stoneMatches = filter.matchesFilter(stone);

		assertFalse(emptyMatches);
		assertFalse(diamondMatches);
		assertTrue(stoneMatches);
	}

	private ContentsFilterLogic contentsFilter(PrimaryMatch match) {
		InventoryHandler inventory = mock(InventoryHandler.class);
		ISlotTracker tracker = mock(ISlotTracker.class);
		when(inventory.getSlotTracker()).thenReturn(tracker);
		when(tracker.getFullStacks()).thenReturn(Set.of());
		when(tracker.getPartialStacks()).thenReturn(Set.of(ItemStackKey.of(new ItemStack(Items.STONE))));
		ItemStack upgrade = new ItemStack(Items.STICK);
		ContentsFilterLogic filter = new ContentsFilterLogic(upgrade, ignored -> {
		}, 1, () -> inventory, mock(MemorySettingsCategory.class));
		filter.setPrimaryMatch(match);
		filter.setDepositFilterType(ContentsFilterType.STORAGE);
		return filter;
	}
}
