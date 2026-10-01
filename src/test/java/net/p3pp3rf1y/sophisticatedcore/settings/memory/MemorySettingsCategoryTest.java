package net.p3pp3rf1y.sophisticatedcore.settings.memory;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemorySettingsCategoryTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		Items.COBBLESTONE.builtInRegistryHolder().bindComponents(components);
		Items.DIAMOND.builtInRegistryHolder().bindComponents(components);
	}

	@Test
	void selectSlotsReplacesMemorizedItemWithoutLeavingStaleReverseSlot() {
		InventoryHandler inventory = mock(InventoryHandler.class);
		when(inventory.size()).thenReturn(1);
		when(inventory.getStackInSlot(0)).thenReturn(new ItemStack(Items.COBBLESTONE));
		MemorySettingsCategory memory = new MemorySettingsCategory(() -> inventory, new MemorySettingsCategoryData(), () -> {
		});

		memory.selectSlots(0, 1);
		when(inventory.getStackInSlot(0)).thenReturn(new ItemStack(Items.DIAMOND));
		memory.selectSlots(0, 1);
		memory.unselectSlot(0);

		assertFalse(memory.isSlotSelected(0));
		assertTrue(memory.getFilterItemSlots().isEmpty());
	}

	@Test
	void selectSlotsReplacesMemorizedStackWithoutLeavingStaleReverseSlot() {
		InventoryHandler inventory = mock(InventoryHandler.class);
		when(inventory.size()).thenReturn(1);
		when(inventory.getStackInSlot(0)).thenReturn(new ItemStack(Items.COBBLESTONE));
		MemorySettingsCategoryData data = new MemorySettingsCategoryData();
		data.setIgnoreNbt(false);
		MemorySettingsCategory memory = new MemorySettingsCategory(() -> inventory, data, () -> {
		});

		memory.selectSlots(0, 1);
		when(inventory.getStackInSlot(0)).thenReturn(new ItemStack(Items.DIAMOND));
		memory.selectSlots(0, 1);
		memory.unselectSlot(0);

		assertFalse(memory.isSlotSelected(0));
		assertTrue(memory.getFilterStackSlots().isEmpty());
	}

	@Test
	void unselectSlotAfterSharedSettingsDataReload() {
		MemorySettingsCategoryData data = new MemorySettingsCategoryData(Map.of(0, Items.COBBLESTONE), Map.of(), true);
		MemorySettingsCategory memory = new MemorySettingsCategory(() -> mock(InventoryHandler.class), data, () -> {
		});

		data.reloadFrom(new MemorySettingsCategoryData(Map.of(0, Items.DIAMOND), Map.of(), true));
		memory.unselectSlot(0);

		assertFalse(memory.isSlotSelected(0));
		assertTrue(memory.getFilterItemSlots().isEmpty());
	}

	@Test
	void unselectStackSlotAfterSharedSettingsDataReload() {
		MemorySettingsCategoryData data = new MemorySettingsCategoryData(Map.of(), Map.of(0, ItemStackKey.of(new ItemStack(Items.COBBLESTONE))), false);
		MemorySettingsCategory memory = new MemorySettingsCategory(() -> mock(InventoryHandler.class), data, () -> {
		});

		data.reloadFrom(new MemorySettingsCategoryData(Map.of(), Map.of(0, ItemStackKey.of(new ItemStack(Items.DIAMOND))), false));
		memory.unselectSlot(0);

		assertFalse(memory.isSlotSelected(0));
		assertTrue(memory.getFilterStackSlots().isEmpty());
	}
}
