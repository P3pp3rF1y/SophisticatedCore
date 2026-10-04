package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.settings.main.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientLinkedStorageContentsTest {
	@AfterEach
	void clearSnapshots() {
		ClientLinkedStorageContents.clear();
	}

	@Test
	void updateContentsReplacesContents() {
		UUID groupId = UUID.randomUUID();
		ContainerContents currentContents = new ContainerContents();
		currentContents.inventory().stacks().add(new ItemStack(Items.STICK));

		ClientLinkedStorageContents.updateContents(groupId, 4L, currentContents, Component.literal("Current linked storage"), 36, 4, 2);

		assertEquals(1, ClientLinkedStorageContents.getContents(groupId).orElseThrow().contents().inventory().stacks().size());
		assertEquals("Current linked storage", ClientLinkedStorageContents.getGroupName(groupId).orElseThrow().getString());
		assertEquals(4L, ClientLinkedStorageContents.getRevision(groupId).orElseThrow());
		assertEquals(36, ClientLinkedStorageContents.getInventorySlots(groupId).orElseThrow());
		assertEquals(4, ClientLinkedStorageContents.getUpgradeSlots(groupId).orElseThrow());
		assertEquals(2, ClientLinkedStorageContents.getColumnsTaken(groupId).orElseThrow());
	}

	@Test
	void updateContentsUpdatesExistingContentsRegardlessOfRevision() {
		UUID groupId = UUID.randomUUID();
		ContainerContents currentContents = new ContainerContents();
		currentContents.inventory().stacks().add(new ItemStack(Items.STICK));
		ClientLinkedStorageContents.updateContents(groupId, 8L, currentContents, Component.literal("Menu storage"), 27, 3, 0);
		ILinkedStorageContents contents = ClientLinkedStorageContents.getContents(groupId).orElseThrow();
		ClientLinkedStorageContents.updateContents(groupId, 7L, new ContainerContents(), Component.literal("Payload storage"), 45, 5, 3);

		assertSame(contents, ClientLinkedStorageContents.getContents(groupId).orElseThrow());
		assertEquals(0, contents.contents().inventory().stacks().size());
		assertEquals(7L, ClientLinkedStorageContents.getRevision(groupId).orElseThrow());
		assertEquals(45, ClientLinkedStorageContents.getInventorySlots(groupId).orElseThrow());
	}

	@Test
	void settingsUpdatePreservesSnapshotRevisionAndInventory() {
		UUID groupId = UUID.randomUUID();
		ContainerContents initial = new ContainerContents();
		initial.inventory().stacks().add(new ItemStack(Items.DIAMOND));
		ClientLinkedStorageContents.updateContents(groupId, 7L, initial, Component.literal("Storage"), 27, 3, 2);
		ILinkedStorageContents contents = ClientLinkedStorageContents.getContents(groupId).orElseThrow();
		ClientLinkedStorageContents.removeUpdatedGroup(groupId);

		ContainerContents.SettingsData settings = new ContainerContents.SettingsData();
		settings.setMainSettingsContext(Context.CONTAINER);
		ClientLinkedStorageContents.updateSettings(groupId, settings);
		boolean settingsWereUpdated = ClientLinkedStorageContents.removeUpdatedSettings(groupId);
		boolean groupWasUpdated = ClientLinkedStorageContents.removeUpdatedGroup(groupId);

		assertSame(contents, ClientLinkedStorageContents.getContents(groupId).orElseThrow());
		assertTrue(contents.contents().inventory().stacks().getFirst().is(Items.DIAMOND));
		assertEquals(Context.CONTAINER, contents.contents().settings().mainSettingsContext());
		assertEquals(7L, ClientLinkedStorageContents.getRevision(groupId).orElseThrow());
		assertEquals(27, ClientLinkedStorageContents.getInventorySlots(groupId).orElseThrow());
		assertEquals(3, ClientLinkedStorageContents.getUpgradeSlots(groupId).orElseThrow());
		assertEquals(2, ClientLinkedStorageContents.getColumnsTaken(groupId).orElseThrow());
		assertTrue(settingsWereUpdated);
		assertFalse(groupWasUpdated);
	}
}
