package net.p3pp3rf1y.sophisticatedcore.upgrades.compacting;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.IInventoryPartHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryPartitioner;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CompactingUpgradeWrapperTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private CompactingUpgradeWrapper getWrapper(IStorageWrapper storage) {
		CompactingUpgradeItem item = mock(CompactingUpgradeItem.class);
		when(item.getFilterSlotCount()).thenReturn(1);
		when(item.shouldCompactThreeByThree()).thenReturn(true);
		ItemStack upgrade = mock(ItemStack.class);
		when(upgrade.getItem()).thenReturn(item);
		CompoundTag tag = new CompoundTag();
		when(upgrade.getTag()).thenReturn(tag);
		when(upgrade.getOrCreateTag()).thenReturn(tag);
		return new CompactingUpgradeWrapper(storage, upgrade, s -> {});
	}

	@Test
	void chainedCompactingDoesNotReenterInventoryInsertion() throws Exception {
		try (MockedStatic<RecipeHelper> recipes = mockStatic(RecipeHelper.class)) {
			recipes.when(() -> RecipeHelper.getItemCompactingShapes(any(Item.class))).thenReturn(Set.of(RecipeHelper.CompactingShape.NONE));
			Constructor<RecipeHelper.CompactingResult> constructor = RecipeHelper.CompactingResult.class.getDeclaredConstructor(ItemStack.class, List.class);
			constructor.setAccessible(true);
			Item[] chain = {Items.IRON_NUGGET, Items.IRON_INGOT, Items.IRON_BLOCK};
			for (int i = 0; i < 2; i++) {
				Item ingredient = chain[i];
				RecipeHelper.CompactingResult result = constructor.newInstance(new ItemStack(chain[i + 1]), List.of());
				recipes.when(() -> RecipeHelper.getItemCompactingShapes(ingredient)).thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
				recipes.when(() -> RecipeHelper.getCompactingResult(ingredient, 3, 3)).thenReturn(result);
			}

			CompactingUpgradeWrapper wrapper = getWrapper(mock(IStorageWrapper.class));
			CompactingInventory inventory = new CompactingInventory();
			inventory.setStackInSlot(0, new ItemStack(Items.IRON_NUGGET, 162));
			inventory.afterInsert = slot -> wrapper.onAfterInsert(inventory, slot);

			wrapper.onAfterInsert(inventory, 0);

			int blocks = 0;
			for (int slot = 0; slot < inventory.getSlots(); slot++) {
				ItemStack stack = inventory.getStackInSlot(slot);
				assertTrue(stack.isEmpty() || stack.is(Items.IRON_BLOCK));
				blocks += stack.getCount();
			}
			assertEquals(2, blocks);
			assertEquals(1, inventory.maxInsertionDepth);
		}
	}

	@Test
	void compactingOrdinarySlotsDoesNotExtractFromCompressionSlots() throws Exception {
		try (MockedStatic<RecipeHelper> recipes = mockStatic(RecipeHelper.class)) {
			recipes.when(() -> RecipeHelper.getItemCompactingShapes(Items.IRON_NUGGET)).thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
			Constructor<RecipeHelper.CompactingResult> constructor = RecipeHelper.CompactingResult.class.getDeclaredConstructor(ItemStack.class, List.class);
			constructor.setAccessible(true);
			RecipeHelper.CompactingResult result = constructor.newInstance(new ItemStack(Items.IRON_INGOT), List.of());
			recipes.when(() -> RecipeHelper.getCompactingResult(Items.IRON_NUGGET, 3, 3)).thenReturn(result);
			InventoryHandler inventory = mock(InventoryHandler.class);
			InventoryPartitioner partitioner = mock(InventoryPartitioner.class);
			when(inventory.getInventoryPartitioner()).thenReturn(partitioner);
			when(inventory.getSlots()).thenReturn(2);
			when(partitioner.getPartBySlot(0)).thenReturn(mock(IInventoryPartHandler.class));
			when(partitioner.getPartBySlot(1)).thenReturn(() -> "default");
			when(inventory.getStackInSlot(0)).thenReturn(new ItemStack(Items.IRON_NUGGET, 81));
			ItemStack ordinarySlot = new ItemStack(Items.IRON_NUGGET, 9);
			when(inventory.getStackInSlot(1)).thenReturn(ordinarySlot);
			when(inventory.extractItem(eq(1), anyInt(), anyBoolean())).thenAnswer(i -> {
				int count = Math.min(ordinarySlot.getCount(), i.getArgument(1));
				if (!(boolean) i.getArgument(2)) {
					ordinarySlot.shrink(count);
				}
				return new ItemStack(Items.IRON_NUGGET, count);
			});
			when(inventory.insertItem(any(ItemStack.class), anyBoolean())).thenReturn(ItemStack.EMPTY);

			getWrapper(mock(IStorageWrapper.class)).onAfterInsert(inventory, 1);

			verify(inventory, never()).extractItem(eq(0), anyInt(), anyBoolean());
			verify(inventory).insertItem(argThat(stack -> stack.is(Items.IRON_INGOT) && stack.getCount() == 1), eq(false));
			assertTrue(ordinarySlot.isEmpty());
		}
	}

	@Test
	void skipsPartsThatManageTheirOwnCompacting() {
		InventoryHandler inventory = mock(InventoryHandler.class);
		InventoryPartitioner partitioner = mock(InventoryPartitioner.class);
		IInventoryPartHandler part = mock(IInventoryPartHandler.class);
		when(inventory.getInventoryPartitioner()).thenReturn(partitioner);
		when(partitioner.getPartBySlot(0)).thenReturn(part);

		getWrapper(mock(IStorageWrapper.class)).onAfterInsert(inventory, 0);

		verify(inventory, never()).getStackInSlot(anyInt());
	}

	private static class CompactingInventory extends ItemStackHandler implements IItemHandlerSimpleInserter {
		private IntConsumer afterInsert = slot -> {};
		private int insertionDepth = 0;
		private int maxInsertionDepth = 0;

		private CompactingInventory() {
			super(3);
		}

		@Override
		public ItemStack insertItem(ItemStack stack, boolean simulate) {
			for (int slot = 0; slot < getSlots() && !stack.isEmpty(); slot++) {
				stack = insertItem(slot, stack, simulate);
			}
			return stack;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			insertionDepth++;
			maxInsertionDepth = Math.max(maxInsertionDepth, insertionDepth);
			try {
				ItemStack remainder = super.insertItem(slot, stack, simulate);
				if (!simulate && remainder.getCount() < stack.getCount()) {
					afterInsert.accept(slot);
				}
				return remainder;
			} finally {
				insertionDepth--;
			}
		}
	}
}
