package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CraftingContainerRecipeTransferHandlerServerTest {
	static {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
		bindTestComponents(Items.STONE, Items.DIAMOND);
	}

	@Test
	void transferReturnsClearedCraftingItemsToExistingSuppliedHandlerStack() {
		SimpleContainer craftingInventory = new SimpleContainer(new ItemStack(Items.STONE, 4));
		SimpleContainer sourceInventory = new SimpleContainer(new ItemStack(Items.DIAMOND));
		TestItemHandler storageInventory = new TestItemHandler(1);
		storageInventory.set(0, ItemResource.of(Items.STONE), 8);

		List<Slot> slots = List.of(new Slot(craftingInventory, 0, 0, 0), new Slot(sourceInventory, 0, 0, 0),
				new SlotSuppliedHandler(() -> storageInventory, 0, 0, 0));
		StorageContainerMenuBase<?> container = mock(StorageContainerMenuBase.class);
		when(container.getTotalSlotsNumber()).thenReturn(slots.size());
		when(container.getSlot(anyInt())).thenAnswer(invocation -> slots.get(invocation.getArgument(0)));

		Player player = mock(Player.class);
		player.containerMenu = container;

		ResourceKey<Recipe<?>> recipeId = ResourceKey.create(Registries.RECIPE, Identifier.parse("test"));
		CraftingContainerRecipeTransferHandlerServer.setItemsWithSlotTransfers(player, recipeId, RecipeType.CRAFTING, List.of(new SlotTransfer(1, 0, 1)),
				List.of(0), List.of(1, 2), false);

		assertEquals(12, storageInventory.getAmountAsInt(0));
	}

	private static void bindTestComponents(Item... items) {
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		for (Item item : items) {
			item.builtInRegistryHolder().bindComponents(components);
		}
	}

	private static class TestItemHandler extends ItemStacksResourceHandler implements IndexModifier<ItemResource> {
		private TestItemHandler(int size) {
			super(size);
		}
	}
}
