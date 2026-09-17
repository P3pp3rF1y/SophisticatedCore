package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
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
	void updateContentsKeepsTheCachedContentsAndAcceptsOlderRevisions() {
		UUID groupId = UUID.randomUUID();
		ClientLinkedStorageContents.updateContents(groupId, 7L, new ContainerContents(), Component.literal("Menu storage"), 27, 3, 0);
		ILinkedStorageContents contents = ClientLinkedStorageContents.getContents(groupId).orElseThrow();

		ClientLinkedStorageContents.updateContents(groupId, 8L, new ContainerContents(), Component.literal("Payload storage"), 45, 5, 3);

		assertSame(contents, ClientLinkedStorageContents.getContents(groupId).orElseThrow());
		assertEquals(8L, ClientLinkedStorageContents.getRevision(groupId).orElseThrow());
		assertEquals(45, ClientLinkedStorageContents.getInventorySlots(groupId).orElseThrow());

		ClientLinkedStorageContents.updateContents(groupId, 6L, new ContainerContents(), Component.literal("Older storage"), 9, 1, 1);

		assertSame(contents, ClientLinkedStorageContents.getContents(groupId).orElseThrow());
		assertEquals(6L, ClientLinkedStorageContents.getRevision(groupId).orElseThrow());
		assertEquals(9, ClientLinkedStorageContents.getInventorySlots(groupId).orElseThrow());
	}
}
