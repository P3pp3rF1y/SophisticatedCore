package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkPendingCraftData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkPendingCraftPlan;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerItem;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerTargetData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnderLinkerClearRecipeTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void matchesAndAssembleAcceptBoundEnderLinker() {
		EnderLinkerItem linkerItem = linkerItem();
		ItemStack boundLinker = linkerStack(2);
		LinkedStorageStackData.setLinkerTarget(boundLinker, new EnderLinkerTargetData(UUID.randomUUID(), Component.empty()));
		EnderLinkerClearRecipe recipe = recipe();

		assertTrue(recipe.matches(crafting(boundLinker, ItemStack.EMPTY), null));
		ItemStack result = recipe.assemble(crafting(boundLinker, ItemStack.EMPTY), Mockito.mock(RegistryAccess.class));
		assertTrue(result.is(Items.BLAZE_ROD));
		assertEquals(1, result.getCount());
		assertFalse(LinkedStorageStackData.getLinkerTarget(result) != null);
	}

	@Test
	void matchesRejectsBlankEnderLinker() {
		assertFalse(recipe().matches(crafting(linkerStack()), null));
	}

	@Test
	void matchesAndAssembleAcceptPendingEnderLinker() {
		EnderLinkerItem linkerItem = linkerItem();
		ItemStack pendingLinker = linkerStack();
		LinkedStorageStackData.setPendingCraft(pendingLinker, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, UUID.randomUUID()));

		assertTrue(recipe().matches(crafting(pendingLinker), null));
		ItemStack result = recipe().assemble(crafting(pendingLinker), Mockito.mock(RegistryAccess.class));
		assertTrue(result.is(Items.BLAZE_ROD));
		assertFalse(LinkedStorageStackData.getPendingCraft(result) != null);
	}

	@Test
	void clearPendingCraftClaimConsumesActiveClaim() {
		UUID claimId = UUID.randomUUID();
		ItemStack pendingLinker = linkerStack();
		LinkedStorageStackData.setPendingCraft(pendingLinker, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, claimId));
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
		ItemStack boundLinker = linkerStack();
		LinkedStorageStackData.setLinkerTarget(boundLinker, new EnderLinkerTargetData(UUID.randomUUID(), Component.empty()));
		assertFalse(recipe().matches(crafting(boundLinker, new ItemStack(Items.DIRT)), null));
	}

	private static EnderLinkerClearRecipe recipe() {
		return new EnderLinkerClearRecipe(new net.minecraft.resources.ResourceLocation("sophisticatedcore", "ender_linker_clear_test"),
				CraftingBookCategory.MISC);
	}

	private static EnderLinkerItem linkerItem() {
		EnderLinkerItem item = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);
		Mockito.doReturn(Items.BLAZE_ROD).when(item).asItem();
		return item;
	}

	private static ItemStack linkerStack() {
		return linkerStack(1);
	}

	private static ItemStack linkerStack(int count) {
		ItemStack stack = Mockito.spy(new ItemStack(Items.BLAZE_ROD, count));
		Mockito.doReturn(linkerItem()).when(stack).getItem();
		return stack;
	}

	private static CraftingContainer crafting(ItemStack... stacks) {
		CraftingContainer input = Mockito.mock(CraftingContainer.class);
		Mockito.when(input.getContainerSize()).thenReturn(stacks.length);
		for (int slot = 0; slot < stacks.length; slot++) {
			Mockito.when(input.getItem(slot)).thenReturn(stacks[slot]);
		}
		return input;
	}
}
