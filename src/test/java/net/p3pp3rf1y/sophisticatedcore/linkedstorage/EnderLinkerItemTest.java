package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
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
import org.junit.jupiter.api.BeforeAll;
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
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}
	@Test
	void onItemUseFirstPassesForNonEndpointBlocks() {
		EnderLinkerItem item = item();
		ServerLevel level = Mockito.mock(ServerLevel.class);
		UseOnContext context = Mockito.mock(UseOnContext.class);
		Mockito.when(context.getPlayer()).thenReturn(Mockito.mock(Player.class));
		Mockito.when(context.getLevel()).thenReturn(level);
		Mockito.when(context.getClickedPos()).thenReturn(BlockPos.ZERO);
		assertEquals(InteractionResult.PASS, item.onItemUseFirst(new ItemStack(Items.BLAZE_ROD), context));
	}
	@Test
	void overrideStackedOnOtherCreatesGroupForCompatibleEndpoint() {
		UUID group = UUID.randomUUID(), owner = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Player player = player(level, owner);
		Slot slot = Mockito.mock(Slot.class);
		TestAdapter adapter = new TestAdapter();
		LinkedStorageEndpointAdapters.register(adapter);
		ItemStack endpoint = new ItemStack(Items.BONE);
		Mockito.when(slot.getItem()).thenReturn(endpoint);
		Mockito.when(manager.createGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(group);
		Mockito.when(manager.usesHostFactory(group, adapter.factoryId())).thenReturn(true);
		Mockito.when(manager.getHostDescriptor(group)).thenReturn(Optional.of(new LinkedStorageHostDescriptor(adapter.factoryId(), new CompoundTag())));
		host(manager, group);
		Mockito.when(data.manager()).thenReturn(manager);
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			ItemStack linker = new ItemStack(Items.BLAZE_ROD);
			assertTrue(item().overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player));
			Mockito.verify(manager).createGroup(Mockito.eq(owner), Mockito.any(), Mockito.any(), Mockito.any());
			assertEquals(group, LinkedStorageStackData.getLinkerTarget(linker).groupId());
			assertEquals(group, LinkedStorageStackData.getEndpoint(endpoint).groupId());
			assertTrue(EnderLinkerItem.hasBoundPresentation(linker));
		}
	}
	@Test
	void overrideStackedOnOtherConsumesBoundLinkerForCompatibleEndpoint() {
		UUID group = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Player player = player(level, UUID.randomUUID());
		Slot slot = Mockito.mock(Slot.class);
		TestAdapter adapter = new TestAdapter();
		LinkedStorageEndpointAdapters.register(adapter);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD, 2), endpoint = new ItemStack(Items.BONE);
		LinkedStorageStackData.setLinkerTarget(linker, new EnderLinkerTargetData(group, Component.empty()));
		Mockito.when(slot.getItem()).thenReturn(endpoint);
		Mockito.when(manager.usesHostFactory(group, adapter.factoryId())).thenReturn(true);
		Mockito.when(manager.getHostDescriptor(group)).thenReturn(Optional.of(new LinkedStorageHostDescriptor(adapter.factoryId(), new CompoundTag())));
		Mockito.when(data.manager()).thenReturn(manager);
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			assertTrue(item().overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player));
		}
		assertEquals(1, linker.getCount());
		assertEquals(group, LinkedStorageStackData.getEndpoint(endpoint).groupId());
	}
	@Test
	void linkWithResultBindsLinkerToExistingEndpointRegardlessOfRecordedOwner() {
		UUID group = UUID.randomUUID(), endpointId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Mockito.when(manager.isEndpointMember(group, endpointId)).thenReturn(true);
		host(manager, group);
		Mockito.when(data.manager()).thenReturn(manager);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD), endpoint = new ItemStack(Items.BONE);
		LinkedStorageStackData.setEndpoint(endpoint, new LinkedStorageEndpointData(group, endpointId));
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			assertEquals(LinkedStorageService.LinkResult.SUCCESS, LinkedStorageService.linkWithResult(level, UUID.randomUUID(), linker, endpoint));
			assertEquals(group, LinkedStorageStackData.getLinkerTarget(linker).groupId());
			Mockito.verify(manager, Mockito.never()).registerEndpoint(Mockito.any(), Mockito.any());
		}
	}
	@Test
	void linkWithResultRejectsPendingCraftLinker() {
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);
		LinkedStorageStackData.setPendingCraft(linker, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, UUID.randomUUID()));
		assertThrows(IllegalStateException.class,
				() -> LinkedStorageService.linkWithResult(Mockito.mock(ServerLevel.class), null, linker, new ItemStack(Items.BONE)));
	}
	@Test
	@SuppressWarnings("unchecked")
	void linkWithResultCreatesGroupForBlockEndpointUsingDirectEndpointData() {
		UUID group = UUID.randomUUID();
		UUID owner = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ILinkedStorageBlockEndpoint endpoint = Mockito.mock(ILinkedStorageBlockEndpoint.class);
		ILinkedStorageEndpointAdapter<ILinkedStorageBlockEndpoint> adapter = Mockito.mock(ILinkedStorageEndpointAdapter.class);
		LinkedStorageHostDescriptor descriptor = new LinkedStorageHostDescriptor(TestAdapter.FACTORY_ID, new CompoundTag());
		Mockito.when(endpoint.getLinkedStorageBlockEndpointAdapter()).thenReturn(adapter);
		Mockito.when(adapter.createHostDescriptor(level, endpoint)).thenReturn(descriptor);
		Mockito.when(adapter.copyCanonicalContents(level, endpoint)).thenReturn(new CompoundTag());
		Mockito.when(manager.createGroup(Mockito.eq(owner), Mockito.any(), Mockito.eq(descriptor), Mockito.any())).thenReturn(group);
		host(manager, group);
		Mockito.when(data.manager()).thenReturn(manager);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			assertEquals(LinkedStorageService.LinkResult.SUCCESS, LinkedStorageService.linkWithResult(level, owner, linker, endpoint));
		}
		assertEquals(group, LinkedStorageStackData.getLinkerTarget(linker).groupId());
		Mockito.verify(adapter).bindEndpoint(Mockito.eq(level), Mockito.same(endpoint), Mockito.argThat(dataArgument -> dataArgument.groupId().equals(group)));
		Mockito.verify(adapter).onEndpointLinked(level, endpoint);
	}
	@Test
	void linkStoresCanonicalGroupNameInBoundLinker() {
		UUID group = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		LinkedStorageEndpointAdapters.register(new TestAdapter());
		Mockito.when(manager.createGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(group);
		ILinkedStorageVirtualHost host = Mockito.mock(ILinkedStorageVirtualHost.class);
		Mockito.when(host.getLinkedStorageDisplayName()).thenReturn(Optional.of(Component.literal("Main Backpack")));
		Mockito.when(manager.resolveVirtualHost(group)).thenReturn(Optional.of(host));
		Mockito.when(data.manager()).thenReturn(manager);
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			ItemStack linker = new ItemStack(Items.BLAZE_ROD);
			assertTrue(LinkedStorageService.link(level, linker, new ItemStack(Items.BONE)));
			assertEquals("Main Backpack", LinkedStorageStackData.getLinkerTarget(linker).groupName().getString());
		}
	}
	@Test
	void linkRejectsIncompatibleSecondaryWithoutRegisteringEndpointOrConsumingLinker() {
		UUID group = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		TestAdapter adapter = new TestAdapter() {
			@Override
			public boolean supports(ItemStack stack) {
				return stack.is(Items.STRING);
			}
			@Override
			public boolean isCompatible(ServerLevel serverLevel, ItemStack endpoint, LinkedStorageHostDescriptor descriptor) {
				return false;
			}
		};
		LinkedStorageEndpointAdapters.register(adapter);
		Mockito.when(manager.usesHostFactory(group, adapter.factoryId())).thenReturn(true);
		Mockito.when(manager.getHostDescriptor(group)).thenReturn(Optional.of(new LinkedStorageHostDescriptor(adapter.factoryId(), new CompoundTag())));
		Mockito.when(data.manager()).thenReturn(manager);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD), endpoint = new ItemStack(Items.STRING);
		LinkedStorageStackData.setLinkerTarget(linker, new EnderLinkerTargetData(group, Component.empty()));
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			assertFalse(LinkedStorageService.link(level, linker, endpoint));
		}
		assertEquals(1, linker.getCount());
		assertFalse(LinkedStorageStackData.getEndpoint(endpoint) != null);
		Mockito.verify(manager, Mockito.never()).registerEndpoint(Mockito.eq(group), Mockito.any());
	}
	@Test
	void inventoryTickRejectsPendingLinkerWithoutActiveClaim() {
		UUID claim = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);
		LinkedStorageStackData.setPendingCraft(linker, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, claim));
		Mockito.when(manager.getActivePendingCraftClaim(claim)).thenReturn(Optional.empty());
		Mockito.when(data.manager()).thenReturn(manager);
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			assertThrows(IllegalStateException.class, () -> item().inventoryTick(linker, level, Mockito.mock(Player.class), 0, false));
		}
	}
	@Test
	void overrideStackedOnOtherResolvesPendingLinkerClaim() {
		UUID group = UUID.randomUUID(), primary = UUID.randomUUID(), claim = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Player player = player(level, UUID.randomUUID());
		Slot slot = Mockito.mock(Slot.class);
		TestAdapter adapter = new TestAdapter();
		LinkedStorageEndpointAdapters.register(adapter);
		ItemStack endpoint = new ItemStack(Items.BONE);
		Mockito.when(slot.getItem()).thenReturn(endpoint);
		Mockito.when(manager.getActivePendingCraftClaim(claim))
				.thenReturn(Optional.of(new ActivePendingCraftClaim(claim, group, primary, EnderLinkPendingCraftPlan.CREATE_PRIMARY)));
		Mockito.when(manager.isEndpointMember(group, primary)).thenReturn(true);
		Mockito.when(manager.usesHostFactory(group, adapter.factoryId())).thenReturn(true);
		Mockito.when(manager.getHostDescriptor(group)).thenReturn(Optional.of(new LinkedStorageHostDescriptor(adapter.factoryId(), new CompoundTag())));
		Mockito.when(manager.consumeActivePendingCraftClaim(claim)).thenReturn(true);
		host(manager, group);
		Mockito.when(data.manager()).thenReturn(manager);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);
		LinkedStorageStackData.setPendingCraft(linker, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, claim));
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			assertTrue(item().overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player));
		}
		assertTrue(linker.isEmpty());
		assertEquals(group, LinkedStorageStackData.getEndpoint(endpoint).groupId());
		Mockito.verify(manager).consumeActivePendingCraftClaim(claim);
	}
	@Test
	void inventoryTickResolvesPendingLinkerInPlayerInventory() {
		UUID group = UUID.randomUUID(), endpoint = UUID.randomUUID(), claim = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);
		LinkedStorageStackData.setPendingCraft(linker, new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.CREATE_PRIMARY, claim));
		Mockito.when(manager.getActivePendingCraftClaim(claim))
				.thenReturn(Optional.of(new ActivePendingCraftClaim(claim, group, endpoint, EnderLinkPendingCraftPlan.CREATE_PRIMARY)));
		Mockito.when(manager.isEndpointMember(group, endpoint)).thenReturn(true);
		Mockito.when(manager.consumeActivePendingCraftClaim(claim)).thenReturn(true);
		host(manager, group);
		Mockito.when(data.manager()).thenReturn(manager);
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			item().inventoryTick(linker, level, Mockito.mock(Player.class), 0, false);
		}
		assertEquals(group, LinkedStorageStackData.getLinkerTarget(linker).groupId());
		assertFalse(LinkedStorageStackData.getPendingCraft(linker) != null);
		Mockito.verify(manager).consumeActivePendingCraftClaim(claim);
	}
	@Test
	void overrideStackedOnOtherIgnoresNonStorageItemsWithoutFailureFeedback() {
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Player player = Mockito.mock(Player.class);
		Slot slot = Mockito.mock(Slot.class);
		Mockito.when(player.level()).thenReturn(level);
		Mockito.when(slot.getItem()).thenReturn(ItemStack.EMPTY);
		assertFalse(item().overrideStackedOnOther(new ItemStack(Items.BLAZE_ROD), slot, ClickAction.SECONDARY, player));
		Mockito.verify(player, Mockito.never()).playNotifySound(Mockito.any(), Mockito.any(), Mockito.anyFloat(), Mockito.anyFloat());
		Mockito.verify(player, Mockito.never()).displayClientMessage(Mockito.any(Component.class), Mockito.anyBoolean());
	}
	@Test
	void getMaxStackSizeAllowsBlankAndBoundLinkersToStack() {
		EnderLinkerItem item = item();
		ItemStack blank = new ItemStack(Items.BLAZE_ROD, 64), bound = new ItemStack(Items.BLAZE_ROD);
		LinkedStorageStackData.setLinkerTarget(bound, new EnderLinkerTargetData(UUID.randomUUID(), Component.empty()));
		assertEquals(64, item.getMaxStackSize(blank));
		assertEquals(64, item.getMaxStackSize(bound));
	}
	@Test
	void overrideStackedOnOtherShowsFailureFeedbackForAlreadyLinkedStorage() {
		UUID group = UUID.randomUUID();
		ItemStack linker = new ItemStack(Items.BLAZE_ROD), endpoint = new ItemStack(Items.NETHER_WART);
		LinkedStorageStackData.setLinkerTarget(linker, new EnderLinkerTargetData(group, Component.empty()));
		LinkedStorageEndpointData endpointData = new LinkedStorageEndpointData(group, UUID.randomUUID());
		LinkedStorageStackData.setEndpoint(endpoint, endpointData);
		Player player = Mockito.mock(Player.class);
		Slot slot = Mockito.mock(Slot.class);
		Mockito.when(player.level()).thenReturn(Mockito.mock(ServerLevel.class));
		Mockito.when(slot.getItem()).thenReturn(endpoint);
		assertTrue(item().overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player));
		assertEquals(group, LinkedStorageStackData.getLinkerTarget(linker).groupId());
		assertEquals(endpointData, LinkedStorageStackData.getEndpoint(endpoint));
		Mockito.verify(player).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 1, 0.7F);
		Mockito.verify(player).displayClientMessage(TranslationHelper.INSTANCE.translStatusMessage("ender_linker.already_linked"), true);
	}
	@Test
	void overrideStackedOnOtherBindsBlankLinkerToExistingEndpoint() {
		UUID group = UUID.randomUUID(), endpointId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Player player = Mockito.mock(Player.class);
		Slot slot = Mockito.mock(Slot.class);
		ItemStack endpoint = new ItemStack(Items.NETHER_WART);
		LinkedStorageEndpointData endpointData = new LinkedStorageEndpointData(group, endpointId);
		LinkedStorageStackData.setEndpoint(endpoint, endpointData);
		Mockito.when(player.level()).thenReturn(level);
		Mockito.when(slot.getItem()).thenReturn(endpoint);
		Mockito.when(manager.isEndpointMember(group, endpointId)).thenReturn(true);
		host(manager, group);
		Mockito.when(data.manager()).thenReturn(manager);
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			ItemStack linker = new ItemStack(Items.BLAZE_ROD);
			assertTrue(item().overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player));
			assertEquals(group, LinkedStorageStackData.getLinkerTarget(linker).groupId());
			assertEquals(endpointData, LinkedStorageStackData.getEndpoint(endpoint));
		}
		Mockito.verify(player, Mockito.never()).playNotifySound(Mockito.any(), Mockito.any(), Mockito.anyFloat(), Mockito.anyFloat());
		Mockito.verify(player, Mockito.never()).displayClientMessage(Mockito.any(Component.class), Mockito.anyBoolean());
	}
	@Test
	void overrideStackedOnOtherKeepsBoundLinkerAndShowsFailureFeedbackForIncompatibleEndpoint() {
		UUID group = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		Player player = Mockito.mock(Player.class);
		Slot slot = Mockito.mock(Slot.class);
		TestAdapter adapter = new TestAdapter() {
			@Override
			public boolean supports(ItemStack stack) {
				return stack.is(Items.STRING);
			}
			@Override
			public boolean isCompatible(ServerLevel serverLevel, ItemStack endpoint, LinkedStorageHostDescriptor descriptor) {
				return false;
			}
		};
		LinkedStorageEndpointAdapters.register(adapter);
		Mockito.when(player.level()).thenReturn(level);
		Mockito.when(slot.getItem()).thenReturn(new ItemStack(Items.STRING));
		Mockito.when(manager.usesHostFactory(group, adapter.factoryId())).thenReturn(true);
		Mockito.when(manager.getHostDescriptor(group)).thenReturn(Optional.of(new LinkedStorageHostDescriptor(adapter.factoryId(), new CompoundTag())));
		Mockito.when(data.manager()).thenReturn(manager);
		ItemStack linker = new ItemStack(Items.BLAZE_ROD);
		EnderLinkerTargetData target = new EnderLinkerTargetData(group, Component.empty());
		LinkedStorageStackData.setLinkerTarget(linker, target);
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			assertTrue(item().overrideStackedOnOther(linker, slot, ClickAction.SECONDARY, player));
		}
		Mockito.verify(player).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 1, 0.7F);
		Mockito.verify(player).displayClientMessage(Mockito.any(Component.class), Mockito.eq(true));
		assertEquals(target, LinkedStorageStackData.getLinkerTarget(linker));
	}
	@Test
	void createSecondaryEndpointCopyCreatesNewSecondaryWithoutChangingSource() {
		UUID group = UUID.randomUUID(), sourceId = UUID.randomUUID();
		LinkedStorageGroupManager manager = Mockito.mock(LinkedStorageGroupManager.class);
		LinkedStorageGroupsSavedData data = Mockito.mock(LinkedStorageGroupsSavedData.class);
		ServerLevel level = Mockito.mock(ServerLevel.class);
		TestAdapter adapter = new TestAdapter();
		LinkedStorageEndpointAdapters.register(adapter);
		Mockito.when(manager.isEndpointMember(group, sourceId)).thenReturn(true);
		Mockito.when(manager.usesHostFactory(group, adapter.factoryId())).thenReturn(true);
		Mockito.when(data.manager()).thenReturn(manager);
		ItemStack source = new ItemStack(Items.BONE);
		LinkedStorageEndpointData sourceEndpoint = new LinkedStorageEndpointData(group, sourceId);
		LinkedStorageStackData.setEndpoint(source, sourceEndpoint);
		try (MockedStatic<LinkedStorageGroupsSavedData> groups = Mockito.mockStatic(LinkedStorageGroupsSavedData.class)) {
			groups.when(() -> LinkedStorageGroupsSavedData.get(level)).thenReturn(data);
			ItemStack copy = LinkedStorageService.createSecondaryEndpointCopy(level, source).orElseThrow();
			LinkedStorageEndpointData copied = LinkedStorageStackData.getEndpoint(copy);
			assertEquals(sourceEndpoint, LinkedStorageStackData.getEndpoint(source));
			assertEquals(group, copied.groupId());
			assertFalse(sourceId.equals(copied.endpointId()));
		}
	}
	private static EnderLinkerItem item() {
		return Mockito.mock(EnderLinkerItem.class, Mockito.CALLS_REAL_METHODS);
	}
	private static Player player(ServerLevel level, UUID id) {
		Player player = Mockito.mock(Player.class);
		Inventory inventory = Mockito.mock(Inventory.class);
		Mockito.when(player.level()).thenReturn(level);
		Mockito.when(player.getUUID()).thenReturn(id);
		Mockito.when(player.getInventory()).thenReturn(inventory);
		Mockito.when(inventory.add(Mockito.any(ItemStack.class))).thenReturn(true);
		return player;
	}
	private static void host(LinkedStorageGroupManager manager, UUID group) {
		ILinkedStorageVirtualHost host = Mockito.mock(ILinkedStorageVirtualHost.class);
		Mockito.when(host.getLinkedStorageDisplayName()).thenReturn(Optional.empty());
		Mockito.when(manager.resolveVirtualHost(group)).thenReturn(Optional.of(host));
	}
	private static class TestAdapter implements ILinkedStorageItemEndpointAdapter {
		private static final ResourceLocation FACTORY_ID = new ResourceLocation("sophisticatedcore", "test_endpoint_adapter");
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
			LinkedStorageStackData.setEndpoint(stack, endpoint);
		}
	}
}
