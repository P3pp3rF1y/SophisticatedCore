package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkPendingCraftData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkPendingCraftPlan;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerItem;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerTargetData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnderLinkerClearRecipeTest {
	@Test
	void matchesAndAssembleAcceptBoundEnderLinker() {
		EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);
		Mockito.when(linkerItem.components()).thenReturn(DataComponentMap.EMPTY);
		EnderLinkerClearRecipe recipe = new EnderLinkerClearRecipe(CraftingBookCategory.MISC);
		ItemStack boundLinker = new ItemStack(linkerItem, 2);
		boundLinker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, new EnderLinkerTargetData(UUID.randomUUID(), Component.empty()));
		CraftingInput boundInput = CraftingInput.of(2, 1, List.of(boundLinker, ItemStack.EMPTY));

		assertTrue(recipe.matches(boundInput, null));
		ItemStack result = recipe.assemble(boundInput, Mockito.mock(HolderLookup.Provider.class));
		assertTrue(result.is(linkerItem));
		assertEquals(1, result.getCount());
		assertFalse(result.has(ModCoreDataComponents.ENDER_LINKER_TARGET));
	}

	@Test
	void matchesRejectsBlankEnderLinker() {
		EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);
		Mockito.when(linkerItem.components()).thenReturn(DataComponentMap.EMPTY);
		EnderLinkerClearRecipe recipe = new EnderLinkerClearRecipe(CraftingBookCategory.MISC);

		assertFalse(recipe.matches(CraftingInput.of(1, 1, List.of(new ItemStack(linkerItem))), null));
	}

	@Test
	void matchesAndAssembleAcceptPendingEnderLinker() {
		EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);
		Mockito.when(linkerItem.components()).thenReturn(DataComponentMap.EMPTY);
		EnderLinkerClearRecipe recipe = new EnderLinkerClearRecipe(CraftingBookCategory.MISC);
		ItemStack pendingLinker = new ItemStack(linkerItem);
		pendingLinker.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT,
				new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, UUID.randomUUID()));
		CraftingInput pendingInput = CraftingInput.of(1, 1, List.of(pendingLinker));

		assertTrue(recipe.matches(pendingInput, null));
		ItemStack result = recipe.assemble(pendingInput, Mockito.mock(HolderLookup.Provider.class));
		assertTrue(result.is(linkerItem));
		assertFalse(result.has(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT));
	}

	@Test
	void clearPendingCraftClaimConsumesActiveClaim() {
		UUID claimId = UUID.randomUUID();
		EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);
		Mockito.when(linkerItem.components()).thenReturn(DataComponentMap.EMPTY);
		ItemStack pendingLinker = new ItemStack(linkerItem);
		pendingLinker.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, claimId));
		Container inventory = Mockito.mock(Container.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		Mockito.when(inventory.getContainerSize()).thenReturn(1);
		Mockito.when(inventory.getItem(0)).thenReturn(pendingLinker);
		Mockito.when(savedData.manager()).thenReturn(manager);
		Mockito.when(manager.consumeActivePendingCraftClaim(claimId)).thenReturn(true);

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);

			EnderLinkerClearRecipe.clearPendingCraftClaim(level, inventory);
		}

		Mockito.verify(manager).consumeActivePendingCraftClaim(claimId);
	}

	@Test
	void matchesRejectsAdditionalInput() {
		EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);
		Mockito.when(linkerItem.components()).thenReturn(DataComponentMap.EMPTY);
		EnderLinkerClearRecipe recipe = new EnderLinkerClearRecipe(CraftingBookCategory.MISC);
		ItemStack boundLinker = new ItemStack(linkerItem);
		boundLinker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, new EnderLinkerTargetData(UUID.randomUUID(), Component.empty()));

		assertFalse(recipe.matches(CraftingInput.of(2, 1, List.of(boundLinker, new ItemStack(Items.DIRT))), null));
	}
}
