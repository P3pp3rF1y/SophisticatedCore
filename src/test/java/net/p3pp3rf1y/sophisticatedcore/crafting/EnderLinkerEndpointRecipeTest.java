package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ActivePendingCraftClaim;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkPendingCraftData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkPendingCraftPlan;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerItem;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerTargetData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageItemEndpointAdapter;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageVirtualHost;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointAdapters;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostDescriptor;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnderLinkerEndpointRecipeTest {
	@Test
	void pendingCraftPlanRejectsUnknownSerializedName() {
		assertThrows(IllegalStateException.class, () -> EnderLinkPendingCraftPlan.fromSerializedName("unknown"));
	}

	@Test
	void getCraftingDiagnosticDoesNotReportIncompleteLinkerRecipeAsError() {
		assertTrue(
				EnderLinkerEndpointRecipe.getCraftingDiagnostic(Mockito.mock(ServerLevel.class), new SimpleContainer(new ItemStack(linkerItem()))).isEmpty());
	}

	@Test
	void getCraftingDiagnosticDoesNotReportIncompatibleStorageAsError() {
		ItemStack linker = new ItemStack(linkerItem());
		linker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, new EnderLinkerTargetData(UUID.randomUUID(), Component.empty()));

		assertTrue(EnderLinkerEndpointRecipe.getCraftingDiagnostic(Mockito.mock(ServerLevel.class), new SimpleContainer(linker, new ItemStack(Items.DIRT)))
				.isEmpty());
	}

	@Test
	void getCraftingDiagnosticDoesNotReportValidLinkerClearRecipeAsError() {
		ItemStack linker = new ItemStack(linkerItem());
		linker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, new EnderLinkerTargetData(UUID.randomUUID(), Component.empty()));

		assertTrue(EnderLinkerEndpointRecipe.getCraftingDiagnostic(Mockito.mock(ServerLevel.class), new SimpleContainer(linker)).isEmpty());
	}

	@Test
	void getCraftingDiagnosticReportsAlreadyLinkedEndpointInsteadOfGenericCraftingInputs() {
		ItemStack linker = new ItemStack(linkerItem());
		linker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, new EnderLinkerTargetData(UUID.randomUUID(), Component.empty()));
		ItemStack endpoint = new ItemStack(Items.STICK);
		endpoint.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));

		EnderLinkerEndpointRecipe.CraftingDiagnostic diagnostic = EnderLinkerEndpointRecipe
				.getCraftingDiagnostic(Mockito.mock(ServerLevel.class), new SimpleContainer(linker, endpoint)).orElseThrow();

		assertEquals(1, diagnostic.slot());
		assertEquals(EnderLinkerEndpointRecipe.CraftingDiagnostic.Failure.ALREADY_LINKED, diagnostic.failure());
	}

	@Test
	void issueCraftClaimReissuesExpiredInFlightClaim() {
		UUID expiredClaimId = UUID.randomUUID();
		ItemStack result = new ItemStack(Items.BLAZE_ROD);
		result.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, expiredClaimId));
		Player player = Mockito.mock(Player.class);
		Mockito.when(player.getUUID()).thenReturn(UUID.randomUUID());

		EnderLinkerEndpointRecipe.issueCraftClaim(player, result);

		assertNotNull(result.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT).claimId());
		assertNotEquals(expiredClaimId, result.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT).claimId());
	}

	@Test
	void issueCraftClaimSharesInFlightClaimAcrossDuplicateCallbacks() {
		ItemStack result = new ItemStack(Items.BLAZE_ROD);
		result.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, null));
		ItemStack callbackCopy = result.copy();
		Player player = Mockito.mock(Player.class);
		Mockito.when(player.getUUID()).thenReturn(UUID.randomUUID());

		EnderLinkerEndpointRecipe.issueCraftClaim(player, result);
		EnderLinkerEndpointRecipe.issueCraftClaim(player, callbackCopy);

		assertEquals(result.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT).claimId(),
				callbackCopy.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT).claimId());
	}

	@Test
	void completeCraftUsesPendingLinkerAsSecondaryCraftInput() {
		UUID groupId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ILinkedStorageItemEndpointAdapter adapter = new TestEndpointAdapter();
		LinkedStorageEndpointAdapters.register(adapter);
		Mockito.when(manager.createGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(groupId);
		Mockito.when(manager.usesHostFactory(groupId, adapter.factoryId())).thenReturn(true);
		AtomicReference<ActivePendingCraftClaim> activeClaim = new AtomicReference<>();
		Mockito.doAnswer(invocation -> {
			activeClaim.set(invocation.getArgument(0));
			return null;
		}).when(manager).activatePendingCraftClaim(Mockito.any());
		Mockito.when(manager.getActivePendingCraftClaim(Mockito.any()))
				.thenAnswer(invocation -> Optional.ofNullable(activeClaim.get()).filter(claim -> claim.claimId().equals(invocation.getArgument(0))));
		Mockito.when(manager.consumeActivePendingCraftClaim(Mockito.any())).thenReturn(true);
		Mockito.when(manager.getHostDescriptor(groupId)).thenReturn(Optional.of(new LinkedStorageHostDescriptor(adapter.factoryId(), new CompoundTag())));
		mockLinkedStorageHost(manager, groupId);
		Mockito.when(savedData.manager()).thenReturn(manager);

		EnderLinkerItem linkerItem = linkerItem();
		ItemStack linker = new ItemStack(linkerItem, 3);
		ItemStack firstEndpoint = new ItemStack(Items.STICK);
		EnderLinkerEndpointRecipe recipe = new EnderLinkerEndpointRecipe(CraftingBookCategory.MISC);
		CraftingInput firstInput = CraftingInput.of(2, 1, List.of(linker, firstEndpoint));
		Player player = player();
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);
			assertTrue(recipe.matches(firstInput, null));
			ItemStack firstResult = recipe.assemble(firstInput, Mockito.mock(HolderLookup.Provider.class));
			assertTrue(firstResult.is(linkerItem));
			assertNotNull(firstResult.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT));
			assertTrue(EnderLinkerItem.hasBoundPresentation(firstResult));
			assertFalse(firstResult.has(ModCoreDataComponents.ENDER_LINKER_TARGET));
			EnderLinkerEndpointRecipe.issueCraftClaim(player, firstResult);
			ItemStack unclaimedEventResult = firstResult.copy();
			unclaimedEventResult.remove(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
			assertTrue(EnderLinkerEndpointRecipe.completeCraft(level, player, new SimpleContainer(linker, firstEndpoint), unclaimedEventResult));
			Mockito.verify(manager).createGroup(Mockito.eq(player.getUUID()), Mockito.any(), Mockito.any(), Mockito.any());
			assertEquals(groupId, firstEndpoint.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT).groupId());
			assertEquals(3, linker.getCount());
			assertTrue(recipe.getRemainingItems(firstInput).get(0).isEmpty());
			assertEquals(groupId, recipe.getRemainingItems(firstInput).get(1).get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT).groupId());

			ItemStack secondEndpoint = new ItemStack(Items.STICK);
			CraftingInput secondInput = CraftingInput.of(2, 1, List.of(firstResult, secondEndpoint));
			ItemStack secondResult = recipe.assemble(secondInput, Mockito.mock(HolderLookup.Provider.class));
			EnderLinkerEndpointRecipe.issueCraftClaim(player, secondResult);
			assertTrue(EnderLinkerEndpointRecipe.completeCraft(level, player, new SimpleContainer(firstResult, secondEndpoint), secondResult));
			assertTrue(EnderLinkerEndpointRecipe.finalizePendingCraftResult(level, secondResult));
			assertEquals(groupId, firstResult.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupId());
			assertEquals(groupId, secondResult.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT).groupId());
			assertEquals(1, firstResult.getCount());
		}
	}

	@Test
	void finalizeTransferredCraftResultResolvesDeliveredPendingLinkerInStorage() {
		UUID groupId = UUID.randomUUID();
		UUID endpointId = UUID.randomUUID();
		UUID claimId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		IItemHandler storage = Mockito.mock(IItemHandler.class);
		Inventory playerInventory = Mockito.mock(Inventory.class);
		ItemStack transferredStack = new ItemStack(linkerItem());
		ItemStack storedStack = transferredStack.copy();
		EnderLinkPendingCraftData pendingCraft = new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, claimId);
		transferredStack.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, pendingCraft);
		storedStack.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, pendingCraft);
		Mockito.when(manager.getActivePendingCraftClaim(claimId))
				.thenReturn(Optional.of(new ActivePendingCraftClaim(claimId, groupId, endpointId, EnderLinkPendingCraftPlan.CREATE_PRIMARY)));
		Mockito.when(manager.isEndpointMember(groupId, endpointId)).thenReturn(true);
		Mockito.when(manager.consumeActivePendingCraftClaim(claimId)).thenReturn(true);
		mockLinkedStorageHost(manager, groupId);
		Mockito.when(savedData.manager()).thenReturn(manager);
		Mockito.when(storage.getSlots()).thenReturn(1);
		Mockito.when(storage.getStackInSlot(0)).thenReturn(storedStack);

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);

			EnderLinkerEndpointRecipe.finalizeTransferredCraftResult(level, transferredStack, storage, playerInventory);
		}

		assertEquals(groupId, storedStack.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupId());
		assertFalse(storedStack.has(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT));
		Mockito.verify(manager).consumeActivePendingCraftClaim(claimId);
	}

	@Test
	void finalizeDeliveredCraftResultResolvesPendingEndpointInPlayerInventory() {
		UUID groupId = UUID.randomUUID();
		UUID endpointId = UUID.randomUUID();
		UUID claimId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Inventory playerInventory = Mockito.mock(Inventory.class);
		ItemStack craftedStack = new ItemStack(Items.STICK);
		ItemStack inventoryStack = craftedStack.copy();
		EnderLinkPendingCraftData pendingCraft = new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.ADD_SECONDARY, claimId);
		craftedStack.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, pendingCraft);
		inventoryStack.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.ADD_SECONDARY, null));
		Mockito.when(manager.getActivePendingCraftClaim(claimId))
				.thenReturn(Optional.of(new ActivePendingCraftClaim(claimId, groupId, endpointId, EnderLinkPendingCraftPlan.ADD_SECONDARY)));
		Mockito.when(manager.isEndpointMember(groupId, endpointId)).thenReturn(true);
		Mockito.when(manager.usesHostFactory(groupId, TestEndpointAdapter.FACTORY_ID)).thenReturn(true);
		Mockito.when(manager.consumeActivePendingCraftClaim(claimId)).thenReturn(true);
		Mockito.when(savedData.manager()).thenReturn(manager);
		Mockito.when(playerInventory.getContainerSize()).thenReturn(1);
		Mockito.when(playerInventory.getItem(0)).thenReturn(inventoryStack);
		LinkedStorageEndpointAdapters.register(new TestEndpointAdapter());

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);

			EnderLinkerEndpointRecipe.finalizeDeliveredCraftResult(level, craftedStack, playerInventory, ItemStack.EMPTY);
		}

		assertEquals(groupId, inventoryStack.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT).groupId());
		assertFalse(inventoryStack.has(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT));
		Mockito.verify(manager).consumeActivePendingCraftClaim(claimId);
	}

	@Test
	void matchesRejectsExtraOrPreviouslyLinkedInputs() {
		LinkedStorageEndpointAdapters.register(new TestEndpointAdapter());
		EnderLinkerItem linkerItem = linkerItem();
		EnderLinkerEndpointRecipe recipe = new EnderLinkerEndpointRecipe(CraftingBookCategory.MISC);

		assertFalse(recipe.matches(CraftingInput.of(3, 1, List.of(new ItemStack(linkerItem), new ItemStack(Items.STICK), new ItemStack(Items.DIRT))), null));

		ItemStack linkedEndpoint = new ItemStack(Items.STICK);
		linkedEndpoint.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));
		ItemStack boundLinker = new ItemStack(linkerItem);
		boundLinker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, new EnderLinkerTargetData(UUID.randomUUID(), Component.empty()));
		assertFalse(recipe.matches(CraftingInput.of(2, 1, List.of(boundLinker, linkedEndpoint)), null));
	}

	@Test
	void matchesRejectsIncompatibleSecondaryBeforeCrafting() {
		UUID groupId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ILinkedStorageItemEndpointAdapter adapter = new TestEndpointAdapter() {
			@Override
			public boolean supports(ItemStack stack) {
				return stack.is(Items.DIAMOND);
			}

			@Override
			public boolean isCompatible(ServerLevel serverLevel, ItemStack endpoint, LinkedStorageHostDescriptor hostDescriptor) {
				return false;
			}
		};
		LinkedStorageEndpointAdapters.register(adapter);
		Mockito.when(manager.getHostDescriptor(groupId)).thenReturn(Optional.of(new LinkedStorageHostDescriptor(adapter.factoryId(), new CompoundTag())));
		Mockito.when(manager.usesHostFactory(groupId, adapter.factoryId())).thenReturn(true);
		Mockito.when(savedData.manager()).thenReturn(manager);
		ItemStack linker = new ItemStack(linkerItem());
		linker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, new EnderLinkerTargetData(groupId, Component.empty()));
		CraftingInput input = CraftingInput.of(2, 1, List.of(linker, new ItemStack(Items.DIAMOND)));

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);

			assertFalse(new EnderLinkerEndpointRecipe(CraftingBookCategory.MISC).matches(input, level));
		}
	}

	@Test
	void completeCraftBindsBlankLinkerToGroupOfExistingEndpoint() {
		UUID groupId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		EnderLinkerItem linkerItem = linkerItem();
		ItemStack linker = new ItemStack(linkerItem);
		ItemStack endpoint = new ItemStack(Items.STICK);
		endpoint.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, new LinkedStorageEndpointData(groupId, UUID.randomUUID()));
		EnderLinkerEndpointRecipe recipe = new EnderLinkerEndpointRecipe(CraftingBookCategory.MISC);
		CraftingInput input = CraftingInput.of(2, 1, List.of(linker, endpoint));
		Mockito.when(savedData.manager()).thenReturn(manager);
		Mockito.when(manager.getActivePendingCraftClaim(Mockito.any()))
				.thenAnswer(invocation -> Optional.of(new ActivePendingCraftClaim(invocation.getArgument(0), groupId,
						endpoint.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT).endpointId(), EnderLinkPendingCraftPlan.BIND_EXISTING_ENDPOINT)));
		Mockito.when(manager.consumeActivePendingCraftClaim(Mockito.any())).thenReturn(true);
		Player player = player();
		Mockito.when(manager.getHostDescriptor(groupId))
				.thenReturn(Optional.of(new LinkedStorageHostDescriptor(TestEndpointAdapter.FACTORY_ID, new CompoundTag())));
		mockLinkedStorageHost(manager, groupId);

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);
			assertTrue(recipe.matches(input, null));
			ItemStack result = recipe.assemble(input, Mockito.mock(HolderLookup.Provider.class));
			assertTrue(result.is(linkerItem));
			EnderLinkerEndpointRecipe.issueCraftClaim(player, result);
			assertTrue(EnderLinkerEndpointRecipe.completeCraft(level, player, new SimpleContainer(linker, endpoint), result));
			assertTrue(EnderLinkerEndpointRecipe.finalizePendingCraftLinker(level, result));
			assertEquals(groupId, result.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupId());
			assertEquals(groupId, recipe.getRemainingItems(input).get(1).get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT).groupId());
		}
	}

	private static void mockLinkedStorageHost(LinkedStorageGroupManager manager, UUID groupId) {
		ILinkedStorageVirtualHost host = Mockito.mock(ILinkedStorageVirtualHost.class);
		Mockito.when(host.getLinkedStorageDisplayName()).thenReturn(Optional.empty());
		Mockito.when(manager.resolveVirtualHost(groupId)).thenReturn(Optional.of(host));
	}

	private static class TestEndpointAdapter implements ILinkedStorageItemEndpointAdapter {
		private static final ResourceLocation FACTORY_ID = ResourceLocation.fromNamespaceAndPath("sophisticatedcore", "recipe_test_endpoint_adapter");

		@Override
		public boolean supports(ItemStack stack) {
			return stack.is(Items.STICK);
		}

		@Override
		public ResourceLocation factoryId() {
			return FACTORY_ID;
		}

		@Override
		public LinkedStorageHostDescriptor createHostDescriptor(ServerLevel level, ItemStack stack) {
			return new LinkedStorageHostDescriptor(FACTORY_ID, new CompoundTag());
		}

		@Override
		public CompoundTag copyCanonicalContents(ServerLevel level, ItemStack stack) {
			return new CompoundTag();
		}

		@Override
		public void bindEndpoint(ServerLevel level, ItemStack stack, LinkedStorageEndpointData endpoint) {
			stack.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, endpoint);
		}
	}

	private static EnderLinkerItem linkerItem() {
		EnderLinkerItem item = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);
		Mockito.when(item.components()).thenReturn(DataComponentMap.EMPTY);
		return item;
	}

	private static Player player() {
		Player player = Mockito.mock(Player.class);
		Inventory inventory = Mockito.mock(Inventory.class);
		Mockito.when(player.getUUID()).thenReturn(UUID.randomUUID());
		Mockito.when(player.getInventory()).thenReturn(inventory);
		Mockito.when(inventory.add(Mockito.any(ItemStack.class))).thenReturn(true);
		return player;
	}
}
