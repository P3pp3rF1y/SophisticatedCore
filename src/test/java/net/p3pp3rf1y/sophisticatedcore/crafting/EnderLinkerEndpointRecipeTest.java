package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraftforge.items.IItemHandler;
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
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void pendingCraftPlanRejectsUnknownSerializedName() {
		assertThrows(IllegalStateException.class, () -> EnderLinkPendingCraftPlan.fromSerializedName("unknown"));
	}
	@Test
	void getCraftingDiagnosticDoesNotReportIncompleteLinkerRecipeAsError() {
		assertTrue(EnderLinkerEndpointRecipe.getCraftingDiagnostic(Mockito.mock(ServerLevel.class), container(linkerStack())).isEmpty());
	}
	@Test
	void getCraftingDiagnosticDoesNotReportIncompatibleStorageAsError() {
		ItemStack linker = linkerStack();
		LinkedStorageStackData.setLinkerTarget(linker, new EnderLinkerTargetData(UUID.randomUUID(), Component.empty()));
		assertTrue(EnderLinkerEndpointRecipe.getCraftingDiagnostic(Mockito.mock(ServerLevel.class), container(linker, new ItemStack(Items.DIRT))).isEmpty());
	}
	@Test
	void getCraftingDiagnosticDoesNotReportValidLinkerClearRecipeAsError() {
		ItemStack linker = linkerStack();
		LinkedStorageStackData.setLinkerTarget(linker, new EnderLinkerTargetData(UUID.randomUUID(), Component.empty()));
		assertTrue(EnderLinkerEndpointRecipe.getCraftingDiagnostic(Mockito.mock(ServerLevel.class), container(linker)).isEmpty());
	}
	@Test
	void getCraftingDiagnosticReportsAlreadyLinkedEndpointInsteadOfGenericCraftingInputs() {
		ItemStack linker = linkerStack();
		LinkedStorageStackData.setLinkerTarget(linker, new EnderLinkerTargetData(UUID.randomUUID(), Component.empty()));
		ItemStack endpoint = new ItemStack(Items.STICK);
		LinkedStorageStackData.setEndpoint(endpoint, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));
		EnderLinkerEndpointRecipe.CraftingDiagnostic diagnostic = EnderLinkerEndpointRecipe
				.getCraftingDiagnostic(Mockito.mock(ServerLevel.class), container(linker, endpoint)).orElseThrow();
		assertEquals(1, diagnostic.slot());
		assertEquals(EnderLinkerEndpointRecipe.CraftingDiagnostic.Failure.ALREADY_LINKED, diagnostic.failure());
	}
	@Test
	void issueCraftClaimReissuesExpiredInFlightClaim() {
		UUID expired = UUID.randomUUID();
		ItemStack result = new ItemStack(Items.BLAZE_ROD);
		LinkedStorageStackData.setPendingCraft(result, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, expired));
		Player player = player();
		EnderLinkerEndpointRecipe.issueCraftClaim(player, result);
		assertNotNull(LinkedStorageStackData.getPendingCraft(result).claimId());
		assertNotEquals(expired, LinkedStorageStackData.getPendingCraft(result).claimId());
	}
	@Test
	void issueCraftClaimSharesInFlightClaimAcrossDuplicateCallbacks() {
		ItemStack result = new ItemStack(Items.BLAZE_ROD);
		LinkedStorageStackData.setPendingCraft(result, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, null));
		ItemStack copy = result.copy();
		Player player = player();
		EnderLinkerEndpointRecipe.issueCraftClaim(player, result);
		EnderLinkerEndpointRecipe.issueCraftClaim(player, copy);
		assertEquals(LinkedStorageStackData.getPendingCraft(result).claimId(), LinkedStorageStackData.getPendingCraft(copy).claimId());
	}
	@Test
	void completeCraftUsesPendingLinkerAsSecondaryCraftInput() {
		UUID groupId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		TestEndpointAdapter adapter = new TestEndpointAdapter();
		LinkedStorageEndpointAdapters.register(adapter);
		Mockito.when(manager.createGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(groupId);
		Mockito.when(manager.usesHostFactory(groupId, adapter.factoryId())).thenReturn(true);
		AtomicReference<ActivePendingCraftClaim> active = new AtomicReference<>();
		Mockito.doAnswer(i -> {
			active.set(i.getArgument(0));
			return null;
		}).when(manager).activatePendingCraftClaim(Mockito.any());
		Mockito.when(manager.getActivePendingCraftClaim(Mockito.any()))
				.thenAnswer(i -> Optional.ofNullable(active.get()).filter(c -> c.claimId().equals(i.getArgument(0))));
		Mockito.when(manager.consumeActivePendingCraftClaim(Mockito.any())).thenReturn(true);
		Mockito.when(manager.getHostDescriptor(groupId)).thenReturn(Optional.of(new LinkedStorageHostDescriptor(adapter.factoryId(), new CompoundTag())));
		mockHost(manager, groupId);
		Mockito.when(data.manager()).thenReturn(manager);
		EnderLinkerItem item = linkerItem();
		ItemStack linker = linkerStack(3);
		ItemStack first = new ItemStack(Items.STICK);
		EnderLinkerEndpointRecipe recipe = recipe();
		Player player = player();
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			assertTrue(recipe.matches(crafting(linker, first), null));
			ItemStack firstResult = recipe.assemble(crafting(linker, first), Mockito.mock(RegistryAccess.class));
			assertTrue(firstResult.is(Items.BLAZE_ROD));
			assertNotNull(LinkedStorageStackData.getPendingCraft(firstResult));
			firstResult = linkerStack(firstResult);
			assertTrue(EnderLinkerItem.hasBoundPresentation(firstResult));
			assertFalse(LinkedStorageStackData.getLinkerTarget(firstResult) != null);
			EnderLinkerEndpointRecipe.issueCraftClaim(player, firstResult);
			ItemStack unclaimed = firstResult.copy();
			LinkedStorageStackData.clear(unclaimed);
			assertTrue(EnderLinkerEndpointRecipe.completeCraft(level, player, container(linker, first), unclaimed));
			Mockito.verify(manager).createGroup(Mockito.eq(player.getUUID()), Mockito.any(), Mockito.any(), Mockito.any());
			assertEquals(groupId, LinkedStorageStackData.getEndpoint(first).groupId());
			assertEquals(3, linker.getCount());
			assertTrue(recipe.getRemainingItems(crafting(linker, first)).get(0).isEmpty());
			assertEquals(groupId, LinkedStorageStackData.getEndpoint(recipe.getRemainingItems(crafting(linker, first)).get(1)).groupId());
			ItemStack second = new ItemStack(Items.STICK);
			ItemStack secondResult = recipe.assemble(crafting(firstResult, second), Mockito.mock(RegistryAccess.class));
			EnderLinkerEndpointRecipe.issueCraftClaim(player, secondResult);
			assertTrue(EnderLinkerEndpointRecipe.completeCraft(level, player, container(firstResult, second), secondResult));
			assertTrue(EnderLinkerEndpointRecipe.finalizePendingCraftResult(level, secondResult));
			assertEquals(groupId, LinkedStorageStackData.getLinkerTarget(firstResult).groupId());
			assertEquals(groupId, LinkedStorageStackData.getEndpoint(secondResult).groupId());
			assertEquals(1, firstResult.getCount());
		}
	}
	@Test
	void finalizeTransferredCraftResultResolvesDeliveredPendingLinkerInStorage() {
		UUID group = UUID.randomUUID(), endpoint = UUID.randomUUID(), claim = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		IItemHandler storage = Mockito.mock(IItemHandler.class);
		Inventory inventory = Mockito.mock(Inventory.class);
		ItemStack transferred = linkerStack(), stored = transferred.copy();
		EnderLinkPendingCraftData pending = new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, claim);
		LinkedStorageStackData.setPendingCraft(transferred, pending);
		LinkedStorageStackData.setPendingCraft(stored, pending);
		Mockito.when(manager.getActivePendingCraftClaim(claim))
				.thenReturn(Optional.of(new ActivePendingCraftClaim(claim, group, endpoint, EnderLinkPendingCraftPlan.CREATE_PRIMARY)));
		Mockito.when(manager.isEndpointMember(group, endpoint)).thenReturn(true);
		Mockito.when(manager.consumeActivePendingCraftClaim(claim)).thenReturn(true);
		mockHost(manager, group);
		Mockito.when(data.manager()).thenReturn(manager);
		Mockito.when(storage.getSlots()).thenReturn(1);
		Mockito.when(storage.getStackInSlot(0)).thenReturn(stored);
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			EnderLinkerEndpointRecipe.finalizeTransferredCraftResult(level, transferred, storage, inventory);
		}
		assertEquals(group, LinkedStorageStackData.getLinkerTarget(stored).groupId());
		assertFalse(LinkedStorageStackData.getPendingCraft(stored) != null);
		Mockito.verify(manager).consumeActivePendingCraftClaim(claim);
	}
	@Test
	void finalizeDeliveredCraftResultResolvesPendingEndpointInPlayerInventory() {
		UUID group = UUID.randomUUID(), endpoint = UUID.randomUUID(), claim = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Inventory inventory = Mockito.mock(Inventory.class);
		ItemStack crafted = new ItemStack(Items.STICK), stored = crafted.copy();
		LinkedStorageStackData.setPendingCraft(crafted, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.ADD_SECONDARY, claim));
		LinkedStorageStackData.setPendingCraft(stored, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.ADD_SECONDARY, null));
		Mockito.when(manager.getActivePendingCraftClaim(claim))
				.thenReturn(Optional.of(new ActivePendingCraftClaim(claim, group, endpoint, EnderLinkPendingCraftPlan.ADD_SECONDARY)));
		Mockito.when(manager.isEndpointMember(group, endpoint)).thenReturn(true);
		Mockito.when(manager.usesHostFactory(group, TestEndpointAdapter.FACTORY_ID)).thenReturn(true);
		Mockito.when(manager.consumeActivePendingCraftClaim(claim)).thenReturn(true);
		Mockito.when(data.manager()).thenReturn(manager);
		Mockito.when(inventory.getContainerSize()).thenReturn(1);
		Mockito.when(inventory.getItem(0)).thenReturn(stored);
		LinkedStorageEndpointAdapters.register(new TestEndpointAdapter());
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			EnderLinkerEndpointRecipe.finalizeDeliveredCraftResult(level, crafted, inventory, ItemStack.EMPTY);
		}
		assertEquals(group, LinkedStorageStackData.getEndpoint(stored).groupId());
		assertFalse(LinkedStorageStackData.getPendingCraft(stored) != null);
		Mockito.verify(manager).consumeActivePendingCraftClaim(claim);
	}
	@Test
	void matchesRejectsExtraOrPreviouslyLinkedInputs() {
		LinkedStorageEndpointAdapters.register(new TestEndpointAdapter());
		assertFalse(recipe().matches(crafting(linkerStack(), new ItemStack(Items.STICK), new ItemStack(Items.DIRT)), null));
		ItemStack endpoint = new ItemStack(Items.STICK);
		LinkedStorageStackData.setEndpoint(endpoint, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));
		ItemStack linker = linkerStack();
		LinkedStorageStackData.setLinkerTarget(linker, new EnderLinkerTargetData(UUID.randomUUID(), Component.empty()));
		assertFalse(recipe().matches(crafting(linker, endpoint), null));
	}
	@Test
	void matchesRejectsIncompatibleSecondaryBeforeCrafting() {
		UUID group = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ILinkedStorageItemEndpointAdapter adapter = new TestEndpointAdapter() {
			@Override
			public boolean supports(ItemStack stack) {
				return stack.is(Items.DIAMOND);
			}
			@Override
			public boolean isCompatible(ServerLevel serverLevel, ItemStack endpoint, LinkedStorageHostDescriptor descriptor) {
				return false;
			}
		};
		LinkedStorageEndpointAdapters.register(adapter);
		Mockito.when(manager.getHostDescriptor(group)).thenReturn(Optional.of(new LinkedStorageHostDescriptor(adapter.factoryId(), new CompoundTag())));
		Mockito.when(manager.usesHostFactory(group, adapter.factoryId())).thenReturn(true);
		Mockito.when(data.manager()).thenReturn(manager);
		ItemStack linker = linkerStack();
		LinkedStorageStackData.setLinkerTarget(linker, new EnderLinkerTargetData(group, Component.empty()));
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			assertFalse(recipe().matches(crafting(linker, new ItemStack(Items.DIAMOND)), level));
		}
	}
	@Test
	void completeCraftBindsBlankLinkerToGroupOfExistingEndpoint() {
		UUID group = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ItemStack linker = linkerStack(), endpoint = new ItemStack(Items.STICK);
		LinkedStorageEndpointData endpointData = new LinkedStorageEndpointData(group, UUID.randomUUID());
		LinkedStorageStackData.setEndpoint(endpoint, endpointData);
		Mockito.when(data.manager()).thenReturn(manager);
		Mockito.when(manager.getActivePendingCraftClaim(Mockito.any())).thenAnswer(i -> Optional
				.of(new ActivePendingCraftClaim(i.getArgument(0), group, endpointData.endpointId(), EnderLinkPendingCraftPlan.BIND_EXISTING_ENDPOINT)));
		Mockito.when(manager.consumeActivePendingCraftClaim(Mockito.any())).thenReturn(true);
		Mockito.when(manager.getHostDescriptor(group))
				.thenReturn(Optional.of(new LinkedStorageHostDescriptor(TestEndpointAdapter.FACTORY_ID, new CompoundTag())));
		mockHost(manager, group);
		Player player = player();
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			assertTrue(recipe().matches(crafting(linker, endpoint), null));
			ItemStack result = recipe().assemble(crafting(linker, endpoint), Mockito.mock(RegistryAccess.class));
			assertTrue(result.is(Items.BLAZE_ROD));
			EnderLinkerEndpointRecipe.issueCraftClaim(player, result);
			assertTrue(EnderLinkerEndpointRecipe.completeCraft(level, player, container(linker, endpoint), result));
			assertTrue(EnderLinkerEndpointRecipe.finalizePendingCraftLinker(level, result));
			assertEquals(group, LinkedStorageStackData.getLinkerTarget(result).groupId());
			assertEquals(group, LinkedStorageStackData.getEndpoint(recipe().getRemainingItems(crafting(linker, endpoint)).get(1)).groupId());
		}
	}
	private static EnderLinkerEndpointRecipe recipe() {
		return new EnderLinkerEndpointRecipe(new ResourceLocation("sophisticatedcore", "ender_linker_endpoint_test"), CraftingBookCategory.MISC);
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
	private static ItemStack linkerStack(ItemStack source) {
		ItemStack stack = linkerStack(source.getCount());
		if (source.hasTag()) {
			stack.setTag(source.getTag().copy());
		}
		return stack;
	}
	private static Player player() {
		Player player = Mockito.mock(Player.class);
		Inventory inventory = Mockito.mock(Inventory.class);
		Mockito.when(player.getUUID()).thenReturn(UUID.randomUUID());
		Mockito.when(player.getInventory()).thenReturn(inventory);
		Mockito.when(inventory.add(Mockito.any(ItemStack.class))).thenReturn(true);
		return player;
	}
	private static Container container(ItemStack... stacks) {
		Container input = Mockito.mock(Container.class);
		Mockito.when(input.getContainerSize()).thenReturn(stacks.length);
		for (int slot = 0; slot < stacks.length; slot++) {
			Mockito.when(input.getItem(slot)).thenReturn(stacks[slot]);
		}
		return input;
	}
	private static CraftingContainer crafting(ItemStack... stacks) {
		CraftingContainer input = Mockito.mock(CraftingContainer.class);
		Mockito.when(input.getContainerSize()).thenReturn(stacks.length);
		for (int slot = 0; slot < stacks.length; slot++) {
			Mockito.when(input.getItem(slot)).thenReturn(stacks[slot]);
		}
		return input;
	}
	private static void mockHost(LinkedStorageGroupManager manager, UUID group) {
		ILinkedStorageVirtualHost host = Mockito.mock(ILinkedStorageVirtualHost.class);
		Mockito.when(host.getLinkedStorageDisplayName()).thenReturn(Optional.empty());
		Mockito.when(manager.resolveVirtualHost(group)).thenReturn(Optional.of(host));
	}
	private static class TestEndpointAdapter implements ILinkedStorageItemEndpointAdapter {
		private static final ResourceLocation FACTORY_ID = new ResourceLocation("sophisticatedcore", "recipe_test_endpoint_adapter");
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
			LinkedStorageStackData.setEndpoint(stack, endpoint);
		}
	}
}
