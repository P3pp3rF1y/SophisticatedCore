package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
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

	default int getCapacity(int slot, ItemResource resource) {
		return 0;
	}

	default boolean shouldRenderInaccessibleSlotOverlay(int slot) {
		return !isSlotAccessible(slot);
	}

	default int extract(int slot, ItemResource resource, int amount, TransactionContext transaction, IResourceExtractor extractSuper) {
		return 0;
	}

	default int insert(int slot, ItemResource resource, int amount, TransactionContext transaction, IResourceInserter insertSuper) {
		return 0;
	}

	default void set(int slot, ItemResource resource, int amount, IndexModifier<ItemResource> setSuper) {
		//noop
	}

	default boolean isValid(int slot, ItemResource resource, @Nullable Player player, BiPredicate<Integer, ItemResource> isValidSuper) {
		return false;
	}

	default boolean canBeReplaced() {
		return false;
	}

	default int size() {
		return 0;
	}

	String getName();

	@Nullable
	default Identifier getNoItemIcon(int slot) {
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

	default boolean isInfinite(int slot) {
		return false;
	}

	default ItemResource getResource(int index, IntFunction<ItemResource> getResourceSuper) {
		return ItemResource.EMPTY;
	}

	default long getAmountAsLong(int index, IntFunction<Long> amountAsLongSuper) {
		return 0;
	}

	default ItemStack getStackInSlot(int slot, IntFunction<ItemStack> getStackInSlotSuper) {
		return ItemStack.EMPTY;
	}

	default void setStackInSlot(int slot, ItemStack stack, BiConsumer<Integer, ItemStack> setStackInSlotInternal) {
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
		public int getCapacity(int slot, ItemResource resource) {
			return parent.getBaseCapacity(resource);
		}

		@Override
		public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction, IResourceExtractor extractSuper) {
			return extractSuper.extract(slot, resource, amount, transaction);
		}

		@Override
		public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction, IResourceInserter insertSuper) {
			return insertSuper.insert(slot, resource, amount, transaction);
		}

		@Override
		public void set(int slot, ItemResource resource, int amount, IndexModifier<ItemResource> setSuper) {
			setSuper.set(slot, resource, amount);
		}

		@Override
		public boolean isValid(int slot, ItemResource resource, @Nullable Player player, BiPredicate<Integer, ItemResource> isValidSuper) {
			return true;
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
		public boolean shouldRenderInaccessibleSlotOverlay(int slot) {
			return false;
		}

		@Override
		public int size() {
			return slots;
		}

		@Override
		public String getName() {
			return NAME;
		}

		@Override
		public ItemResource getResource(int index, IntFunction<ItemResource> getResourceSuper) {
			return getResourceSuper.apply(index);
		}

		@Override
		public long getAmountAsLong(int index, IntFunction<Long> amountAsLongSuper) {
			return amountAsLongSuper.apply(index);
		}

		@Override
		public ItemStack getStackInSlot(int slot, IntFunction<ItemStack> getStackInSlotSuper) {
			return getStackInSlotSuper.apply(slot);
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack, BiConsumer<Integer, ItemStack> setStackInSlotInternal) {
			setStackInSlotInternal.accept(slot, stack);
		}
	}

	interface Factory {
		IInventoryPartHandler create(InventoryHandler parent, SlotRange slotRange, Supplier<MemorySettingsCategory> getMemorySettings);
	}
}
