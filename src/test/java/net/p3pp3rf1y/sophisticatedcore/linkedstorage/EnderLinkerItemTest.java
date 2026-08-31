package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnderLinkerItemTest {
	@Test
	void onItemUseFirstPassesForNonEndpointBlocks() {
		EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		UseOnContext context = Mockito.mock(UseOnContext.class);
		BlockPos pos = BlockPos.ZERO;
		Mockito.when(context.getPlayer()).thenReturn(Mockito.mock(Player.class));
		Mockito.when(context.getLevel()).thenReturn(level);
		Mockito.when(context.getClickedPos()).thenReturn(pos);

		assertEquals(InteractionResult.PASS, linkerItem.onItemUseFirst(new ItemStack(Items.BLAZE_ROD), context));
	}

	@Test
	void overrideStackedOnOtherCreatesGroupForCompatibleEndpoint() {
		UUID groupId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Player player = Mockito.mock(Player.class);
		UUID ownerId = UUID.randomUUID();
		Slot slot = Mockito.mock(Slot.class);
		ILinkedStorageItemEndpointAdapter adapter = new TestEndpointAdapter();
		LinkedStorageEndpointAdapters.register(adapter);
		Mockito.when(player.level()).thenReturn(level);
		Mockito.when(player.getUUID()).thenReturn(ownerId);
		Mockito.when(slot.getItem()).thenReturn(new ItemStack(Items.BONE));
		Mockito.when(manager.createGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(groupId);
		Mockito.when(manager.usesHostFactory(groupId, adapter.factoryId())).thenReturn(true);
		Mockito.when(manager.getHostDescriptor(groupId)).thenReturn(Optional.of(new LinkedStorageHostDescriptor(adapter.factoryId(), new CompoundTag())));
		mockLinkedStorageHost(manager, groupId);
		Mockito.when(savedData.manager()).thenReturn(manager);

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);
			EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);
			ItemStack linker = new ItemStack(Items.BLAZE_ROD);

			assertTrue(linkerItem.overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player));
			Mockito.verify(manager).createGroup(Mockito.eq(ownerId), Mockito.any(), Mockito.any(), Mockito.any());
			assertEquals(groupId, linker.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupId());
			assertEquals(groupId, slot.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT).groupId());
			assertTrue(EnderLinkerItem.hasBoundPresentation(linker));
		}
	}

	@Test
	void overrideStackedOnOtherConsumesBoundLinkerForCompatibleEndpoint() {
		UUID groupId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Player player = Mockito.mock(Player.class);
		Inventory inventory = Mockito.mock(Inventory.class);
		Slot slot = Mockito.mock(Slot.class);
		ILinkedStorageItemEndpointAdapter adapter = new TestEndpointAdapter();
		LinkedStorageEndpointAdapters.register(adapter);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD, 2);
		linker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, new EnderLinkerTargetData(groupId, Component.empty()));
		ItemStack endpoint = new ItemStack(Items.BONE);
		Mockito.when(player.level()).thenReturn(level);
		Mockito.when(player.getUUID()).thenReturn(UUID.randomUUID());
		Mockito.when(player.getInventory()).thenReturn(inventory);
		Mockito.when(inventory.add(Mockito.any(ItemStack.class))).thenReturn(true);
		Mockito.when(slot.getItem()).thenReturn(endpoint);
		Mockito.when(manager.usesHostFactory(groupId, adapter.factoryId())).thenReturn(true);
		Mockito.when(manager.getHostDescriptor(groupId)).thenReturn(Optional.of(new LinkedStorageHostDescriptor(adapter.factoryId(), new CompoundTag())));
		Mockito.when(savedData.manager()).thenReturn(manager);

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);
			EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);

			assertTrue(linkerItem.overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player));
		}

		assertEquals(1, linker.getCount());
		assertEquals(groupId, endpoint.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT).groupId());
	}

	@Test
	void linkWithResultBindsLinkerToExistingEndpointRegardlessOfRecordedOwner() {
		UUID groupId = UUID.randomUUID();
		UUID playerId = UUID.randomUUID();
		UUID endpointId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Mockito.when(manager.isEndpointMember(groupId, endpointId)).thenReturn(true);
		mockLinkedStorageHost(manager, groupId);
		Mockito.when(savedData.manager()).thenReturn(manager);
		EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);
		Mockito.when(linkerItem.components()).thenReturn(DataComponentMap.EMPTY);
		ItemStack linker = new ItemStack(linkerItem);
		ItemStack endpoint = new ItemStack(Items.BONE);
		endpoint.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, new LinkedStorageEndpointData(groupId, endpointId));

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);

			assertEquals(LinkedStorageService.LinkResult.SUCCESS, LinkedStorageService.linkWithResult(level, playerId, linker, endpoint));
			assertEquals(groupId, linker.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupId());
			Mockito.verify(manager, Mockito.never()).registerEndpoint(Mockito.any(), Mockito.any());
		}
	}

	@Test
	void linkWithResultRejectsPendingCraftLinker() {
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);
		linker.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, UUID.randomUUID()));

		assertThrows(IllegalStateException.class,
				() -> LinkedStorageService.linkWithResult(Mockito.mock(ServerLevel.class), null, linker, new ItemStack(Items.BONE)));
	}

	@Test
	@SuppressWarnings("unchecked")
	void linkWithResultCreatesGroupForBlockEndpointUsingDirectEndpointData() {
		UUID groupId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ILinkedStorageBlockEndpoint endpoint = Mockito.mock(ILinkedStorageBlockEndpoint.class);
		ILinkedStorageEndpointAdapter<ILinkedStorageBlockEndpoint> adapter = Mockito.mock(ILinkedStorageEndpointAdapter.class);
		LinkedStorageHostDescriptor descriptor = new LinkedStorageHostDescriptor(TestEndpointAdapter.FACTORY_ID, new CompoundTag());
		Mockito.when(endpoint.getLinkedStorageBlockEndpointAdapter()).thenReturn(adapter);
		Mockito.when(adapter.createHostDescriptor(level, endpoint)).thenReturn(descriptor);
		Mockito.when(adapter.copyCanonicalContents(level, endpoint)).thenReturn(new CompoundTag());
		Mockito.when(manager.createGroup(Mockito.eq(ownerId), Mockito.any(), Mockito.eq(descriptor), Mockito.any())).thenReturn(groupId);
		mockLinkedStorageHost(manager, groupId);
		Mockito.when(savedData.manager()).thenReturn(manager);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);

			assertEquals(LinkedStorageService.LinkResult.SUCCESS, LinkedStorageService.linkWithResult(level, ownerId, linker, endpoint));
		}

		assertEquals(groupId, linker.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupId());
		Mockito.verify(adapter).bindEndpoint(Mockito.eq(level), Mockito.same(endpoint),
				Mockito.argThat(endpointData -> endpointData.groupId().equals(groupId)));
		Mockito.verify(adapter).onEndpointLinked(level, endpoint);
	}

	@Test
	@SuppressWarnings("unchecked")
	void linkWithResultDoesNotRegisterBlockEndpointWhenBindingFails() {
		UUID groupId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ILinkedStorageBlockEndpoint endpoint = Mockito.mock(ILinkedStorageBlockEndpoint.class);
		ILinkedStorageEndpointAdapter<ILinkedStorageBlockEndpoint> adapter = Mockito.mock(ILinkedStorageEndpointAdapter.class);
		LinkedStorageHostDescriptor descriptor = new LinkedStorageHostDescriptor(TestEndpointAdapter.FACTORY_ID, new CompoundTag());
		Mockito.when(endpoint.getLinkedStorageBlockEndpointAdapter()).thenReturn(adapter);
		Mockito.when(manager.getHostDescriptor(groupId)).thenReturn(Optional.of(descriptor));
		Mockito.when(manager.usesHostFactory(groupId, adapter.factoryId())).thenReturn(true);
		Mockito.when(adapter.getCompatibility(level, endpoint, descriptor)).thenReturn(ILinkedStorageEndpointAdapter.Compatibility.COMPATIBLE);
		Mockito.when(savedData.manager()).thenReturn(manager);
		Mockito.doThrow(new IllegalStateException("binding failed")).when(adapter).bindEndpoint(Mockito.eq(level), Mockito.same(endpoint), Mockito.any());
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);
		linker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, new EnderLinkerTargetData(groupId, Component.empty()));

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);

			assertThrows(IllegalStateException.class, () -> LinkedStorageService.linkWithResult(level, null, linker, endpoint));
		}

		assertEquals(1, linker.getCount());
		Mockito.verify(manager, Mockito.never()).registerEndpoint(Mockito.eq(groupId), Mockito.any());
	}

	@Test
	void linkStoresCanonicalGroupNameInBoundLinker() {
		UUID groupId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ILinkedStorageItemEndpointAdapter adapter = new TestEndpointAdapter();
		LinkedStorageEndpointAdapters.register(adapter);
		Mockito.when(manager.createGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(groupId);
		Mockito.when(manager.resolveVirtualHost(groupId)).thenReturn(Optional.of(new ILinkedStorageVirtualHost() {
			@Override
			public Optional<Component> getLinkedStorageDisplayName() {
				return Optional.of(Component.literal("Main Backpack"));
			}
		}));
		Mockito.when(savedData.manager()).thenReturn(manager);

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);
			ItemStack linker = new ItemStack(Items.BLAZE_ROD);

			assertTrue(LinkedStorageService.link(level, linker, new ItemStack(Items.BONE)));
			assertEquals("Main Backpack", linker.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupName().getString());
		}
	}

	@Test
	void linkRejectsIncompatibleSecondaryWithoutRegisteringEndpointOrConsumingLinker() {
		UUID groupId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ILinkedStorageItemEndpointAdapter adapter = new TestEndpointAdapter() {
			@Override
			public boolean supports(ItemStack stack) {
				return stack.is(Items.STRING);
			}

			@Override
			public boolean isCompatible(ServerLevel serverLevel, ItemStack endpoint, LinkedStorageHostDescriptor hostDescriptor) {
				return false;
			}
		};
		LinkedStorageEndpointAdapters.register(adapter);
		Mockito.when(manager.usesHostFactory(groupId, adapter.factoryId())).thenReturn(true);
		Mockito.when(manager.getHostDescriptor(groupId)).thenReturn(Optional.of(new LinkedStorageHostDescriptor(adapter.factoryId(), new CompoundTag())));
		Mockito.when(savedData.manager()).thenReturn(manager);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);
		linker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, new EnderLinkerTargetData(groupId, Component.empty()));
		ItemStack endpoint = new ItemStack(Items.STRING);

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);

			assertFalse(LinkedStorageService.link(level, linker, endpoint));
		}

		assertEquals(1, linker.getCount());
		assertFalse(endpoint.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT));
		Mockito.verify(manager, Mockito.never()).registerEndpoint(Mockito.eq(groupId), Mockito.any());
	}

	@Test
	void overrideStackedOnOtherResolvesPendingLinkerClaim() {
		UUID groupId = UUID.randomUUID();
		UUID primaryEndpointId = UUID.randomUUID();
		UUID claimId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Player player = Mockito.mock(Player.class);
		Slot slot = Mockito.mock(Slot.class);
		ILinkedStorageItemEndpointAdapter adapter = new TestEndpointAdapter();
		LinkedStorageEndpointAdapters.register(adapter);
		Mockito.when(player.level()).thenReturn(level);
		Mockito.when(slot.getItem()).thenReturn(new ItemStack(Items.BONE));
		Mockito.when(manager.getActivePendingCraftClaim(claimId))
				.thenReturn(Optional.of(new ActivePendingCraftClaim(claimId, groupId, primaryEndpointId, EnderLinkPendingCraftPlan.CREATE_PRIMARY)));
		Mockito.when(manager.isEndpointMember(groupId, primaryEndpointId)).thenReturn(true);
		Mockito.when(manager.usesHostFactory(groupId, adapter.factoryId())).thenReturn(true);
		Mockito.when(manager.getHostDescriptor(groupId)).thenReturn(Optional.of(new LinkedStorageHostDescriptor(adapter.factoryId(), new CompoundTag())));
		Mockito.when(manager.consumeActivePendingCraftClaim(claimId)).thenReturn(true);
		mockLinkedStorageHost(manager, groupId);
		Mockito.when(savedData.manager()).thenReturn(manager);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);
		linker.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, claimId));

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);
			EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);

			assertTrue(linkerItem.overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player));
		}

		assertTrue(linker.isEmpty());
		assertEquals(groupId, slot.getItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT).groupId());
		Mockito.verify(manager).consumeActivePendingCraftClaim(claimId);
	}

	@Test
	void inventoryTickResolvesPendingLinkerInPlayerInventory() {
		UUID groupId = UUID.randomUUID();
		UUID endpointId = UUID.randomUUID();
		UUID claimId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);
		linker.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, claimId));
		Mockito.when(manager.getActivePendingCraftClaim(claimId))
				.thenReturn(Optional.of(new ActivePendingCraftClaim(claimId, groupId, endpointId, EnderLinkPendingCraftPlan.CREATE_PRIMARY)));
		Mockito.when(manager.isEndpointMember(groupId, endpointId)).thenReturn(true);
		Mockito.when(manager.consumeActivePendingCraftClaim(claimId)).thenReturn(true);
		mockLinkedStorageHost(manager, groupId);
		Mockito.when(savedData.manager()).thenReturn(manager);

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);
			EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);

			linkerItem.inventoryTick(linker, level, Mockito.mock(Player.class), null);
		}

		assertEquals(groupId, linker.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupId());
		assertFalse(linker.has(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT));
		Mockito.verify(manager).consumeActivePendingCraftClaim(claimId);
	}

	@Test
	void inventoryTickRejectsPendingLinkerWithoutActiveClaim() {
		UUID claimId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);
		linker.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, claimId));
		Mockito.when(manager.getActivePendingCraftClaim(claimId)).thenReturn(Optional.empty());
		Mockito.when(savedData.manager()).thenReturn(manager);

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);
			EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);

			assertThrows(IllegalStateException.class, () -> linkerItem.inventoryTick(linker, level, Mockito.mock(Player.class), null));
		}
	}

	@Test
	void overrideStackedOnOtherIgnoresNonStorageItemsWithoutFailureFeedback() {
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Player player = Mockito.mock(Player.class);
		Slot slot = Mockito.mock(Slot.class);
		Mockito.when(player.level()).thenReturn(level);
		Mockito.when(slot.getItem()).thenReturn(ItemStack.EMPTY);
		EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);

		assertFalse(linkerItem.overrideStackedOnOther(new ItemStack(Items.BLAZE_ROD), slot, ClickAction.SECONDARY, player));

		Mockito.verify(player, Mockito.never()).playNotifySound(Mockito.any(), Mockito.any(), Mockito.anyFloat(), Mockito.anyFloat());
		Mockito.verify(player, Mockito.never()).displayClientMessage(Mockito.any(Component.class), Mockito.anyBoolean());
	}

	@Test
	void getMaxStackSizeAllowsBlankAndBoundLinkersToStack() {
		EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);
		ItemStack blankLinker = new ItemStack(Items.BLAZE_ROD, 64);
		ItemStack boundLinker = new ItemStack(Items.BLAZE_ROD);
		boundLinker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, new EnderLinkerTargetData(UUID.randomUUID(), Component.empty()));

		assertEquals(64, linkerItem.getMaxStackSize(blankLinker));
		assertEquals(64, linkerItem.getMaxStackSize(boundLinker));
	}

	@Test
	void overrideStackedOnOtherBindsBlankLinkerToExistingEndpoint() {
		UUID groupId = UUID.randomUUID();
		UUID endpointId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Player player = Mockito.mock(Player.class);
		Slot slot = Mockito.mock(Slot.class);
		ItemStack endpoint = new ItemStack(Items.NETHER_WART);
		LinkedStorageEndpointData endpointData = new LinkedStorageEndpointData(groupId, endpointId);
		endpoint.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, endpointData);
		Mockito.when(player.level()).thenReturn(level);
		Mockito.when(slot.getItem()).thenReturn(endpoint);
		Mockito.when(manager.isEndpointMember(groupId, endpointId)).thenReturn(true);
		mockLinkedStorageHost(manager, groupId);
		Mockito.when(savedData.manager()).thenReturn(manager);

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);
			EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);
			ItemStack blankLinker = new ItemStack(Items.BLAZE_ROD);

			assertTrue(linkerItem.overrideStackedOnOther(blankLinker, slot, ClickAction.SECONDARY, player));
			assertEquals(groupId, blankLinker.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupId());
			assertEquals(endpointData, endpoint.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT));

		}

		Mockito.verify(player, Mockito.never()).playNotifySound(Mockito.any(), Mockito.any(), Mockito.anyFloat(), Mockito.anyFloat());
		Mockito.verify(player, Mockito.never()).displayClientMessage(Mockito.any(Component.class), Mockito.anyBoolean());
	}

	@Test
	void overrideStackedOnOtherShowsFailureFeedbackForAlreadyLinkedStorage() {
		UUID groupId = UUID.randomUUID();
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);
		linker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, new EnderLinkerTargetData(groupId, Component.empty()));
		ItemStack endpoint = new ItemStack(Items.NETHER_WART);
		LinkedStorageEndpointData endpointData = new LinkedStorageEndpointData(groupId, UUID.randomUUID());
		endpoint.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, endpointData);
		Player player = Mockito.mock(Player.class);
		Slot slot = Mockito.mock(Slot.class);
		Mockito.when(player.level()).thenReturn(Mockito.mock(ServerLevel.class));
		Mockito.when(slot.getItem()).thenReturn(endpoint);
		EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);

		assertTrue(linkerItem.overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player));

		assertEquals(groupId, linker.get(ModCoreDataComponents.ENDER_LINKER_TARGET).groupId());
		assertEquals(endpointData, endpoint.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT));
		Mockito.verify(player).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7F);
		Mockito.verify(player).displayClientMessage(TranslationHelper.INSTANCE.translStatusMessage("ender_linker.already_linked"), true);
	}

	@Test
	void overrideStackedOnOtherKeepsBoundLinkerAndShowsFailureFeedbackForIncompatibleEndpoint() {
		UUID groupId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Player player = Mockito.mock(Player.class);
		Slot slot = Mockito.mock(Slot.class);
		ILinkedStorageItemEndpointAdapter adapter = new TestEndpointAdapter() {
			@Override
			public boolean supports(ItemStack stack) {
				return stack.is(Items.STRING);
			}

			@Override
			public boolean isCompatible(ServerLevel serverLevel, ItemStack endpoint, LinkedStorageHostDescriptor hostDescriptor) {
				return false;
			}
		};
		LinkedStorageEndpointAdapters.register(adapter);
		Mockito.when(player.level()).thenReturn(level);
		Mockito.when(slot.getItem()).thenReturn(new ItemStack(Items.STRING));
		Mockito.when(manager.usesHostFactory(groupId, adapter.factoryId())).thenReturn(true);
		Mockito.when(manager.getHostDescriptor(groupId)).thenReturn(Optional.of(new LinkedStorageHostDescriptor(adapter.factoryId(), new CompoundTag())));
		Mockito.when(savedData.manager()).thenReturn(manager);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);
		EnderLinkerTargetData target = new EnderLinkerTargetData(groupId, Component.empty());
		linker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, target);

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);
			EnderLinkerItem linkerItem = Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);

			assertTrue(linkerItem.overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player));
		}

		Mockito.verify(player).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7F);
		Mockito.verify(player).displayClientMessage(Mockito.any(Component.class), Mockito.eq(true));
		assertEquals(target, linker.get(ModCoreDataComponents.ENDER_LINKER_TARGET));
	}

	@Test
	void createSecondaryEndpointCopyCreatesNewSecondaryWithoutChangingSource() {
		UUID groupId = UUID.randomUUID();
		UUID sourceEndpointId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ILinkedStorageItemEndpointAdapter adapter = new TestEndpointAdapter();
		LinkedStorageEndpointAdapters.register(adapter);
		Mockito.when(manager.isEndpointMember(groupId, sourceEndpointId)).thenReturn(true);
		Mockito.when(manager.usesHostFactory(groupId, adapter.factoryId())).thenReturn(true);
		Mockito.when(savedData.manager()).thenReturn(manager);
		ItemStack source = new ItemStack(Items.BONE);
		LinkedStorageEndpointData sourceEndpoint = new LinkedStorageEndpointData(groupId, sourceEndpointId);
		source.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, sourceEndpoint);

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);

			ItemStack copy = LinkedStorageService.createSecondaryEndpointCopy(level, source).orElseThrow();
			LinkedStorageEndpointData copiedEndpoint = copy.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
			assertEquals(sourceEndpoint, source.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT));
			assertEquals(groupId, copiedEndpoint.groupId());
			assertFalse(sourceEndpointId.equals(copiedEndpoint.endpointId()));
		}
	}

	@Test
	void createSecondaryEndpointCopyDoesNotRegisterEndpointWhenBindingFails() {
		UUID groupId = UUID.randomUUID();
		UUID sourceEndpointId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData savedData = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ILinkedStorageItemEndpointAdapter adapter = new TestEndpointAdapter() {
			@Override
			public boolean supports(ItemStack stack) {
				return stack.is(Items.BLAZE_POWDER);
			}

			@Override
			public void bindEndpoint(ServerLevel serverLevel, ItemStack stack, LinkedStorageEndpointData endpoint) {
				throw new IllegalStateException("binding failed");
			}
		};
		LinkedStorageEndpointAdapters.register(adapter);
		Mockito.when(manager.isEndpointMember(groupId, sourceEndpointId)).thenReturn(true);
		Mockito.when(manager.usesHostFactory(groupId, adapter.factoryId())).thenReturn(true);
		Mockito.when(savedData.manager()).thenReturn(manager);
		ItemStack source = new ItemStack(Items.BLAZE_POWDER);
		source.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, new LinkedStorageEndpointData(groupId, sourceEndpointId));

		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(savedData);

			assertThrows(IllegalStateException.class, () -> LinkedStorageService.createSecondaryEndpointCopy(level, source));
		}

		Mockito.verify(manager, Mockito.never()).registerEndpoint(Mockito.eq(groupId), Mockito.any());
	}

	private static void mockLinkedStorageHost(LinkedStorageGroupManager manager, UUID groupId) {
		ILinkedStorageVirtualHost host = Mockito.mock(ILinkedStorageVirtualHost.class);
		Mockito.when(host.getLinkedStorageDisplayName()).thenReturn(Optional.empty());
		Mockito.when(manager.resolveVirtualHost(groupId)).thenReturn(Optional.of(host));
	}

	private static class TestEndpointAdapter implements ILinkedStorageItemEndpointAdapter {
		private static final ResourceLocation FACTORY_ID = ResourceLocation.fromNamespaceAndPath("sophisticatedcore", "test_endpoint_adapter");

		@Override
		public boolean supports(ItemStack stack) {
			return stack.is(Items.BONE);
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
}
