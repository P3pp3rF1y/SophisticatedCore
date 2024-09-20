package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;

import java.util.*;
import java.util.stream.Collectors;

public class InventorySorter {
	private InventorySorter() {}

	public static final Comparator<Map.Entry<ItemStackKey, Integer>> BY_NAME = Comparator.comparing(o -> o.getKey().getStack().getHoverName().getString().toLowerCase());
	public static final Comparator<Map.Entry<ItemStackKey, Integer>> BY_MOD =
			Comparator
					.<Map.Entry<ItemStackKey, Integer>, String>comparing(o -> {
						ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(o.getKey().getStack().getItem());
						return registryName.getNamespace();
					})
					.thenComparing(o -> o.getKey().getStack().getHoverName().getString());


	public static final Comparator<Map.Entry<ItemStackKey, Integer>> BY_COUNT = (first, second) -> {
		int ret = second.getValue().compareTo(first.getValue());
		return ret != 0 ? ret : getRegistryName(first.getKey()).compareTo(getRegistryName(second.getKey()));
	};

	public static final Comparator<Map.Entry<ItemStackKey, Integer>> BY_TAGS = new Comparator<>() {
		@Override
		public int compare(Map.Entry<ItemStackKey, Integer> first, Map.Entry<ItemStackKey, Integer> second) {
			ItemStack firstStack = first.getKey().getStack();
			Item firstItem = firstStack.getItem();
			ItemStack secondStack = second.getKey().getStack();
			Item secondItem = secondStack.getItem();
			if (firstItem == secondItem) {
				return 0;
			}
			int ret = compareTags(firstStack.getTags().collect(Collectors.toSet()), secondStack.getTags().collect(Collectors.toSet()));
			return ret != 0 ? ret : getRegistryName(first.getKey()).compareTo(getRegistryName(second.getKey()));
		}

		private int compareTags(Set<TagKey<Item>> firstTags, Set<TagKey<Item>> secondTags) {
			int ret = Integer.compare(secondTags.size(), firstTags.size());
			if (ret != 0) {
				return ret;
			}

			if (firstTags.size() == 1) {
				return firstTags.iterator().next().location().compareTo(secondTags.iterator().next().location());
			}

			ArrayList<TagKey<Item>> firstTagsSorted = new ArrayList<>(firstTags);
			ArrayList<TagKey<Item>> secondTagsSorted = new ArrayList<>(secondTags);
			firstTagsSorted.sort(Comparator.comparing(TagKey::location));
			secondTagsSorted.sort(Comparator.comparing(TagKey::location));

			for (int i = 0; i < firstTagsSorted.size(); i++) {
				ret = firstTagsSorted.get(i).location().compareTo(secondTagsSorted.get(i).location());
				if (ret != 0) {
					return ret;
				}
			}
			return 0;
		}
	};

	private static String getRegistryName(ItemStackKey itemStackKey) {
		return BuiltInRegistries.ITEM.getKey(itemStackKey.getStack().getItem()).toString();
	}

	public static void sortHandler(IItemHandlerModifiable handler, Comparator<? super Map.Entry<ItemStackKey, Integer>> comparator, Set<Integer> noSortSlots) {
		Map<ItemStackKey, Integer> compactedStacks = InventoryHelper.getCompactedStacks(handler, noSortSlots);
		List<Map.Entry<ItemStackKey, Integer>> sortedList = new ArrayList<>(compactedStacks.entrySet());
		sortedList.sort(comparator);

		int slots = handler.getSlots();

		sortIntoNoSortSlots(handler, noSortSlots, sortedList);

		sortIntoOtherSlots(handler, noSortSlots, sortedList, slots);
	}

	private static void sortIntoOtherSlots(IItemHandlerModifiable handler, Set<Integer> noSortSlots, List<Map.Entry<ItemStackKey, Integer>> sortedList, int slots) {
		Iterator<Map.Entry<ItemStackKey, Integer>> ite = sortedList.iterator();
		ItemStackKey current = null;
		int count = 0;

		for (int slot = 0; slot < slots; slot++) {
			if (noSortSlots.contains(slot)) {
				continue;
			}
			if ((current == null || count <= 0) && ite.hasNext()) {
				Map.Entry<ItemStackKey, Integer> entry = ite.next();
				current = entry.getKey();
				count = entry.getValue();
			}
			if (current != null && count > 0) {
				count -= placeStack(handler, current, count, slot, false);
			} else {
				emptySlot(handler, slot);
			}
		}
	}

	private static void sortIntoNoSortSlots(IItemHandlerModifiable handler, Set<Integer> noSortSlots, List<Map.Entry<ItemStackKey, Integer>> sortedList) {
		Iterator<Map.Entry<ItemStackKey, Integer>> it = sortedList.iterator();
		if (!noSortSlots.isEmpty()) {
			while(it.hasNext()) {
				Map.Entry<ItemStackKey, Integer> entry = it.next();
				ItemStackKey current = entry.getKey();
				Integer count = entry.getValue();

				for(int slot : noSortSlots) {
					ItemStack slotStack = handler.getStackInSlot(slot);
					if (ItemStack.isSameItemSameComponents(slotStack, current.getStack())) {
						int placedCount = placeStack(handler, current, count, slot, true);
						count -= placedCount;
						entry.setValue(count);
						if (count <= 0) {
							it.remove();
							break;
						}
					}
				}
			}

		}
	}

	private static void emptySlot(IItemHandlerModifiable handler, int slot) {
		if (!handler.getStackInSlot(slot).isEmpty()) {
			handler.setStackInSlot(slot, ItemStack.EMPTY);
		}
	}

	private static int placeStack(IItemHandlerModifiable handler, ItemStackKey current, int count, int slot, boolean countWithCurrentStack) {
		ItemStack copy = current.getStack().copy();
		int slotLimit = handler instanceof InventoryHandler inventoryHandler ? inventoryHandler.getStackLimit(slot, copy) : handler.getSlotLimit(slot);
		int existingCount = handler.getStackInSlot(slot).getCount();
		if (countWithCurrentStack) {
			count += existingCount;
		}
		int countPlaced = Math.min(count, slotLimit);
		copy.setCount(countPlaced);
		if (!ItemStack.matches(handler.getStackInSlot(slot), copy)) {
			handler.setStackInSlot(slot, copy);
		}
		return countWithCurrentStack ? countPlaced - existingCount : countPlaced;
	}

}
