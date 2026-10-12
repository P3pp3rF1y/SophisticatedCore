package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientLinkedStorageContentsTest {
	@Test
	void hostDescriptorCopiesCarrierAndSurvivesContentsAndSettingsUpdates() {
		UUID groupId = UUID.randomUUID();
		CompoundTag descriptor = new CompoundTag();
		descriptor.putString(LinkedStorageHostDescriptor.FACTORY_ID_TAG, "sophisticatedstorage:storage");
		ClientLinkedStorageContents.updateHostDescriptor(groupId, descriptor);
		descriptor.putString(LinkedStorageHostDescriptor.FACTORY_ID_TAG, "changed");
		ClientLinkedStorageContents.updateContents(groupId, 1, new CompoundTag(), Component.literal("Chest"), 54, 3, 0);
		ClientLinkedStorageContents.updateSettings(groupId, new CompoundTag());
		ClientLinkedStorageContents.getHostDescriptor(groupId).orElseThrow().putString(LinkedStorageHostDescriptor.FACTORY_ID_TAG, "changed again");

		assertEquals("sophisticatedstorage:storage",
				ClientLinkedStorageContents.getHostDescriptor(groupId).orElseThrow().getString(LinkedStorageHostDescriptor.FACTORY_ID_TAG));
	}

	@Test
	void storageHostDescriptorUsesLatestSnapshotProfileRegardlessOfPacketOrder() {
		UUID groupId = UUID.randomUUID();
		CompoundTag descriptor = new CompoundTag();
		descriptor.putString(LinkedStorageHostDescriptor.FACTORY_ID_TAG, "sophisticatedstorage:storage");
		CompoundTag carrier = new CompoundTag();
		carrier.putString("storageType", "chest");
		carrier.putInt("inventorySlots", 27);
		descriptor.put(LinkedStorageHostDescriptor.VIRTUAL_CARRIER_TAG, carrier);

		ClientLinkedStorageContents.updateContents(groupId, 1, new CompoundTag(), Component.literal("First"), 54, 3, 0);
		ClientLinkedStorageContents.updateHostDescriptor(groupId, descriptor);
		carrier.putInt("inventorySlots", 1);
		CompoundTag first = ClientLinkedStorageContents.getHostDescriptor(groupId).orElseThrow().getCompound(LinkedStorageHostDescriptor.VIRTUAL_CARRIER_TAG);
		assertEquals("chest", first.getString("storageType"));
		assertEquals("First", first.getString("displayName"));
		assertEquals(54, first.getInt("inventorySlots"));
		assertEquals(3, first.getInt("upgradeSlots"));

		ClientLinkedStorageContents.updateContents(groupId, 2, new CompoundTag(), Component.literal("Expanded"), 81, 5, 0);
		CompoundTag expanded = ClientLinkedStorageContents.getHostDescriptor(groupId).orElseThrow()
				.getCompound(LinkedStorageHostDescriptor.VIRTUAL_CARRIER_TAG);
		assertEquals("Expanded", expanded.getString("displayName"));
		assertEquals(81, expanded.getInt("inventorySlots"));
		assertEquals(5, expanded.getInt("upgradeSlots"));
	}

	@Test
	void clearRemovesHostDescriptors() {
		UUID groupId = UUID.randomUUID();
		ClientLinkedStorageContents.updateHostDescriptor(groupId, new CompoundTag());
		ClientLinkedStorageContents.clear();
		assertTrue(ClientLinkedStorageContents.getHostDescriptor(groupId).isEmpty());
	}

	@AfterEach
	void clearSnapshots() {
		ClientLinkedStorageContents.clear();
	}

	@Test
	void updateContentsReplacesContents() {
		UUID groupId = UUID.randomUUID();
		CompoundTag currentContents = new CompoundTag();
		currentContents.putString("current", "value");

		ClientLinkedStorageContents.updateContents(groupId, 4L, currentContents, Component.literal("Current linked storage"), 36, 4, 2);

		assertEquals("value", ClientLinkedStorageContents.getContents(groupId).orElseThrow().getContents().getString("current"));
		assertEquals("Current linked storage", ClientLinkedStorageContents.getGroupName(groupId).orElseThrow().getString());
		assertEquals(4L, ClientLinkedStorageContents.getRevision(groupId).orElseThrow());
		assertEquals(36, ClientLinkedStorageContents.getInventorySlots(groupId).orElseThrow());
		assertEquals(4, ClientLinkedStorageContents.getUpgradeSlots(groupId).orElseThrow());
		assertEquals(2, ClientLinkedStorageContents.getColumnsTaken(groupId).orElseThrow());
	}

	@Test
	void updateContentsUpdatesExistingContents() {
		UUID groupId = UUID.randomUUID();
		CompoundTag payloadContents = new CompoundTag();
		payloadContents.putString("source", "payload");
		ClientLinkedStorageContents.updateContents(groupId, 7L, new CompoundTag(), Component.literal("Menu storage"), 27, 3, 0);
		ILinkedStorageContents contents = ClientLinkedStorageContents.getContents(groupId).orElseThrow();
		ClientLinkedStorageContents.updateContents(groupId, 8L, payloadContents, Component.literal("Payload storage"), 45, 5, 3);

		assertSame(contents, ClientLinkedStorageContents.getContents(groupId).orElseThrow());
		assertEquals("payload", contents.getContents().getString("source"));
		assertEquals(45, ClientLinkedStorageContents.getInventorySlots(groupId).orElseThrow());
	}

	@Test
	void updateContentsAcceptsAnOlderRevision() {
		UUID groupId = UUID.randomUUID();
		ClientLinkedStorageContents.updateContents(groupId, 8L, new CompoundTag(), Component.literal("Newer"), 45, 5, 3);
		CompoundTag olderContents = new CompoundTag();
		olderContents.putString("source", "older packet");

		ClientLinkedStorageContents.updateContents(groupId, 7L, olderContents, Component.literal("Older"), 27, 3, 1);

		assertEquals(7L, ClientLinkedStorageContents.getRevision(groupId).orElseThrow());
		assertEquals("older packet", ClientLinkedStorageContents.getContents(groupId).orElseThrow().getContents().getString("source"));
	}

	@Test
	void settingsUpdatePreservesSnapshotRevisionAndInventory() {
		UUID groupId = UUID.randomUUID();
		CompoundTag initial = new CompoundTag();
		initial.putString("inventory", "diamond");
		initial.put("settings", new CompoundTag());
		ClientLinkedStorageContents.updateContents(groupId, 7L, initial, Component.literal("Storage"), 27, 3, 2);
		ILinkedStorageContents contents = ClientLinkedStorageContents.getContents(groupId).orElseThrow();
		ClientLinkedStorageContents.removeUpdatedGroup(groupId);

		CompoundTag settings = new CompoundTag();
		settings.putBoolean("changed", true);
		ClientLinkedStorageContents.updateSettings(groupId, settings);
		boolean settingsWereUpdated = ClientLinkedStorageContents.removeUpdatedSettings(groupId);
		boolean groupWasUpdated = ClientLinkedStorageContents.removeUpdatedGroup(groupId);

		assertSame(contents, ClientLinkedStorageContents.getContents(groupId).orElseThrow());
		assertEquals("diamond", contents.getContents().getString("inventory"));
		assertTrue(contents.getContents().getCompound("settings").getBoolean("changed"));
		assertEquals(7L, ClientLinkedStorageContents.getRevision(groupId).orElseThrow());
		assertEquals(27, ClientLinkedStorageContents.getInventorySlots(groupId).orElseThrow());
		assertEquals(3, ClientLinkedStorageContents.getUpgradeSlots(groupId).orElseThrow());
		assertEquals(2, ClientLinkedStorageContents.getColumnsTaken(groupId).orElseThrow());
		assertTrue(settingsWereUpdated);
		assertFalse(groupWasUpdated);
	}

	@Test
	void storageNativeSettingsUpdateReplacesNestedRootWithoutChangingInventory() {
		UUID groupId = UUID.randomUUID();
		CompoundTag descriptor = new CompoundTag();
		descriptor.putString(LinkedStorageHostDescriptor.FACTORY_ID_TAG, "sophisticatedstorage:storage");
		ClientLinkedStorageContents.updateHostDescriptor(groupId, descriptor);
		CompoundTag initial = new CompoundTag();
		initial.putString("inventory", "diamond");
		CompoundTag initialSettings = new CompoundTag();
		initialSettings.put("no_sort", new CompoundTag());
		initial.put("settings", initialSettings);
		ClientLinkedStorageContents.updateContents(groupId, 7L, initial, Component.literal("Chest"), 27, 3, 2);
		CompoundTag settings = new CompoundTag();
		settings.putString("custom_setting", "preserved");
		CompoundTag memory = new CompoundTag();
		memory.putBoolean("changed", true);
		settings.put("memory", memory);

		ClientLinkedStorageContents.updateSettings(groupId, settings);

		CompoundTag contents = ClientLinkedStorageContents.getContents(groupId).orElseThrow().getContents();
		assertEquals("diamond", contents.getString("inventory"));
		assertEquals(settings, contents.getCompound("settings"));
		assertTrue(contents.getCompound("settings").getCompound("memory").getBoolean("changed"));
		assertFalse(contents.getCompound("settings").contains("no_sort"));
		assertFalse(contents.contains("memory"));
		assertEquals(7L, ClientLinkedStorageContents.getRevision(groupId).orElseThrow());
	}
}
