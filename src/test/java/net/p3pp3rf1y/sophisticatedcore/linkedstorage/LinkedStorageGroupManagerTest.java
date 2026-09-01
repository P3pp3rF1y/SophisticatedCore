package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedStorageGroupManagerTest {
	@Test
	void resolveVirtualHostResolvesEndpointKeysToCachedVirtualHost() {
		ResourceLocation factoryId = ResourceLocation.fromNamespaceAndPath("sophisticatedcore", "endpoint_key_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupManager manager = new LinkedStorageGroupsSavedData().manager();
		UUID primaryEndpointId = UUID.randomUUID();
		UUID secondaryEndpointId = UUID.randomUUID();
		UUID groupId = manager.createGroup(UUID.randomUUID(), primaryEndpointId, new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new CompoundTag());
		manager.registerEndpoint(groupId, secondaryEndpointId);

		ILinkedStorageVirtualHost host = manager.resolveVirtualHost(groupId).orElseThrow();
		assertSame(host, manager.resolveVirtualHost(new LinkedStorageEndpointData(groupId, primaryEndpointId), true).orElseThrow());
		assertSame(host, manager.resolveVirtualHost(new LinkedStorageEndpointData(groupId, secondaryEndpointId), false).orElseThrow());
		assertTrue(manager.resolveVirtualHost(new LinkedStorageEndpointData(groupId, secondaryEndpointId), true).isEmpty());
	}

	@Test
	void setContentsRefreshesVirtualHostOnlyAfterFullRootReplacement() {
		ResourceLocation factoryId = ResourceLocation.fromNamespaceAndPath("sophisticatedcore", "replacement_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupManager manager = new LinkedStorageGroupsSavedData().manager();
		UUID groupId = manager.createGroup(UUID.randomUUID(), UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new CompoundTag());
		TestHost host = (TestHost) manager.resolveVirtualHost(groupId).orElseThrow();
		ILinkedStorageContentsBinding contents = manager.resolveContents(groupId).orElseThrow();

		contents.contents().putString("mutation", "retained");
		contents.markDirty();
		contents.setContents(contents.groupId(), new CompoundTag());

		assertEquals(1, host.refreshes());
	}

	@Test
	void markRenderDirtyUpdatesPersistedRenderRevision() {
		ResourceLocation factoryId = ResourceLocation.fromNamespaceAndPath("sophisticatedcore", "render_revision_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupsSavedData savedData = new LinkedStorageGroupsSavedData();
		LinkedStorageGroupManager manager = savedData.manager();
		UUID groupId = manager.createGroup(UUID.randomUUID(), UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new CompoundTag());
		ILinkedStorageContentsBinding contents = manager.resolveContents(groupId).orElseThrow();

		contents.markRenderDirty();

		assertEquals(1, manager.getRevision(groupId));
		assertEquals(1, manager.getRenderRevision(groupId));
		LinkedStorageGroupsSavedData loaded = LinkedStorageGroupsSavedData.load(savedData.save(new CompoundTag(), Mockito.mock()), Mockito.mock());
		assertEquals(1, loaded.manager().getRenderRevision(groupId));
	}

	@Test
	void setColumnsTakenPersistsCanonicalColumnsAndRefreshesVirtualHostBeforeFanOut() {
		ResourceLocation factoryId = ResourceLocation.fromNamespaceAndPath("sophisticatedcore", "layout_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupsSavedData savedData = new LinkedStorageGroupsSavedData();
		LinkedStorageGroupManager manager = savedData.manager();
		UUID groupId = manager.createGroup(UUID.randomUUID(), UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new CompoundTag());
		TestHost host = (TestHost) manager.resolveVirtualHost(groupId).orElseThrow();
		AtomicInteger notifications = new AtomicInteger();

		manager.subscribeToGroupChanges(groupId, () -> {
			assertEquals(2, host.contents().getColumnsTaken());
			assertEquals(1, host.layoutRefreshes());
			notifications.incrementAndGet();
		});
		manager.resolveContents(groupId).orElseThrow().setColumnsTaken(2);

		assertEquals(2, manager.resolveContents(groupId).orElseThrow().getColumnsTaken());
		assertEquals(1, manager.getRevision(groupId));
		assertEquals(1, notifications.get());
		assertEquals(2, LinkedStorageGroupsSavedData.load(savedData.save(new CompoundTag(), Mockito.mock()), Mockito.mock()).manager().resolveContents(groupId)
				.orElseThrow().getColumnsTaken());
	}

	@Test
	void updatePrimaryHostDescriptorUpdatesOnlyPrimaryVirtualCarrier() {
		ResourceLocation factoryId = ResourceLocation.fromNamespaceAndPath("sophisticatedcore", "primary_carrier_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupManager manager = new LinkedStorageGroupsSavedData().manager();
		UUID primaryEndpointId = UUID.randomUUID();
		UUID groupId = manager.createGroup(UUID.randomUUID(), primaryEndpointId, new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new CompoundTag());
		TestHost host = (TestHost) manager.resolveVirtualHost(groupId).orElseThrow();
		CompoundTag updatedCarrier = new CompoundTag();
		updatedCarrier.putString("name", "Primary Backpack");

		assertFalse(manager.updatePrimaryHostDescriptor(groupId, UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, updatedCarrier)));
		assertTrue(manager.updatePrimaryHostDescriptor(groupId, primaryEndpointId, new LinkedStorageHostDescriptor(factoryId, updatedCarrier)));

		assertEquals("Primary Backpack", host.virtualCarrier.getString("name"));
		assertEquals("Primary Backpack", manager.getHostDescriptor(groupId).orElseThrow().virtualCarrier().getString("name"));
	}

	@Test
	void resolveContentsAndResolveVirtualHostReturnEmptyForUnknownGroups() {
		LinkedStorageGroupManager manager = new LinkedStorageGroupsSavedData().manager();

		assertTrue(manager.resolveContents(UUID.randomUUID()).isEmpty());
		assertTrue(manager.resolveVirtualHost(UUID.randomUUID()).isEmpty());
	}

	@Test
	void clearRetainsUnrelatedModelData() {
		ItemStack stack = new ItemStack(Items.STICK);
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(42F), List.of(), List.of(), List.of()));
		stack.set(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));

		LinkedStorageStackLifecycle.clear(stack);

		assertEquals(42F, stack.get(DataComponents.CUSTOM_MODEL_DATA).floats().getFirst());
	}

	@Test
	void saveAndLoadPersistCanonicalRootAndRevision() {
		ResourceLocation factoryId = ResourceLocation.fromNamespaceAndPath("sophisticatedcore", "persisted_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupsSavedData savedData = new LinkedStorageGroupsSavedData();
		LinkedStorageGroupManager manager = savedData.manager();
		CompoundTag virtualCarrier = new CompoundTag();
		virtualCarrier.putString("render", "current");
		UUID groupId = manager.createGroup(UUID.randomUUID(), UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, virtualCarrier), new CompoundTag());
		TestHost host = (TestHost) manager.resolveVirtualHost(groupId).orElseThrow();
		host.virtualCarrier.putString("render", "refreshed");
		ILinkedStorageContentsBinding contents = manager.resolveContents(groupId).orElseThrow();
		contents.setContents(contents.groupId(), new CompoundTag());
		contents.contents().putString("canonical", "contents");
		contents.markDirty();
		contents.markRenderDirty();

		LinkedStorageGroupsSavedData loaded = LinkedStorageGroupsSavedData.load(savedData.save(new CompoundTag(), Mockito.mock()), Mockito.mock());
		assertEquals(3, loaded.manager().getRevision(groupId));
		assertEquals("contents", loaded.manager().resolveContents(groupId).orElseThrow().contents().getString("canonical"));
		assertEquals("refreshed", loaded.manager().getHostDescriptor(groupId).orElseThrow().virtualCarrier().getString("render"));
	}

	@Test
	void registerEndpointAndUnregisterEndpointPersistMembershipLedger() {
		ResourceLocation factoryId = ResourceLocation.fromNamespaceAndPath("sophisticatedcore", "membership_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupsSavedData savedData = new LinkedStorageGroupsSavedData();
		LinkedStorageGroupManager manager = savedData.manager();
		UUID primaryEndpointId = UUID.randomUUID();
		UUID secondaryEndpointId = UUID.randomUUID();
		UUID groupId = manager.createGroup(UUID.randomUUID(), primaryEndpointId, new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new CompoundTag());

		AtomicInteger notifications = new AtomicInteger();
		manager.subscribeToGroupChanges(groupId, notifications::incrementAndGet);
		manager.registerEndpoint(groupId, secondaryEndpointId);
		assertEquals(1, manager.getRevision(groupId));
		assertEquals(1, notifications.get());
		assertTrue(manager.unregisterEndpoint(groupId, secondaryEndpointId));
		assertEquals(2, manager.getRevision(groupId));
		assertEquals(2, notifications.get());
		manager.registerEndpoint(groupId, secondaryEndpointId);
		assertEquals(3, manager.getRevision(groupId));
		assertEquals(3, notifications.get());

		LinkedStorageGroupManager loaded = LinkedStorageGroupsSavedData.load(savedData.save(new CompoundTag(), Mockito.mock()), Mockito.mock()).manager();
		assertTrue(loaded.isEndpointMember(groupId, primaryEndpointId));
		assertTrue(loaded.isPrimaryEndpoint(groupId, primaryEndpointId));
		assertTrue(loaded.isEndpointMember(groupId, secondaryEndpointId));
		assertFalse(loaded.isPrimaryEndpoint(groupId, secondaryEndpointId));
	}

	@Test
	void recordEndpointOpenedPersistsOwnershipAndEndpointAccessWithoutChangingContentRevision() {
		ResourceLocation factoryId = ResourceLocation.fromNamespaceAndPath("sophisticatedcore", "ownership_test_host_" + UUID.randomUUID());
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupsSavedData savedData = new LinkedStorageGroupsSavedData();
		LinkedStorageGroupManager manager = savedData.manager();
		UUID ownerId = UUID.randomUUID();
		UUID otherPlayerId = UUID.randomUUID();
		UUID endpointId = UUID.randomUUID();
		UUID groupId = manager.createGroup(ownerId, endpointId, new LinkedStorageHostDescriptor(factoryId, new CompoundTag()), new CompoundTag());

		assertEquals(ownerId, savedData.findGroup(groupId).orElseThrow().ownerId());
		assertFalse(otherPlayerId.equals(savedData.findGroup(groupId).orElseThrow().ownerId()));
		manager.recordEndpointOpened(groupId, endpointId, otherPlayerId, 1200L);
		assertEquals(0L, manager.getRevision(groupId));
		assertEquals(otherPlayerId, getEndpoint(savedData, groupId, endpointId).lastOpenedBy());
		assertEquals(1200L, getEndpoint(savedData, groupId, endpointId).lastOpenedAt());

		LinkedStorageGroupsSavedData loaded = LinkedStorageGroupsSavedData.load(savedData.save(new CompoundTag(), Mockito.mock()), Mockito.mock());
		assertEquals(ownerId, loaded.findGroup(groupId).orElseThrow().ownerId());
		assertEquals(otherPlayerId, getEndpoint(loaded, groupId, endpointId).lastOpenedBy());
		assertEquals(1200L, getEndpoint(loaded, groupId, endpointId).lastOpenedAt());
	}

	private static LinkedStorageEndpointRecord getEndpoint(LinkedStorageGroupsSavedData savedData, UUID groupId, UUID endpointId) {
		return savedData.findGroup(groupId).orElseThrow().endpoints().stream().filter(endpoint -> endpoint.endpointId().equals(endpointId)).findFirst()
				.orElseThrow();
	}

	private static class TestHost implements ILinkedStorageVirtualHost {
		private final ILinkedStorageContentsBinding contents;
		private CompoundTag virtualCarrier;
		private int refreshes;
		private int layoutRefreshes;
		private int snapshots;

		private TestHost(ILinkedStorageContentsBinding contents, CompoundTag virtualCarrier) {
			this.contents = contents;
			this.virtualCarrier = virtualCarrier;
		}

		private ILinkedStorageContentsBinding contents() {
			return contents;
		}

		private int refreshes() {
			return refreshes;
		}

		private int layoutRefreshes() {
			return layoutRefreshes;
		}

		private int snapshots() {
			return snapshots;
		}

		@Override
		public void onLinkedStorageContentsChanged() {
			refreshes++;
		}

		@Override
		public void onLinkedStorageLayoutChanged() {
			layoutRefreshes++;
		}

		@Override
		public Optional<CompoundTag> getVirtualCarrierSnapshot() {
			snapshots++;
			return Optional.of(virtualCarrier.copy());
		}

		@Override
		public void onVirtualCarrierChanged(CompoundTag updatedVirtualCarrier) {
			virtualCarrier = updatedVirtualCarrier.copy();
		}
	}
}
