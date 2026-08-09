package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.common.transfer.TransferOperation;
import net.minecraft.world.inventory.Slot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

class JeiCraftingContainerRecipeTransferHandlerBaseTest {

	@Test
	void validatesUpgradeCraftingSlotsOutsideAbstractContainerSlotList() {
		Slot inventorySlot = slot(115);
		Slot craftingSlot = slot(188);
		Map<Integer, Slot> slots = Map.of(115, inventorySlot, 188, craftingSlot);

		boolean valid = JeiCraftingContainerRecipeTransferHandlerBase.validateSlots(195, slots::get, List.of(new TransferOperation(115, 188)),
				List.of(craftingSlot), List.of(inventorySlot));

		Assertions.assertTrue(valid);
	}

	private Slot slot(int index) {
		Slot slot = Mockito.mock(Slot.class);
		slot.index = index;
		Mockito.when(slot.isActive()).thenReturn(true);
		return slot;
	}
}
