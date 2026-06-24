package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;

import java.util.*;
import java.util.stream.Collectors;

public class InventorySorter {
	private InventorySorter() {
	}

	public static final Comparator<Map.Entry<ItemStackKey, Integer>> BY_NAME = Comparator.comparing(o -> {
		return o.getKey().stack().getHoverName().getString().toLowerCase();
	});
	public static final Comparator<Map.Entry<ItemStackKey, Integer>> BY_MOD = Comparator.<Map.Entry<ItemStackKey, Integer>, String>comparing(o -> {
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(o.getKey().stack().getItem());
		return registryName.getNamespace();
	}).thenComparing(o -> {
		return o.getKey().stack().getHoverName().getString();
	});

	public static final Comparator<Map.Entry<ItemStackKey, Integer>> BY_COUNT = (first, second) -> {
		int ret = second.getValue().compareTo(first.getValue());
		return ret != 0 ? ret : getRegistryName(first.getKey()).compareTo(getRegistryName(second.getKey()));
	};

	public static final Comparator<Map.Entry<ItemStackKey, Integer>> BY_TAGS = new Comparator<>() {
		@Override
		public int compare(Map.Entry<ItemStackKey, Integer> first, Map.Entry<ItemStackKey, Integer> second) {
			ItemStack firstStack = first.getKey().stack();
			Item firstItem = firstStack.getItem();
			ItemStack secondStack = second.getKey().stack();
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
		return BuiltInRegistries.ITEM.getKey(itemStackKey.stack().getItem()).toString();
	}

	public static void sortHandler(InventoryHandler handler, Comparator<? super Map.Entry<ItemStackKey, Integer>> comparator, Set<Integer> noSortSlots) {
		sortHandler(handler, comparator, noSortSlots, Set.of());
	}

	public static void sortHandler(InventoryHandler handler, Comparator<? super Map.Entry<ItemStackKey, Integer>> comparator, Set<Integer> noSortSlots,
			Set<Integer> ignoredSlots) {
		Set<Integer> skippedSlots = new HashSet<>(noSortSlots);
		skippedSlots.addAll(ignoredSlots);
		Set<Integer> accessibleNoSortSlots = new HashSet<>(noSortSlots);
		for (int slot = 0; slot < handler.size(); slot++) {
			if (!handler.isSlotAccessible(slot)) {
				skippedSlots.add(slot);
				accessibleNoSortSlots.remove(slot);
			}
		}
		Map<ItemStackKey, Integer> compactedStacks = InventoryHelper.getCompactedStacks(handler, skippedSlots, false);
		List<Map.Entry<ItemStackKey, Integer>> sortedList = new ArrayList<>(compactedStacks.entrySet());
		sortedList.sort(comparator);

		int slots = handler.size();

		sortIntoNoSortSlots(handler, accessibleNoSortSlots, sortedList);

		sortIntoOtherSlots(handler, skippedSlots, sortedList, slots);
	}

	private static void sortIntoOtherSlots(InventoryHandler handler, Set<Integer> noSortSlots, List<Map.Entry<ItemStackKey, Integer>> sortedList, int slots) {
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

	private static void sortIntoNoSortSlots(InventoryHandler handler, Set<Integer> noSortSlots, List<Map.Entry<ItemStackKey, Integer>> sortedList) {
		Iterator<Map.Entry<ItemStackKey, Integer>> it = sortedList.iterator();
		if (!noSortSlots.isEmpty()) {
			while (it.hasNext()) {
				Map.Entry<ItemStackKey, Integer> entry = it.next();
				ItemStackKey current = entry.getKey();
				Integer count = entry.getValue();

				for (int slot : noSortSlots) {
					ItemStack slotStack = handler.getStackInSlot(slot);
					if (ItemStack.isSameItemSameComponents(slotStack, current.stack())) {
						int placedCount = topUpNoSortSlot(handler, current, count, slot, slotStack);
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

	private static int topUpNoSortSlot(InventoryHandler handler, ItemStackKey current, int count, int slot, ItemStack slotStack) {
		if (handler.isInfinite(slot)) {
			return placeStack(handler, current, count, slot, true);
		}

		int existingCount = slotStack.getCount();
		long slotLimit = handler.getCapacityAsLong(slot, ItemResource.of(current.stack()));
		int countPlaced = (int) Math.min(slotLimit, existingCount + (long) count) - existingCount;
		if (countPlaced <= 0) {
			return 0;
		}

		ItemStack copy = current.stack().copy();
		copy.setCount(existingCount + countPlaced);
		if (!ItemStack.matches(slotStack, copy)) {
			handler.setStackInSlot(slot, copy);
		}
		return countPlaced;
	}

	private static void emptySlot(ResourceHandler<ItemResource> handler, int slot) {
		ItemResource resource = handler.getResource(slot);
		if (!resource.isEmpty()) {
			if (handler instanceof InventoryHandler inventoryHandler) {
				inventoryHandler.setStackInSlotInternal(slot, ItemStack.EMPTY);
			} else {
				InventoryHelper.extract(handler, resource, handler.getAmountAsInt(slot));
			}
		}
	}

	private static int placeStack(InventoryHandler handler, ItemStackKey current, int count, int slot, boolean countWithCurrentStack) {
		return placeStack(current, count, slot, countWithCurrentStack, (s, stack) -> handler.getBaseCapacity(ItemResource.of(stack)), handler::getInternalStack,
				handler::setStackInSlotInternal);
	}

	private static int placeStack(ItemStackKey current, int count, int slot, boolean countWithCurrentStack, IStackLimitGetter stackLimitGetter,
			ISlotStackGetter slotStackGetter, ISlotStackSetter slotStackSetter) {
		ItemStack copy = current.stack().copy();
		int slotLimit = stackLimitGetter.getStackLimit(slot, copy);
		int existingCount = slotStackGetter.getSlotStack(slot).getCount();
		if (countWithCurrentStack) {
			count += existingCount;
		}
		int countPlaced = Math.min(count, slotLimit);
		copy.setCount(countPlaced);
		if (!ItemStack.matches(slotStackGetter.getSlotStack(slot), copy)) {
			slotStackSetter.setSlotStack(slot, copy);
		}
		return countWithCurrentStack ? countPlaced - existingCount : countPlaced;
	}

	private interface IStackLimitGetter {
		int getStackLimit(int slot, ItemStack stack);
	}

	private interface ISlotStackGetter {
		ItemStack getSlotStack(int slot);
	}

	private interface ISlotStackSetter {
		void setSlotStack(int slot, ItemStack stack);
	}
}
