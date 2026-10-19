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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	void transferReturnsClearedCraftingItemsToExistingSuppliedHandlerStack() {
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
		StorageContainerMenuBase<?> container = mock(StorageContainerMenuBase.class);
		when(container.getTotalSlotsNumber()).thenReturn(slots.size());
		when(container.getSlot(anyInt())).thenAnswer(invocation -> slots.get(invocation.getArgument(0)));
		when(container.getOpenOrFirstCraftingContainer(RecipeType.CRAFTING)).thenReturn(Optional.empty());

		Player player = mock(Player.class);
		player.containerMenu = container;

		CraftingContainerRecipeTransferHandlerServer.setItemsWithSlotTransfers(player, ResourceLocation.parse("test"), RecipeType.CRAFTING,
				List.of(new CraftingContainerRecipeTransferHandlerServer.SlotTransfer(1, 0, 1)), List.of(0), List.of(1, 2), false);

		assertEquals(12, storageInventory.getStackInSlot(0).getCount());
	}
}
