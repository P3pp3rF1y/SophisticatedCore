package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CraftingContainerRecipeTransferHandlerServerTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void setItemsWithSlotTransfersReturnsClearedCraftingItemsToExistingSuppliedHandlerStack() {
		SimpleContainer craftingInventory = new SimpleContainer(new ItemStack(Items.STONE, 4));
		SimpleContainer sourceInventory = new SimpleContainer(new ItemStack(Items.DIAMOND));
		ItemStackHandler storageInventory = new ItemStackHandler(1) {
			@Override
			public ItemStack getStackInSlot(int slot) {
				return super.getStackInSlot(slot).copy();
			}
		};
		storageInventory.setStackInSlot(0, new ItemStack(Items.STONE, 8));

		List<Slot> slots = List.of(new Slot(craftingInventory, 0, 0, 0), new Slot(sourceInventory, 0, 0, 0),
				new SlotSuppliedHandler(() -> storageInventory, 0, 0, 0));
		for (int index = 0; index < slots.size(); index++) {
			slots.get(index).index = index;
		}
		StorageContainerMenuBase<?> container = mock(StorageContainerMenuBase.class);
		when(container.getTotalSlotsNumber()).thenReturn(slots.size());
		when(container.getSlot(anyInt())).thenAnswer(invocation -> slots.get(invocation.getArgument(0)));
		CraftingUpgradeContainer craftingContainer = mock(CraftingUpgradeContainer.class);
		when(craftingContainer.getRecipeSlots()).thenReturn(List.of(slots.getFirst()));
		when(container.getOpenOrFirstCraftingContainer(RecipeType.CRAFTING)).thenReturn(Optional.of(craftingContainer));
		when(container.isStorageInventorySlot(1)).thenReturn(true);
		when(container.isStorageInventorySlot(2)).thenReturn(true);

		Player player = mock(Player.class);
		player.containerMenu = container;

		CraftingContainerRecipeTransferHandlerServer.setItemsWithSlotTransfers(player, ResourceLocation.parse("test"), RecipeType.CRAFTING,
				List.of(new CraftingContainerRecipeTransferHandlerServer.SlotTransfer(1, 0, 1)), List.of(0), List.of(1, 2), false);

		assertEquals(12, storageInventory.getStackInSlot(0).getCount());
	}

	@Test
	void setItemsWithStacksRejectsResultSlotWithoutRemovingItsContents() {
		SimpleContainer inventory = new SimpleContainer(ItemStack.EMPTY, new ItemStack(Items.DIAMOND));
		Slot gridSlot = new Slot(inventory, 0, 0, 0);
		Slot resultSlot = new Slot(inventory, 1, 0, 0);
		resultSlot.index = 1;
		StorageContainerMenuBase<?> container = mock(StorageContainerMenuBase.class);
		when(container.getTotalSlotsNumber()).thenReturn(2);
		when(container.getSlot(0)).thenReturn(gridSlot);
		when(container.getSlot(1)).thenReturn(resultSlot);
		CraftingUpgradeContainer craftingContainer = mock(CraftingUpgradeContainer.class);
		when(craftingContainer.getRecipeSlots()).thenReturn(List.of(gridSlot));
		when(container.getOpenOrFirstCraftingContainer(RecipeType.CRAFTING)).thenReturn(Optional.of(craftingContainer));
		Player player = mock(Player.class);
		player.containerMenu = container;

		boolean transferred = CraftingContainerRecipeTransferHandlerServer.setItemsWithStacks(player, ResourceLocation.parse("test"), RecipeType.CRAFTING,
				List.of(new ItemStack(Items.STONE)), List.of(1), List.of(), false);

		assertFalse(transferred);
		assertEquals(1, resultSlot.getItem().getCount());
	}

	@Test
	void validateSlotsRejectsOutputSource() {
		SimpleContainer inventory = new SimpleContainer(3);
		List<Slot> slots = List.of(new Slot(inventory, 0, 0, 0), new Slot(inventory, 1, 0, 0), new Slot(inventory, 2, 0, 0));
		for (int index = 0; index < slots.size(); index++) {
			slots.get(index).index = index;
		}
		StorageContainerMenuBase<?> container = mock(StorageContainerMenuBase.class);
		when(container.getTotalSlotsNumber()).thenReturn(3);
		when(container.getSlot(anyInt())).thenAnswer(invocation -> slots.get(invocation.getArgument(0)));
		when(container.isStorageInventorySlot(2)).thenReturn(true);
		CraftingUpgradeContainer craftingContainer = mock(CraftingUpgradeContainer.class);
		when(craftingContainer.getRecipeSlots()).thenReturn(List.of(slots.getFirst()));
		when(container.getOpenOrFirstCraftingContainer(RecipeType.CRAFTING)).thenReturn(Optional.of(craftingContainer));
		Player player = mock(Player.class);
		player.containerMenu = container;

		boolean valid = CraftingContainerRecipeTransferHandlerServer.validateSlots(player, RecipeType.CRAFTING, List.of(0), List.of(1));

		assertFalse(valid);
	}

	@Test
	void validateSlotsRejectsDuplicateInventoryIndexes() {
		SimpleContainer inventory = new SimpleContainer(2);
		Slot gridSlot = new Slot(inventory, 0, 0, 0);
		Slot storageSlot = new Slot(inventory, 1, 0, 0);
		storageSlot.index = 1;
		StorageContainerMenuBase<?> container = mock(StorageContainerMenuBase.class);
		when(container.getTotalSlotsNumber()).thenReturn(2);
		when(container.getSlot(0)).thenReturn(gridSlot);
		when(container.getSlot(1)).thenReturn(storageSlot);
		when(container.isStorageInventorySlot(1)).thenReturn(true);
		CraftingUpgradeContainer craftingContainer = mock(CraftingUpgradeContainer.class);
		when(craftingContainer.getRecipeSlots()).thenReturn(List.of(gridSlot));
		when(container.getOpenOrFirstCraftingContainer(RecipeType.CRAFTING)).thenReturn(Optional.of(craftingContainer));
		Player player = mock(Player.class);
		player.containerMenu = container;

		boolean valid = CraftingContainerRecipeTransferHandlerServer.validateSlots(player, RecipeType.CRAFTING, List.of(0), List.of(1, 1));

		assertFalse(valid);
	}
}
