package net.p3pp3rf1y.sophisticatedcore.settings.memory;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemorySettingsCategoryTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void unselectSlotAfterCopyingMemorizedItem() {
		InventoryHandler inventory = mock(InventoryHandler.class);
		when(inventory.getSlots()).thenReturn(1);
		when(inventory.getStackInSlot(0)).thenReturn(new ItemStack(Items.COBBLESTONE));
		MemorySettingsCategory source = new MemorySettingsCategory(() -> inventory, new CompoundTag(), tag -> {
		});
		MemorySettingsCategory target = new MemorySettingsCategory(() -> inventory, new CompoundTag(), tag -> {
		});

		source.selectSlot(0);
		source.copyTo(target, 0, 0);
		target.unselectSlot(0);

		assertFalse(target.isSlotSelected(0));
		assertTrue(target.getFilterItemSlots().isEmpty());
	}

	@Test
	void unselectSlotAfterCopyingMemorizedStack() {
		InventoryHandler inventory = mock(InventoryHandler.class);
		when(inventory.getSlots()).thenReturn(1);
		when(inventory.getStackInSlot(0)).thenReturn(new ItemStack(Items.COBBLESTONE));
		MemorySettingsCategory source = new MemorySettingsCategory(() -> inventory, new CompoundTag(), tag -> {
		});
		MemorySettingsCategory target = new MemorySettingsCategory(() -> inventory, new CompoundTag(), tag -> {
		});

		source.setIgnoreNbt(false);
		source.selectSlot(0);
		source.copyTo(target, 0, 0);
		target.unselectSlot(0);

		assertFalse(target.isSlotSelected(0));
		assertTrue(target.getFilterStackSlots().isEmpty());
	}

	@Test
	void selectSlotsReplacesMemorizedItemWithoutLeavingStaleReverseSlot() {
		InventoryHandler inventory = mock(InventoryHandler.class);
		when(inventory.getSlots()).thenReturn(1);
		when(inventory.getStackInSlot(0)).thenReturn(new ItemStack(Items.COBBLESTONE));
		MemorySettingsCategory memory = new MemorySettingsCategory(() -> inventory, new CompoundTag(), tag -> {
		});

		memory.selectSlot(0);
		when(inventory.getStackInSlot(0)).thenReturn(new ItemStack(Items.DIAMOND));
		memory.selectSlot(0);
		memory.unselectSlot(0);

		assertFalse(memory.isSlotSelected(0));
		assertTrue(memory.getFilterItemSlots().isEmpty());
	}

	@Test
	void selectSlotsReplacesMemorizedStackWithoutLeavingStaleReverseSlot() {
		InventoryHandler inventory = mock(InventoryHandler.class);
		when(inventory.getSlots()).thenReturn(1);
		when(inventory.getStackInSlot(0)).thenReturn(new ItemStack(Items.COBBLESTONE));
		MemorySettingsCategory memory = new MemorySettingsCategory(() -> inventory, new CompoundTag(), tag -> {
		});

		memory.setIgnoreNbt(false);
		memory.selectSlot(0);
		when(inventory.getStackInSlot(0)).thenReturn(new ItemStack(Items.DIAMOND));
		memory.selectSlot(0);
		memory.unselectSlot(0);

		assertFalse(memory.isSlotSelected(0));
		assertTrue(memory.getFilterStackSlots().isEmpty());
	}
}
