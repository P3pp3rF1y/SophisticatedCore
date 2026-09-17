package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ClientLinkedStorageContentsTest {
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

		assertEquals("value", ClientLinkedStorageContents.getContents(groupId).orElseThrow().contents().getString("current"));
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
		assertEquals("payload", contents.contents().getString("source"));
		assertEquals(45, ClientLinkedStorageContents.getInventorySlots(groupId).orElseThrow());
	}

	@Test
	void updateContentsAcceptsOlderRevision() {
		UUID groupId = UUID.randomUUID();
		CompoundTag newerContents = new CompoundTag();
		newerContents.putString("source", "newer");
		CompoundTag olderContents = new CompoundTag();
		olderContents.putString("source", "older");

		ClientLinkedStorageContents.updateContents(groupId, 8L, newerContents, Component.literal("Newer storage"), 45, 5, 3);
		ClientLinkedStorageContents.updateContents(groupId, 7L, olderContents, Component.literal("Older storage"), 27, 3, 0);

		assertEquals(7L, ClientLinkedStorageContents.getRevision(groupId).orElseThrow());
		assertEquals("older", ClientLinkedStorageContents.getContents(groupId).orElseThrow().contents().getString("source"));
	}
}
