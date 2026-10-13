package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
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
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		for (var item : Set.of(Items.STICK, Items.STONE, Items.DIAMOND)) {
			item.builtInRegistryHolder().bindComponents(components);
		}
	}

	@Test
	void matchesFilterRejectsEmptyStacksAndResourcesWhenMatchingContentsByMod() {
		ContentsFilterLogic filter = contentsFilter(PrimaryMatch.MOD);
		ItemResource emptyResource = ItemResource.of(ItemStack.EMPTY);
		ItemStack diamond = new ItemStack(Items.DIAMOND);

		boolean emptyStackMatches = filter.matchesFilter(ItemStack.EMPTY);
		boolean emptyResourceMatches = filter.matchesFilter(emptyResource);
		boolean diamondMatches = filter.matchesFilter(diamond);

		assertFalse(emptyStackMatches);
		assertFalse(emptyResourceMatches);
		assertTrue(diamondMatches);
	}

	@Test
	void matchesFilterPreservesExactItemContentsMatching() {
		ContentsFilterLogic filter = contentsFilter(PrimaryMatch.ITEM);
		ItemStack diamond = new ItemStack(Items.DIAMOND);
		ItemStack stone = new ItemStack(Items.STONE);

		boolean emptyStackMatches = filter.matchesFilter(ItemStack.EMPTY);
		boolean diamondMatches = filter.matchesFilter(diamond);
		boolean stoneMatches = filter.matchesFilter(stone);

		assertFalse(emptyStackMatches);
		assertFalse(diamondMatches);
		assertTrue(stoneMatches);
	}

	private ContentsFilterLogic contentsFilter(PrimaryMatch match) {
		InventoryHandler inventory = mock(InventoryHandler.class);
		ISlotTracker tracker = mock(ISlotTracker.class);
		when(inventory.getSlotTracker()).thenReturn(tracker);
		when(tracker.getFullStacks()).thenReturn(Set.of());
		when(tracker.getPartialStacks()).thenReturn(Set.of(ItemStackKey.of(new ItemStack(Items.STONE))));
		FilterAttributes attributes = new FilterAttributes(Set.of(), true, false, false, match, true, ItemContainerContents.EMPTY, true, false);
		return new ContentsFilterLogic(new ItemStack(Items.STICK), ignored -> {
		}, 1, () -> inventory, mock(MemorySettingsCategory.class), ModCoreDataComponents.FILTER_ATTRIBUTES) {
			@Override
			protected FilterAttributes getAttributes() {
				return attributes;
			}
		};
	}
}
