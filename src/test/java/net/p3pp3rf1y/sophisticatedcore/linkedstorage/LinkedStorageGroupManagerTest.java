package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

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
		ResourceLocation factoryId = factoryId("endpoint_key_test_host");
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
		LinkedStorageGroupManager manager = managerWithHost("replacement_test_host");
		UUID groupId = createGroup(manager, "replacement_test_host");
		TestHost host = (TestHost) manager.resolveVirtualHost(groupId).orElseThrow();
		ILinkedStorageContents contents = manager.resolveContents(groupId).orElseThrow();

		contents.getContents().putString("mutation", "retained");
		contents.markChanged();
		contents.setContents(new CompoundTag());

		assertEquals(1, host.refreshes());
	}

	@Test
	void subscribeToRootContentsReplacementsIgnoresOrdinaryContentsMutationsAndSupportsUnsubscribe() {
		LinkedStorageGroupManager manager = managerWithHost("root_listener_test_host");
		UUID groupId = createGroup(manager, "root_listener_test_host");
		AtomicInteger notifications = new AtomicInteger();
		Runnable unsubscribe = manager.subscribeToRootContentsReplacements(groupId, notifications::incrementAndGet);
		ILinkedStorageContents contents = manager.resolveContents(groupId).orElseThrow();

		contents.getContents().putString("ordinaryMutation", "retained");
		contents.markChanged();
		assertEquals(0, notifications.get());
		contents.setContents(new CompoundTag());
		assertEquals(1, notifications.get());
		unsubscribe.run();
		contents.setContents(new CompoundTag());
		assertEquals(1, notifications.get());
	}

	@Test
	void markRenderDirtyUpdatesPersistedRenderRevision() {
		ResourceLocation factoryId = factoryId("render_revision_test_host");
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupsSavedData savedData = new LinkedStorageGroupsSavedData();
		LinkedStorageGroupManager manager = savedData.manager();
		UUID groupId = manager.createGroup(UUID.randomUUID(), UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, new CompoundTag()),
				new CompoundTag());

		manager.resolveContents(groupId).orElseThrow().markRenderDirty();

		assertEquals(1, manager.getRevision(groupId));
		assertEquals(1, manager.getRenderRevision(groupId));
		assertEquals(1, LinkedStorageGroupsSavedData.load(savedData.save(new CompoundTag())).manager().getRenderRevision(groupId));
	}

	@Test
	void setColumnsTakenPersistsCanonicalColumnsAndRefreshesVirtualHostBeforeFanOut() {
		ResourceLocation factoryId = factoryId("layout_test_host");
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
		assertEquals(2,
				LinkedStorageGroupsSavedData.load(savedData.save(new CompoundTag())).manager().resolveContents(groupId).orElseThrow().getColumnsTaken());
	}

	@Test
	void updatePrimaryHostDescriptorUpdatesOnlyPrimaryVirtualCarrier() {
		ResourceLocation factoryId = factoryId("primary_carrier_test_host");
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
		stack.getOrCreateTag().putInt("CustomModelData", 42);
		LinkedStorageStackData.setEndpoint(stack, new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID()));

		LinkedStorageStackLifecycle.clear(stack);

		assertEquals(42, stack.getTag().getInt("CustomModelData"));
	}

	@Test
	void saveAndLoadPersistCanonicalRootAndRevision() {
		ResourceLocation factoryId = factoryId("persisted_test_host");
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupsSavedData savedData = new LinkedStorageGroupsSavedData();
		LinkedStorageGroupManager manager = savedData.manager();
		CompoundTag virtualCarrier = new CompoundTag();
		virtualCarrier.putString("render", "current");
		UUID groupId = manager.createGroup(UUID.randomUUID(), UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, virtualCarrier), new CompoundTag());
		TestHost host = (TestHost) manager.resolveVirtualHost(groupId).orElseThrow();
		host.virtualCarrier.putString("render", "refreshed");
		ILinkedStorageContents contents = manager.resolveContents(groupId).orElseThrow();
		contents.setContents(new CompoundTag());
		contents.getContents().putString("canonical", "contents");
		contents.markChanged();
		contents.markRenderDirty();

		assertEquals(1, host.snapshots());
		LinkedStorageGroupsSavedData loaded = LinkedStorageGroupsSavedData.load(savedData.save(new CompoundTag()));
		assertEquals(3, loaded.manager().getRevision(groupId));
		assertEquals("contents", loaded.manager().resolveContents(groupId).orElseThrow().getContents().getString("canonical"));
		assertEquals("refreshed", loaded.manager().getHostDescriptor(groupId).orElseThrow().virtualCarrier().getString("render"));
	}

	@Test
	void registerEndpointAndUnregisterEndpointPersistMembershipLedger() {
		ResourceLocation factoryId = factoryId("membership_test_host");
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

		LinkedStorageGroupManager loaded = LinkedStorageGroupsSavedData.load(savedData.save(new CompoundTag())).manager();
		assertTrue(loaded.isEndpointMember(groupId, primaryEndpointId));
		assertTrue(loaded.isPrimaryEndpoint(groupId, primaryEndpointId));
		assertTrue(loaded.isEndpointMember(groupId, secondaryEndpointId));
		assertFalse(loaded.isPrimaryEndpoint(groupId, secondaryEndpointId));
	}

	@Test
	void detachLostEndpointRetainsGroupContentsWhenLastEndpointIsLost() {
		ResourceLocation factoryId = factoryId("lost_primary_test_host");
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		LinkedStorageGroupManager manager = new LinkedStorageGroupsSavedData().manager();
		UUID primaryId = UUID.randomUUID();
		CompoundTag initialContents = new CompoundTag();
		initialContents.putInt("diamonds", 7);
		UUID groupId = manager.createGroup(UUID.randomUUID(), primaryId, new LinkedStorageHostDescriptor(factoryId, new CompoundTag()), initialContents);

		boolean detachedPrimary = manager.detachLostEndpoint(groupId, primaryId);

		assertTrue(detachedPrimary);
		assertFalse(manager.isEndpointMember(groupId, primaryId));
		assertEquals(7, manager.resolveContents(groupId).orElseThrow().getContents().getInt("diamonds"));
	}

	@Test
	void recordEndpointOpenedPersistsOwnershipAndEndpointAccessWithoutChangingContentRevision() {
		ResourceLocation factoryId = factoryId("ownership_test_host");
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
		assertEquals(otherPlayerId, endpoint(savedData, groupId, endpointId).lastOpenedBy());
		assertEquals(1200L, endpoint(savedData, groupId, endpointId).lastOpenedAt());

		LinkedStorageGroupsSavedData loaded = LinkedStorageGroupsSavedData.load(savedData.save(new CompoundTag()));
		assertEquals(ownerId, loaded.findGroup(groupId).orElseThrow().ownerId());
		assertEquals(otherPlayerId, endpoint(loaded, groupId, endpointId).lastOpenedBy());
		assertEquals(1200L, endpoint(loaded, groupId, endpointId).lastOpenedAt());
	}

	private static ResourceLocation factoryId(String path) {
		return new ResourceLocation("sophisticatedcore", path + "_" + UUID.randomUUID());
	}

	private static LinkedStorageGroupManager managerWithHost(String path) {
		return new LinkedStorageGroupsSavedData().manager();
	}

	private static UUID createGroup(LinkedStorageGroupManager manager, String path) {
		ResourceLocation factoryId = factoryId(path);
		LinkedStorageHostFactories.register(factoryId, TestHost::new);
		return manager.createGroup(UUID.randomUUID(), UUID.randomUUID(), new LinkedStorageHostDescriptor(factoryId, new CompoundTag()), new CompoundTag());
	}

	private static LinkedStorageEndpointRecord endpoint(LinkedStorageGroupsSavedData savedData, UUID groupId, UUID endpointId) {
		return savedData.findGroup(groupId).orElseThrow().endpoints().stream().filter(endpoint -> endpoint.endpointId().equals(endpointId)).findFirst()
				.orElseThrow();
	}

	private static class TestHost implements ILinkedStorageVirtualHost {
		private final ILinkedStorageContents contents;
		private CompoundTag virtualCarrier;
		private int refreshes;
		private int layoutRefreshes;
		private int snapshots;

		private TestHost(ILinkedStorageContents contents, CompoundTag virtualCarrier) {
			this.contents = contents;
			this.virtualCarrier = virtualCarrier;
		}

		private ILinkedStorageContents contents() {
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
