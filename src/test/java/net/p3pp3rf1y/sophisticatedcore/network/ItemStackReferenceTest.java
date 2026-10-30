package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IFilterSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ItemStackReferenceTest {
	private Player player;
	private StorageContainerMenuBase<?> menu;
	private ItemStack stack;

	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@BeforeEach
	void prepareMenu() {
		try (MockedStatic<ObfuscationReflectionHelper> ignored = mockStatic(ObfuscationReflectionHelper.class)) {
			player = mock(Player.class);
			menu = mock(StorageContainerMenuBase.class);
			player.containerMenu = menu;
			stack = new ItemStack(Items.CHEST);
			Slot realSlot = new Slot(new SimpleContainer(stack), 0, 0, 0);
			when(menu.getTotalSlotsNumber()).thenReturn(2);
			when(menu.getSlot(0)).thenReturn(realSlot);
			when(menu.stillValid(player)).thenReturn(true);
		}
	}

	@Test
	void resolveReturnsItemFromRealSlotInCurrentValidMenu() {
		ItemStackReference reference = new ItemStackReference(menu.containerId, 0, "", "");

		ItemStack resolved = reference.resolve(player);

		assertSame(stack, resolved);
	}

	@Test
	void resolveRejectsGhostSlot() {
		Slot ghostSlot = mock(Slot.class, withSettings().extraInterfaces(IFilterSlot.class));
		when(menu.getSlot(1)).thenReturn(ghostSlot);
		ItemStackReference reference = new ItemStackReference(menu.containerId, 1, "", "");

		ItemStack resolved = reference.resolve(player);

		assertTrue(resolved.isEmpty());
	}

	@Test
	void resolveRejectsDifferentMenuId() {
		ItemStackReference reference = new ItemStackReference(menu.containerId + 1, 0, "", "");

		ItemStack resolved = reference.resolve(player);

		assertTrue(resolved.isEmpty());
	}

	@Test
	void resolveRejectsOutOfBoundsSlot() {
		ItemStackReference reference = new ItemStackReference(menu.containerId, Integer.MAX_VALUE, "", "");

		ItemStack resolved = reference.resolve(player);

		assertTrue(resolved.isEmpty());
	}

	@Test
	void resolveRejectsInvalidMenu() {
		when(menu.stillValid(player)).thenReturn(false);
		ItemStackReference reference = new ItemStackReference(menu.containerId, 0, "", "");

		ItemStack resolved = reference.resolve(player);

		assertTrue(resolved.isEmpty());
	}
}
