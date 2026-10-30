package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IFilterSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.network.ItemStackReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class LinkedStorageEndpointAccessProvidersTest {
	private final UUID groupId = UUID.randomUUID();
	private final UUID endpointId = UUID.randomUUID();
	private ServerPlayer player;
	private StorageContainerMenuBase<?> menu;
	private LinkedStorageGroupManager manager;
	private ItemStack endpoint;
	private ItemStackReference reference;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@BeforeEach
	void prepareEndpoint() {
		try (MockedStatic<ObfuscationReflectionHelper> ignored = mockStatic(ObfuscationReflectionHelper.class)) {
			player = mock(ServerPlayer.class);
			menu = mock(StorageContainerMenuBase.class);
			player.containerMenu = menu;
			manager = mock(LinkedStorageGroupManager.class);
			when(manager.isEndpointMember(groupId, endpointId)).thenReturn(true);
			endpoint = new ItemStack(Items.CHEST);
			LinkedStorageStackData.setEndpoint(endpoint, new LinkedStorageEndpointData(groupId, endpointId));
			when(menu.stillValid(player)).thenReturn(true);
			when(menu.getTotalSlotsNumber()).thenReturn(1);
			when(menu.getSlot(0)).thenReturn(new Slot(new SimpleContainer(endpoint), 0, 0, 0));
			reference = new ItemStackReference(menu.containerId, 0, "", "");
		}
	}

	@Test
	void hasGroupEndpointAllowsRegisteredEndpointInAccessibleMenuSlot() {
		boolean allowed = LinkedStorageEndpointAccessProviders.hasGroupEndpoint(player, manager, groupId, reference);

		assertTrue(allowed);
	}

	@Test
	void hasGroupEndpointAllowsRegisteredEndpointOnCursor() {
		when(menu.getCarried()).thenReturn(endpoint);
		reference = new ItemStackReference(menu.containerId, -1, "", "");

		boolean allowed = LinkedStorageEndpointAccessProviders.hasGroupEndpoint(player, manager, groupId, reference);

		assertTrue(allowed);
	}

	@Test
	void hasGroupEndpointAllowsRegisteredEndpointInPlayerInventory() {
		Inventory inventory = mock(Inventory.class);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.getContainerSize()).thenReturn(41);
		when(inventory.getItem(8)).thenReturn(endpoint);
		reference = new ItemStackReference(ItemStackReference.PLAYER_INVENTORY, 8, "", "");

		boolean allowed = LinkedStorageEndpointAccessProviders.hasGroupEndpoint(player, manager, groupId, reference);

		assertTrue(allowed);
	}

	@Test
	void hasGroupEndpointRejectsDifferentRequestedGroup() {
		UUID otherGroup = UUID.randomUUID();

		boolean allowed = LinkedStorageEndpointAccessProviders.hasGroupEndpoint(player, manager, otherGroup, reference);

		assertFalse(allowed);
		verifyNoInteractions(manager);
	}

	@Test
	void hasGroupEndpointRejectsUnregisteredEndpoint() {
		when(manager.isEndpointMember(groupId, endpointId)).thenReturn(false);

		boolean allowed = LinkedStorageEndpointAccessProviders.hasGroupEndpoint(player, manager, groupId, reference);

		assertFalse(allowed);
	}

	@Test
	void hasGroupEndpointRejectsGhostEvenWithAuthenticEndpointData() {
		Slot ghost = mock(Slot.class, withSettings().extraInterfaces(IFilterSlot.class));
		when(ghost.getItem()).thenReturn(endpoint);
		when(ghost.isActive()).thenReturn(true);
		when(ghost.mayPickup(player)).thenReturn(true);
		when(menu.getSlot(0)).thenReturn(ghost);

		boolean allowed = LinkedStorageEndpointAccessProviders.hasGroupEndpoint(player, manager, groupId, reference);

		assertFalse(allowed);
		verifyNoInteractions(manager);
	}

	@Test
	void hasGroupEndpointRejectsStaleMenuReference() {
		reference = new ItemStackReference(menu.containerId + 1, 0, "", "");

		boolean allowed = LinkedStorageEndpointAccessProviders.hasGroupEndpoint(player, manager, groupId, reference);

		assertFalse(allowed);
		verifyNoInteractions(manager);
	}

	@Test
	void hasGroupEndpointRejectsInvalidMenu() {
		when(menu.stillValid(player)).thenReturn(false);

		boolean allowed = LinkedStorageEndpointAccessProviders.hasGroupEndpoint(player, manager, groupId, reference);

		assertFalse(allowed);
		verifyNoInteractions(manager);
	}

	@Test
	void hasGroupEndpointRejectsUnrelatedReferencedItemDespiteOtherEndpointInMenu() {
		when(menu.getTotalSlotsNumber()).thenReturn(2);
		when(menu.getSlot(1)).thenReturn(new Slot(new SimpleContainer(new ItemStack(Items.STONE)), 0, 0, 0));
		reference = new ItemStackReference(menu.containerId, 1, "", "");

		boolean allowed = LinkedStorageEndpointAccessProviders.hasGroupEndpoint(player, manager, groupId, reference);

		assertFalse(allowed);
		verifyNoInteractions(manager);
	}
}
