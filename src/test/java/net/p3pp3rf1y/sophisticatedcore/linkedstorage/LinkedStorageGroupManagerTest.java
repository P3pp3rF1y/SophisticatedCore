package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedStorageGroupManagerTest {
	private static final ResourceLocation FACTORY_ID = new ResourceLocation("test", "linked_storage");
	private static final ResourceLocation SUBSCRIPTION_FACTORY_ID = new ResourceLocation("test", "linked_storage_subscription");

	@Test
	void persistsContentsEndpointsAndRevisions() {
		LinkedStorageHostFactories.register(FACTORY_ID, (contents, carrier) -> new ILinkedStorageVirtualHost() {
		});
		LinkedStorageGroupsSavedData savedData = new LinkedStorageGroupsSavedData();
		LinkedStorageGroupManager manager = savedData.manager();
		UUID owner = UUID.randomUUID();
		UUID primary = UUID.randomUUID();
		CompoundTag contents = new CompoundTag();
		contents.putInt("slots", 27);
		UUID groupId = manager.createGroup(owner, primary, new LinkedStorageHostDescriptor(FACTORY_ID, new CompoundTag()), contents);
		UUID secondary = UUID.randomUUID();
		manager.registerEndpoint(groupId, secondary);
		manager.resolveContents(groupId).orElseThrow().markChanged();

		LinkedStorageGroupsSavedData loaded = LinkedStorageGroupsSavedData.load(savedData.save(new CompoundTag()));
		LinkedStorageGroupManager loadedManager = loaded.manager();
		assertTrue(loadedManager.isPrimaryEndpoint(groupId, primary));
		assertTrue(loadedManager.isEndpointMember(groupId, secondary));
		assertEquals(2, loadedManager.getRevision(groupId));
		assertEquals(27, loadedManager.resolveContents(groupId).orElseThrow().getContents().getInt("slots"));
	}

	@Test
	void nbtDataRoundTripKeepsEndpointAndLinkerState() {
		UUID groupId = UUID.randomUUID();
		UUID endpointId = UUID.randomUUID();
		LinkedStorageEndpointData endpoint = new LinkedStorageEndpointData(groupId, endpointId);
		EnderLinkerTargetData target = new EnderLinkerTargetData(groupId, net.minecraft.network.chat.Component.literal("Linked Storage"));
		EnderLinkPendingCraftData pending = new EnderLinkPendingCraftData(EnderLinkPendingCraftPlan.ADD_SECONDARY, UUID.randomUUID());

		assertEquals(endpoint, LinkedStorageEndpointData.load(endpoint.save()));
		assertEquals(target, EnderLinkerTargetData.load(target.save()));
		assertEquals(pending, EnderLinkPendingCraftData.load(pending.save()));
	}

	@Test
	void subscribeToGroupChangesReportsProjectionChangesAndUnsubscribes() {
		LinkedStorageHostFactories.register(SUBSCRIPTION_FACTORY_ID, (contents, carrier) -> new ILinkedStorageVirtualHost() {
		});
		LinkedStorageGroupManager manager = new LinkedStorageGroupsSavedData().manager();
		UUID groupId = manager.createGroup(UUID.randomUUID(), UUID.randomUUID(), new LinkedStorageHostDescriptor(SUBSCRIPTION_FACTORY_ID, new CompoundTag()),
				new CompoundTag());
		AtomicReference<LinkedStorageGroupManager.GroupChange> change = new AtomicReference<>();
		Runnable unsubscribe = manager.subscribeToGroupChanges(groupId, change::set);

		manager.resolveContents(groupId).orElseThrow().markChanged();
		assertEquals(new LinkedStorageGroupManager.GroupChange(true, false, false), change.get());
		assertTrue(!change.get().projectionChanged());

		manager.resolveContents(groupId).orElseThrow().setColumnsTaken(1);
		assertEquals(new LinkedStorageGroupManager.GroupChange(true, false, true), change.get());
		assertTrue(change.get().projectionChanged());

		unsubscribe.run();
		change.set(null);
		manager.resolveContents(groupId).orElseThrow().markChanged();
		assertEquals(null, change.get());
	}
}
