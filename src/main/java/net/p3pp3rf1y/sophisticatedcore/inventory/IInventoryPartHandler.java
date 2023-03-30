package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import org.apache.commons.lang3.function.TriFunction;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public interface IInventoryPartHandler {
	IInventoryPartHandler EMPTY = () -> "EMPTY";

	default int getSlotLimit(int slot) {
		return 0;
	}

	default boolean isSlotAccessible(int slot) {
		return false;
	}

	default int getStackLimit(int slot, ItemStack stack) {
		return 0;
	}

	default ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ItemStack.EMPTY;
	}

	default ItemStack insertItem(int slot, ItemStack stack, boolean simulate, TriFunction<Integer, ItemStack, Boolean, ItemStack> insertSuper) {
		return stack;
	}

	default void setStackInSlot(int slot, ItemStack stack, BiConsumer<Integer, ItemStack> setStackInSlotSuper) {
		//noop
	}

	default boolean isItemValid(int slot, ItemStack stack) {
		return false;
	}

	default ItemStack getStackInSlot(int slot, IntFunction<ItemStack> getStackInSlotSuper) {
		return ItemStack.EMPTY;
	}

	default boolean canBeReplaced() {
		return false;
	}

	default int getSlots() { return 0;}

	String getName();

	@Nullable
	default Pair<ResourceLocation, ResourceLocation> getNoItemIcon(int slot) {
		return null;
	}

	default Item getFilterItem(int slot) {
		return Items.AIR;
	}

	default void onSlotLimitChange() {
		//noop
	}

	default Set<Integer> getNoSortSlots() {
		return Set.of();
	}

	default void onSlotFilterChanged(int slot) {
		//noop
	}

	default boolean isFilterItem(Item item) {
		return false;
	}

	default Map<Item, Set<Integer>> getFilterItems() {
		return Map.of();
	}

	default void onInit() {
		//noop
	}

	class Default implements IInventoryPartHandler {
		public static final String NAME = "default";
		private final InventoryHandler parent;
		private final int slots;

		public Default(InventoryHandler parent, int slots) {
			this.parent = parent;
			this.slots = slots;
		}

		@Override
		public int getSlotLimit(int slot) {
			return parent.getBaseSlotLimit();
		}

		@Override
		public int getStackLimit(int slot, ItemStack stack) {
			return parent.getBaseStackLimit(stack);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return parent.extractItemInternal(slot, amount, simulate);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate, TriFunction<Integer, ItemStack, Boolean, ItemStack> insertSuper) {
			return insertSuper.apply(slot, stack, simulate);
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack, BiConsumer<Integer, ItemStack> setStackInSlotSuper) {
			setStackInSlotSuper.accept(slot, stack);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return true;
		}

		@Override
		public ItemStack getStackInSlot(int slot, IntFunction<ItemStack> getStackInSlotSuper) {
			return getStackInSlotSuper.apply(slot);
		}

		@Override
		public boolean canBeReplaced() {
			return true;
		}

		@Override
		public boolean isSlotAccessible(int slot) {
			return true;
		}

		@Override
		public int getSlots() {
			return slots;
		}

		@Override
		public String getName() {
			return NAME;
		}
	}

	interface Factory {
		IInventoryPartHandler create(InventoryHandler parent, InventoryPartitioner.SlotRange slotRange, Supplier<MemorySettingsCategory> getMemorySettings);
	}
}
