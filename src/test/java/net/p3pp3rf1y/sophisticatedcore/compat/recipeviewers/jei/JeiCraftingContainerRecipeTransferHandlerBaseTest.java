package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.common.transfer.TransferOperation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class JeiCraftingContainerRecipeTransferHandlerBaseTest {

	@Test
	void validatesUpgradeCraftingSlotsOutsideAbstractContainerSlotList() {
		StorageContainerMenuBase<?> container = Mockito.mock(StorageContainerMenuBase.class);
		Slot inventorySlot = slot(115);
		Slot craftingSlot = slot(188);
		Mockito.when(container.getTotalSlotsNumber()).thenReturn(195);
		Mockito.when(container.getSlot(115)).thenReturn(inventorySlot);
		Mockito.when(container.getSlot(188)).thenReturn(craftingSlot);

		boolean valid = JeiCraftingContainerRecipeTransferHandlerBase.validateSlots(container, List.of(new TransferOperation(115, 188, 2)),
				List.of(craftingSlot), List.of(inventorySlot));

		Assertions.assertTrue(valid);
	}

	private Slot slot(int index) {
		Slot slot = new Slot(new SimpleContainer(1), 0, 0, 0);
		slot.index = index;
		return slot;
	}
}
