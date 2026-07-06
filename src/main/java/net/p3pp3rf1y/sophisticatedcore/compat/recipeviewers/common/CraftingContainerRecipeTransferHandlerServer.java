package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class CraftingContainerRecipeTransferHandlerServer {
	private CraftingContainerRecipeTransferHandlerServer() {
	}

	/**
	 * Called server-side to actually put the items in place.
	 */
	public static void setItemsWithSlotIDMap(Player player, ResourceKey<Recipe<?>> recipeId, RecipeType<?> recipeType, Map<Integer, Integer> slotIdMap,
			List<Integer> craftingSlots, List<Integer> inventorySlots, boolean maxTransfer) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> container)) {
			return;
		}

		// grab items from slots
		List<RequiredTransfer> requiredTransfers = new ArrayList<>(slotIdMap.size());
		for (Map.Entry<Integer, Integer> entry : slotIdMap.entrySet()) {
			if (!isValidSlot(container, entry.getKey()) || !isValidSlot(container, entry.getValue())) {
				return;
			}
			Slot slot = container.getSlot(entry.getValue());
			final ItemStack slotStack = slot.getItem();
			if (slotStack.isEmpty()) {
				return;
			}
			ItemStack stack = slotStack.copy();
			stack.setCount(1);
			requiredTransfers.add(new RequiredTransfer(entry.getKey(), entry.getValue(), stack));
		}

		setItems(container, player, recipeId, recipeType, requiredTransfers, craftingSlots, inventorySlots, maxTransfer);
	}

	/**
	 * Called server-side to actually put the items in place.
	 */
	public static void setItemsWithSlotTransfers(Player player, ResourceKey<Recipe<?>> recipeId, RecipeType<?> recipeType, List<SlotTransfer> slotTransfers,
			List<Integer> craftingSlots, List<Integer> inventorySlots, boolean maxTransfer) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> container)) {
			return;
		}

		List<RequiredTransfer> requiredTransfers = new ArrayList<>(slotTransfers.size());
		for (SlotTransfer slotTransfer : slotTransfers) {
			if (!isValidSlot(container, slotTransfer.craftingSlotId()) || !isValidSlot(container, slotTransfer.inventorySlotId())) {
				return;
			}

			Slot slot = container.getSlot(slotTransfer.inventorySlotId());
			final ItemStack slotStack = slot.getItem();
			if (slotStack.isEmpty()) {
				return;
			}
			ItemStack stack = slotStack.copy();
			stack.setCount(slotTransfer.count());
			requiredTransfers.add(new RequiredTransfer(slotTransfer.craftingSlotId(), slotTransfer.inventorySlotId(), stack));
		}

		setItems(container, player, recipeId, recipeType, requiredTransfers, craftingSlots, inventorySlots, maxTransfer);
	}

	/**
	 * Called server-side to actually put the items in place.
	 */
	public static void setItemsWithStacks(Player player, ResourceKey<Recipe<?>> recipeId, RecipeType<?> recipeType, List<ItemStack> stacks,
			List<Integer> craftingSlots, List<Integer> inventorySlots, boolean maxTransfer) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> container)) {
			return;
		}

		List<RequiredTransfer> requiredTransfers = new ArrayList<>(stacks.size());
		for (int i = 0; i < stacks.size(); i++) {
			ItemStack stack = stacks.get(i);
			if (stack.isEmpty()) {
				continue;
			}
			stack.setCount(1);
			requiredTransfers.add(new RequiredTransfer(craftingSlots.get(i), null, stack));
		}

		setItems(container, player, recipeId, recipeType, requiredTransfers, craftingSlots, inventorySlots, maxTransfer);
	}

	private static void setItems(StorageContainerMenuBase<?> container, Player player, ResourceKey<Recipe<?>> recipeId, RecipeType<?> recipeType,
			List<RequiredTransfer> requiredTransfers, List<Integer> craftingSlots, List<Integer> inventorySlots, boolean maxTransfer) {
		Map<Integer, ItemStack> toTransfer = removeItemsFromInventory(player, container, requiredTransfers, craftingSlots, inventorySlots, maxTransfer);

		if (toTransfer.isEmpty()) {
			return;
		}

		// clear the crafting grid
		List<ItemStack> clearedCraftingItems = clearAndPutItemsIntoGrid(player, recipeId, recipeType, craftingSlots, container, toTransfer);

		putIntoInventory(player, inventorySlots, container, clearedCraftingItems);

		container.broadcastChanges();
	}

	private static void putIntoInventory(Player player, List<Integer> inventorySlots, StorageContainerMenuBase<?> container,
			List<ItemStack> clearedCraftingItems) {
		for (ItemStack oldCraftingItem : clearedCraftingItems) {
			int added = addStack(container, inventorySlots, oldCraftingItem);
			if (added < oldCraftingItem.getCount()) {
				ItemStack remainingStack = added == 0 ? oldCraftingItem : oldCraftingItem.copyWithCount(oldCraftingItem.getCount() - added);
				if (!player.getInventory().add(remainingStack)) {
					player.drop(remainingStack, false);
				}
			}
		}
	}

	private static List<ItemStack> clearAndPutItemsIntoGrid(Player player, ResourceKey<Recipe<?>> recipeId, RecipeType<?> recipeType,
			List<Integer> craftingSlots, AbstractContainerMenu container, Map<Integer, ItemStack> toTransfer) {
		List<ItemStack> clearedCraftingItems = new ArrayList<>();
		int minSlotStackLimit = Integer.MAX_VALUE;
		for (int craftingSlotNumberIndex = 0; craftingSlotNumberIndex < craftingSlots.size(); craftingSlotNumberIndex++) {
			int craftingSlotNumber = craftingSlots.get(craftingSlotNumberIndex);
			Slot craftingSlot = container.getSlot(craftingSlotNumber);
			if (!craftingSlot.mayPickup(player)) {
				continue;
			}
			if (craftingSlot.hasItem()) {
				ItemStack craftingItem = craftingSlot.remove(craftingSlot.getItem().getCount());
				clearedCraftingItems.add(craftingItem);
			}
			ItemStack transferItem = toTransfer.get(craftingSlotNumber);
			if (transferItem != null) {
				int slotStackLimit = craftingSlot.getMaxStackSize(transferItem);
				minSlotStackLimit = Math.min(slotStackLimit, minSlotStackLimit);
			}
		}

		// put items into the crafting grid
		putItemIntoGrid(container, toTransfer, clearedCraftingItems, minSlotStackLimit);
		if (container instanceof StorageContainerMenuBase<?> storageContainerMenu) {
			storageContainerMenu.getOpenOrFirstCraftingContainer(recipeType).ifPresent(c -> c.setRecipeUsed(recipeId));
		}
		return clearedCraftingItems;
	}

	private static void putItemIntoGrid(AbstractContainerMenu container, Map<Integer, ItemStack> toTransfer, List<ItemStack> clearedCraftingItems,
			int minSlotStackLimit) {
		for (Map.Entry<Integer, ItemStack> entry : toTransfer.entrySet()) {
			Integer craftingSlotIndex = entry.getKey();
			Slot slot = container.getSlot(craftingSlotIndex);

			ItemStack stack = entry.getValue();
			if (slot.mayPlace(stack)) {
				if (stack.getCount() > minSlotStackLimit) {
					ItemStack remainder = stack.split(stack.getCount() - minSlotStackLimit);
					clearedCraftingItems.add(remainder);
				}
				slot.set(stack);
			} else {
				clearedCraftingItems.add(stack);
			}
		}
	}

	private static Map<Integer, ItemStack> removeItemsFromInventory(Player player, StorageContainerMenuBase<?> container, List<RequiredTransfer> required,
			List<Integer> craftingSlots, List<Integer> inventorySlots, boolean maxTransfer) {

		// This map becomes populated with the resulting items to transfer and is returned by this method.
		final Map<Integer, ItemStack> result = new HashMap<>(required.size());

		loopSets : while (true) { // for each set

			// This map holds the original contents of a slot we have removed items from. This is used if we don't
			// have enough items to complete a whole set, we can roll back the items that were removed.
			Map<Slot, ItemStack> originalSlotContents = new HashMap<>();

			// This map holds items found for each set iteration. Its contents are added to the result map
			// after each complete set iteration. If we are transferring as complete sets, this allows
			// us to simply ignore the map's contents when a complete set isn't found.
			final Map<Integer, ItemStack> foundItemsInSet = new HashMap<>(required.size());

			// This flag is set to false if at least one item is found during the set iteration. It is used
			// to determine if iteration should continue and prevents an infinite loop if not transferring
			// as complete sets.
			boolean noItemsFound = true;

			for (RequiredTransfer requiredTransfer : required) { // for each item in set
				final ItemStack requiredStack = requiredTransfer.stack.copy();

				// Locate a slot that has what we need.
				final Slot slot = getSlotWithStack(container, requiredStack, craftingSlots, inventorySlots, requiredTransfer.inventorySlot);

				boolean itemFound = (slot != null) && !slot.getItem().isEmpty() && slot.mayPickup(player);
				ItemStack resultItemStack = result.get(requiredTransfer.craftingSlot);
				boolean resultItemStackLimitReached = (resultItemStack != null)
						&& resultItemStack.getCount() + requiredStack.getCount() > resultItemStack.getMaxStackSize();

				if (!itemFound || resultItemStackLimitReached) {
					// We can't find any more items to fulfill the requirements or the maximum stack size for this item
					// has been reached.

					// Since the full set requirement wasn't satisfied, we need to roll back any
					// slot changes we've made during this set iteration.
					for (Map.Entry<Slot, ItemStack> slotEntry : originalSlotContents.entrySet()) {
						ItemStack stack = slotEntry.getValue();
						slotEntry.getKey().set(stack);
					}
					break loopSets;

				} else { // the item was found and the stack limit has not been reached

					// Keep a copy of the slot's original contents in case we need to roll back.
					if (!originalSlotContents.containsKey(slot)) {
						originalSlotContents.put(slot, slot.getItem().copy());
					}

					// Reduce the size of the found slot.
					ItemStack removedItemStack = slot.remove(requiredStack.getCount());
					mergeStack(foundItemsInSet, requiredTransfer.craftingSlot, removedItemStack);

					noItemsFound = false;
				}
			}

			// Merge the contents of the temporary map with the result map.
			foundItemsInSet.forEach((slot, stack) -> mergeStack(result, slot, stack));

			if (!maxTransfer || noItemsFound) {
				// If max transfer is not requested by the player this will exit the loop after trying one set.
				// If no items were found during this iteration, we're done.
				break;
			}
		}

		return result;
	}

	@Nullable
	private static Slot getSlotWithStack(StorageContainerMenuBase<?> container, ItemStack stack, List<Integer> craftingSlots, List<Integer> inventorySlots,
			@Nullable Integer hintSlot) {
		if (hintSlot != null && hintSlot >= 0 && hintSlot < getTotalSlotsSize(container)) {
			Slot slot = container.getSlot(hintSlot);
			if (hasRequiredStack(slot, stack)) {
				return slot;
			}
		}

		Slot slot = getSlotWithStack(container, craftingSlots, stack);
		if (slot == null) {
			slot = getSlotWithStack(container, inventorySlots, stack);
		}

		return slot;
	}

	private static void mergeStack(Map<Integer, ItemStack> stacks, Integer slot, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		ItemStack existing = stacks.get(slot);
		if (existing == null) {
			stacks.put(slot, stack);
		} else {
			existing.grow(stack.getCount());
		}
	}

	private static int addStack(StorageContainerMenuBase<?> container, Collection<Integer> slotIndexes, ItemStack stack) {
		int added = 0;
		// Add to existing stacks first
		for (final Integer slotIndex : slotIndexes) {
			if (slotIndex >= 0 && slotIndex < getTotalSlotsSize(container)) {
				final Slot slot = container.getSlot(slotIndex);
				final ItemStack inventoryStack = slot.getItem();
				// Check that the slot's contents are stackable with this stack
				if (!inventoryStack.isEmpty() && inventoryStack.isStackable() && ItemStack.isSameItemSameComponents(inventoryStack, stack)) {
					final int remain = stack.getCount() - added;
					final int maxStackSize = slot.getMaxStackSize(inventoryStack);
					final int space = maxStackSize - inventoryStack.getCount();
					if (space > 0) {

						// Enough space
						if (space >= remain) {
							inventoryStack.grow(remain);
							return stack.getCount();
						}

						// Not enough space
						inventoryStack.setCount(maxStackSize);

						added += space;
					}
				}
			}
		}

		if (added >= stack.getCount()) {
			return added;
		}

		for (final Integer slotIndex : slotIndexes) {
			if (slotIndex >= 0 && slotIndex < getTotalSlotsSize(container)) {
				final Slot slot = container.getSlot(slotIndex);
				final ItemStack inventoryStack = slot.getItem();
				if (inventoryStack.isEmpty()) {
					ItemStack stackToAdd = stack.copy();
					stackToAdd.setCount(stack.getCount() - added);
					slot.set(stackToAdd);
					return stack.getCount();
				}
			}
		}

		return added;
	}

	/**
	 * Get the slot which contains a specific itemStack.
	 *
	 * @param container
	 *            the container to search
	 * @param slotNumbers
	 *            the slots in the container to search
	 * @param itemStack
	 *            the itemStack to find
	 * @return the slot that contains the itemStack. returns null if no slot contains the itemStack.
	 */
	@Nullable
	private static Slot getSlotWithStack(StorageContainerMenuBase<?> container, Iterable<Integer> slotNumbers, ItemStack itemStack) {
		for (Integer slotNumber : slotNumbers) {
			if (slotNumber >= 0 && slotNumber < getTotalSlotsSize(container)) {
				Slot slot = container.getSlot(slotNumber);
				if (hasRequiredStack(slot, itemStack)) {
					return slot;
				}
			}
		}
		return null;
	}

	private static boolean hasRequiredStack(Slot slot, ItemStack itemStack) {
		ItemStack slotStack = slot.getItem();
		return ItemStack.isSameItemSameComponents(itemStack, slotStack) && slotStack.getCount() >= itemStack.getCount();
	}

	private static boolean isValidSlot(StorageContainerMenuBase<?> container, int slotId) {
		return slotId >= 0 && slotId < getTotalSlotsSize(container);
	}

	private static int getTotalSlotsSize(StorageContainerMenuBase<?> container) {
		return container.getTotalSlotsNumber();
	}

	private record RequiredTransfer(int craftingSlot, @Nullable Integer inventorySlot, ItemStack stack) {
	}
}
