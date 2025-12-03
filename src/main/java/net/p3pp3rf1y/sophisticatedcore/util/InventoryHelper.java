package net.p3pp3rf1y.sophisticatedcore.util;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotStackAccessor;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IPickupResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

public class InventoryHelper {
	private InventoryHelper() {
	}

	private static final List<Function<Player, ResourceHandler<ItemResource>>> PLAYER_INVENTORY_PROVIDERS = new ArrayList<>();
	private static final List<Function<Player, ResourceHandler<ItemResource>>> PLAYER_EQUIPMENT_INVENTORY_PROVIDERS = new ArrayList<>();

	static {
		registerPlayerInventoryProvider(player -> player.getCapability(Capabilities.Item.ENTITY));
		registerEquipmentInventoryProvider(player -> new CombinedResourceHandler<>(
				PlayerInventoryWrapper.of(player).getArmorSlots(),
				PlayerInventoryWrapper.of(player).getHandSlots()
		));
	}

	public static void registerEquipmentInventoryProvider(Function<Player, ResourceHandler<ItemResource>> provider) {
		PLAYER_EQUIPMENT_INVENTORY_PROVIDERS.add(provider);
	}

	public static void registerPlayerInventoryProvider(Function<Player, ResourceHandler<ItemResource>> provider) {
		PLAYER_INVENTORY_PROVIDERS.add(provider);
	}

	public static Optional<ItemStack> getItemFromEitherHand(Player player, Item item) {
		ItemStack mainHandItem = player.getMainHandItem();
		if (mainHandItem.getItem() == item) {
			return Optional.of(mainHandItem);
		}
		ItemStack offhandItem = player.getOffhandItem();
		if (offhandItem.getItem() == item) {
			return Optional.of(offhandItem);
		}
		return Optional.empty();
	}

	public static boolean hasItem(ResourceHandler<ItemResource> inventory, Predicate<ItemResource> matches) {
		AtomicBoolean result = new AtomicBoolean(false);
		iterate(inventory, (slot, resource, amount) -> {
			if (!resource.isEmpty() && matches.test(resource)) {
				result.set(true);
			}
		}, result::get);
		return result.get();
	}

	public static Set<Integer> getItemSlots(InventoryHandler inventory, Predicate<ItemStack> matches) {
		Set<Integer> slots = new HashSet<>();
		iterate(inventory, (slot, stack) -> {
			if (!stack.isEmpty() && matches.test(stack)) {
				slots.add(slot);
			}
		});
		return slots;
	}

	public static void copyTo(ResourceHandler<ItemResource> from, ResourceHandler<ItemResource> to) {
		try (var tx = Transaction.openRoot()) {
			int slots = Math.min(from.size(), to.size());
			for (int i = 0; i < slots; i++) {
				ItemStack s = from.getResource(i).toStack(from.getAmountAsInt(i));
				if (s.isEmpty()) continue;
				to.insert(ItemResource.of(s), s.getCount(), tx);
			}
			tx.commit();
		}
	}

	public static void insertOrDropItem(Player player, ItemStack stack, ResourceHandler<ItemResource> inventory) {
		ItemStack ret = stack.copy();
		int moved = insert(inventory, ItemResource.of(ret), ret.getCount());
		if (moved > 0) {
			ret.shrink(moved);
			if (ret.isEmpty()) {
				return;
			}
		}
		if (!ret.isEmpty()) {
			player.drop(ret, true);
		}
	}

	public static int runPickupOnPickupResponseUpgrades(Level level, UpgradeHandler upgradeHandler, ItemResource resource, int amount, TransactionContext tx) {
		List<IPickupResponseUpgrade> pickupUpgrades = upgradeHandler.getWrappersThatImplement(IPickupResponseUpgrade.class);

		int totalPickedup = 0;
		for (IPickupResponseUpgrade pickupUpgrade : pickupUpgrades) {
			int pickedUpCount = pickupUpgrade.pickup(level, resource, amount, tx);
			totalPickedup += pickedUpCount;
			if (totalPickedup >= amount) {
				return totalPickedup;
			}
		}

		return totalPickedup;
	}

	public static void iteratePlayerInventory(Player player, BiConsumer<Integer, ItemStack> actOn) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			actOn.accept(slot, player.getInventory().getItem(slot));
		}
	}

	public static <T extends ResourceHandler<ItemResource> & ISlotStackAccessor> void iterate(T handler, BiConsumer<Integer, ItemStack> actOn) {
		iterate(handler, actOn, () -> false);
	}

	public static <T extends ResourceHandler<ItemResource> & ISlotStackAccessor> void iterate(T handler, BiConsumer<Integer, ItemStack> actOn, BooleanSupplier shouldExit) {
		iterate(handler, actOn, shouldExit, true);
	}

	public static <T extends ResourceHandler<ItemResource> & ISlotStackAccessor> void iterate(T handler, BiConsumer<Integer, ItemStack> actOn, BooleanSupplier shouldExit, boolean getVirtualCounts) {
		int slots = handler.size();
		for (int slot = 0; slot < slots; slot++) {
			ItemStack stack;
			stack = !getVirtualCounts && handler instanceof InventoryHandler inventoryHandler ? inventoryHandler.getInternalStack(slot) : handler.getStackInSlot(slot);
			actOn.accept(slot, stack);
			if (shouldExit.getAsBoolean()) {
				break;
			}
		}
	}

	public static void iterate(ResourceHandler<ItemResource> handler, TriConsumer<Integer, ItemResource, Integer> actOn) {
		iterate(handler, actOn, () -> false);
	}

	public static void iterate(ResourceHandler<ItemResource> handler, TriConsumer<Integer, ItemResource, Integer> actOn, BooleanSupplier shouldExit) {
		int slots = handler.size();
		for (int slot = 0; slot < slots; slot++) {
			ItemResource resource = handler.getResource(slot);
			int amount = handler.getAmountAsInt(slot);
			actOn.accept(slot, resource, amount);
			if (shouldExit.getAsBoolean()) {
				break;
			}
		}
	}

	public static int getCountMissingInHandler(ResourceHandler<ItemResource> itemHandler, ItemResource filter, int expectedCount) {
		MutableInt missingCount = new MutableInt(expectedCount);
		iterate(itemHandler, (slot, resource, amount) -> {
			if (resource.equals(filter)) {
				missingCount.subtract(Math.min(amount, missingCount.getValue()));
			}
		}, () -> missingCount.getValue() == 0);
		return missingCount.getValue();
	}

	public static <T, H extends ResourceHandler<ItemResource> & ISlotStackAccessor> T iterate(H handler, BiFunction<Integer, ItemStack, T> getFromStack, Supplier<T> supplyDefault, Predicate<T> shouldExit) {
		return iterate(handler, slot -> getFromStack.apply(slot, handler.getStackInSlot(slot)), supplyDefault, shouldExit);
	}

	public static <T> T iterate(ResourceHandler<ItemResource> handler, TriFunction<Integer, ItemResource, Integer, T> getFromResource, Supplier<T> supplyDefault, Predicate<T> shouldExit) {
		return iterate(handler, slot -> getFromResource.apply(slot, handler.getResource(slot), handler.getAmountAsInt(slot)), supplyDefault, shouldExit);
	}

	public static <T, H extends ResourceHandler<ItemResource>> T iterate(ResourceHandler<ItemResource> handler, IntFunction<T> getFromHandler, Supplier<T> supplyDefault, Predicate<T> shouldExit) {
		T ret = supplyDefault.get();
		int slots = handler.size();
		for (int slot = 0; slot < slots; slot++) {
			T apply = getFromHandler.apply(slot);
			ret = apply;
			if (shouldExit.test(ret)) {
				break;
			}
		}
		return ret;
	}

	public static ItemStack getAndRemove(ResourceHandler<ItemResource> itemHandler, int slot) {
		if (slot >= itemHandler.size()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = itemHandler.getResource(slot).toStack(itemHandler.getAmountAsInt(slot));
		if (stack.isEmpty()) return ItemStack.EMPTY;
		try (var tx = Transaction.openRoot()) {
			int moved = itemHandler.extract(slot, ItemResource.of(stack), stack.getCount(), tx);
			if (moved > 0) {
				tx.commit();
				return stack.copyWithCount(moved);
			}
		}
		return ItemStack.EMPTY;
	}

	public static List<Integer> getEmptySlotsRandomized(ResourceHandler<ItemResource> inventory) {
		List<Integer> list = Lists.newArrayList();

		for (int i = 0; i < inventory.size(); ++i) {
			if (inventory.getAmountAsInt(i) == 0) {
				list.add(i);
			}
		}

		Collections.shuffle(list, new Random());
		return list;
	}

	public static void shuffleItems(List<ItemStack> stacks, int emptySlotsCount, RandomSource rand) {
		List<ItemStack> list = Lists.newArrayList();
		Iterator<ItemStack> iterator = stacks.iterator();

		while (iterator.hasNext()) {
			ItemStack itemstack = iterator.next();
			if (itemstack.isEmpty()) {
				iterator.remove();
			} else if (itemstack.getCount() > 1) {
				list.add(itemstack);
				iterator.remove();
			}
		}

		while (emptySlotsCount - stacks.size() - list.size() > 0 && !list.isEmpty()) {
			ItemStack itemstack2 = list.remove(Mth.nextInt(rand, 0, list.size() - 1));
			int i = Mth.nextInt(rand, 1, itemstack2.getCount() / 2);
			ItemStack itemstack1 = itemstack2.split(i);
			if (itemstack2.getCount() > 1 && rand.nextBoolean()) {
				list.add(itemstack2);
			} else {
				stacks.add(itemstack2);
			}

			if (itemstack1.getCount() > 1 && rand.nextBoolean()) {
				list.add(itemstack1);
			} else {
				stacks.add(itemstack1);
			}
		}

		stacks.addAll(list);
		Collections.shuffle(stacks, new Random());
	}

	public static void dropResources(ResourceHandler<ItemResource> inventoryHandler, Level level, BlockPos pos) {
		dropResources(inventoryHandler, level, pos.getX(), pos.getY(), pos.getZ());
	}

	public static void dropResources(ResourceHandler<ItemResource> inventoryHandler, Level level, double x, double y, double z) {
		try (Transaction tx = Transaction.openRoot()) {
			iterate(inventoryHandler, (slot, resource, amount) -> dropItem(inventoryHandler, level, x, y, z, slot, resource.toStack(amount), tx));
			tx.commit();
		}
	}

	public static <T extends ResourceHandler<ItemResource> & ISlotStackAccessor> void dropItems(T handler, Level level, double x, double y, double z) {
		try (Transaction tx = Transaction.openRoot()) {
			iterate(handler, (slot, stack) -> dropItem(handler, level, x, y, z, slot, stack, tx), () -> false, false);
			tx.commit();
		}
	}

	public static void dropItem(ResourceHandler<ItemResource> handler, Level level, double x, double y, double z, Integer slot, ItemStack stack, Transaction tx) {
		if (stack.isEmpty()) {
			return;
		}
		int totalCountToDrop = stack.getCount();
		while (totalCountToDrop > 0) {
			int countToDrop = Math.min(stack.getMaxStackSize(), totalCountToDrop);
			Containers.dropItemStack(level, x, y, z, stack.copyWithCount(countToDrop));
			totalCountToDrop -= countToDrop;
		}
		handler.extract(handler.getResource(slot), handler.getAmountAsInt(slot), tx);
	}

	public static int getAnalogOutputSignal(InventoryHandler handler) {
		AtomicDouble totalFilled = new AtomicDouble(0);
		AtomicBoolean isEmpty = new AtomicBoolean(true);
		iterate(handler, (slot, stack) -> {
			if (!stack.isEmpty()) {
				int slotLimit = handler.getInternalSlotLimit(slot);
				totalFilled.addAndGet(stack.getCount() / (slotLimit / ((float) 64 / stack.getMaxStackSize())));
				isEmpty.set(false);
			}
		});
		double percentFilled = totalFilled.get() / handler.size();
		return Mth.floor(percentFilled * 14.0F) + (isEmpty.get() ? 0 : 1);
	}

	public static List<ResourceHandler<ItemResource>> getItemHandlersFromPlayerIncludingContainers(Player player) {
		List<ResourceHandler<ItemResource>> itemHandlers = new ArrayList<>();
		PLAYER_INVENTORY_PROVIDERS.forEach(provider -> {
			ResourceHandler<ItemResource> itemHandler = provider.apply(player);
			itemHandlers.add(itemHandler);
			for (int slot = 0; slot < itemHandler.size(); slot++) {
				ItemStack slotStack = itemHandler.getResource(slot).toStack(itemHandler.getAmountAsInt(slot));
				if (slotStack.isEmpty()) continue;
				ResourceHandler<ItemResource> containerHandler =
						ItemAccess.forStack(slotStack).getCapability(Capabilities.Item.ITEM);
				if (containerHandler != null) {
					itemHandlers.add(containerHandler);
				}
			}
		});
		return itemHandlers;
	}

	public static List<ResourceHandler<ItemResource>> getEquipmentItemHandlersFromPlayer(Player player) {
		List<ResourceHandler<ItemResource>> itemHandlers = new ArrayList<>();
		PLAYER_EQUIPMENT_INVENTORY_PROVIDERS.forEach(provider -> {
			itemHandlers.add(provider.apply(player));
		});
		return itemHandlers;
	}

	static Map<ItemStackKey, Integer> getCompactedStacks(InventoryHandler handler) {
		return getCompactedStacks(handler, new HashSet<>());
	}

	static Map<ItemStackKey, Integer> getCompactedStacks(InventoryHandler handler, Set<Integer> ignoreSlots) {
		return getCompactedStacks(handler, ignoreSlots, true);
	}

	static Map<ItemStackKey, Integer> getCompactedStacks(InventoryHandler handler, Set<Integer> ignoreSlots, boolean getVirtualCounts) {
		Map<ItemStackKey, Integer> ret = new HashMap<>();
		iterate(handler, (slot, stack) -> {
			if (stack.isEmpty() || ignoreSlots.contains(slot)) {
				return;
			}
			ItemStackKey itemStackKey = ItemStackKey.of(stack);
			ret.put(itemStackKey, ret.computeIfAbsent(itemStackKey, fs -> 0) + stack.getCount());
		}, () -> false, getVirtualCounts);
		return ret;
	}

	public static List<ItemStack> getCompactedStacksSortedByCount(InventoryHandler handler) {
		Map<ItemStackKey, Integer> compactedStacks = getCompactedStacks(handler);
		List<Map.Entry<ItemStackKey, Integer>> sortedList = new ArrayList<>(compactedStacks.entrySet());
		sortedList.sort(InventorySorter.BY_COUNT);

		List<ItemStack> ret = new ArrayList<>();
		sortedList.forEach(e -> {
			ItemStack stackCopy = e.getKey().stack().copy();
			stackCopy.setCount(e.getValue());
			ret.add(stackCopy);
		});
		return ret;
	}

	public static int mergeIntoPlayerInventory(Player player, ItemStack stack, int startSlot) {
		int moved = 0;
		List<Integer> emptySlots = new ArrayList<>();
		for (int slot = startSlot; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
			ItemStack slotStack = player.getInventory().getItem(slot);
			if (slotStack.isEmpty()) {
				emptySlots.add(slot);
			}
			if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
				int count = Math.min(slotStack.getMaxStackSize() - slotStack.getCount(), stack.getCount() - moved);
				slotStack.grow(count);
				moved += count;
				if (moved >= stack.getCount()) {
					return moved;
				}
			}
		}

		for (int slot : emptySlots) {
			ItemStack slotStack = stack.copyWithCount(Math.min(stack.getMaxStackSize(), stack.getCount() - moved));
			player.getInventory().setItem(slot, slotStack);
			moved += slotStack.getCount();
			if (moved >= stack.getCount()) {
				return moved;
			}
		}

		return moved;
	}

	public static Set<ItemStackKey> getUniqueStacks(InventoryHandler handler) {
		Set<ItemStackKey> uniqueStacks = new HashSet<>();
		iterate(handler, (slot, stack) -> {
			if (stack.isEmpty()) {
				return;
			}
			ItemStackKey itemStackKey = ItemStackKey.of(stack);
			uniqueStacks.add(itemStackKey);
		});
		return uniqueStacks;
	}

	public static Set<ItemStackKey> getUniqueStacks(ResourceHandler<ItemResource> handler) {
		Set<ItemStackKey> uniqueStacks = new HashSet<>();
		iterate(handler, (slot, resource, amount) -> {
			if (resource.isEmpty()) {
				return;
			}
			ItemStackKey itemStackKey = ItemStackKey.of(resource);
			uniqueStacks.add(itemStackKey);
		});
		return uniqueStacks;
	}

	public static List<ItemStack> insertIntoInventory(List<ItemStack> stacks, ResourceHandler<ItemResource> handler, TransactionContext tx) {
		if (stacks.isEmpty()) {
			return stacks;
		}

		List<ItemStack> remaining = new ArrayList<>();
		for (ItemStack stack : stacks) {
			int inserted = handler.insert(ItemResource.of(stack), stack.getCount(), tx);
			if (inserted < stack.getCount()) {
				remaining.add(stack.copyWithCount(stack.getCount() - inserted));
			}
		}
		return remaining;
	}

	public static void set(ResourceHandler<ItemResource> handler, int index, ItemResource resource, int amount) {
		try (Transaction tx = Transaction.openRoot()) {
			ItemResource currentResource = handler.getResource(index);
			if (!currentResource.isEmpty()) {
				handler.extract(index, currentResource, handler.getAmountAsInt(index), tx);
			}
			if (!resource.isEmpty()) {
				handler.insert(index, resource, amount, tx);
			}
			tx.commit();
		}
	}

	public static int insert(int index, ResourceHandler<ItemResource> inv, ItemResource res, int amount) {
		try (Transaction tx = Transaction.openRoot()) {
			int moved = inv.insert(index, res, amount, tx);
			if (moved > 0) tx.commit();
			return moved;
		}
	}

	public static List<ItemStack> getStacks(ResourceHandler<ItemResource> handler) {
		List<ItemStack> stacks = new ArrayList<>();
		iterate(handler, (slot, resource, amount) -> {
			stacks.add(resource.toStack(amount));
		});
		return stacks;
	}

	public static int insert(ResourceHandler<ItemResource> inv, ItemResource res, int amount) {
		try (var tx = Transaction.openRoot()) {
			int moved = inv.insert(res, amount, tx);
			if (moved > 0) tx.commit();
			return moved;
		}
	}

	//TODO cleanup below anything that's not used anywhere else

	public static int simulateInsert(ResourceHandler<ItemResource> inv, ItemResource res, int amount) {
		try (var tx = Transaction.openRoot()) {
			return inv.insert(res, amount, tx);
		}
	}

	public static int extractMatching(ResourceHandler<ItemResource> inv, Predicate<ItemStack> match, int maxAmount, Transaction tx) {
		int moved = 0;
		int slots = inv.size();
		for (int i = 0; i < slots && moved < maxAmount; i++) {
			ItemStack s = inv.getResource(i).toStack(inv.getAmountAsInt(i));
			if (s.isEmpty() || !match.test(s)) continue;
			int want = Math.min(maxAmount - moved, s.getCount());
			moved += inv.extract(i, ItemResource.of(s), want, tx);
		}
		return moved;
	}

	/**
	 * Convenience: opens a transaction and commits if anything moved.
	 */
	public static int extractMatching(ResourceHandler<ItemResource> inv, Predicate<ItemStack> match, int maxAmount) {
		try (var tx = Transaction.openRoot()) {
			int moved = extractMatching(inv, match, maxAmount, tx);
			if (moved > 0) tx.commit();
			return moved;
		}
	}

	/**
	 * Convenience: simulation (no commit).
	 */
	public static int simulateExtractMatching(ResourceHandler<ItemResource> inv, Predicate<ItemStack> match, int maxAmount) {
		try (var tx = Transaction.openRoot()) {
			return extractMatching(inv, match, maxAmount, tx);
		}
	}

	/**
	 * Extract an exact item match (same item + components), within the given transaction.
	 */
	public static int extract(ResourceHandler<ItemResource> inv, ItemStack wanted, int maxAmount, Transaction tx) {
		int limit = Math.min(maxAmount, wanted.getCount());
		return extractMatching(inv, s -> ItemStack.isSameItemSameComponents(s, wanted), limit, tx);
	}

	public static int simulateExtractExact(ResourceHandler<ItemResource> inv, ItemResource wanted, int maxAmount) {
		try (var tx = Transaction.openRoot()) {
			return inv.extract(wanted, maxAmount, tx);
		}
	}

	public static int extract(ResourceHandler<ItemResource> inv, int index, ItemResource wanted, int maxAmount) {
		try (var tx = Transaction.openRoot()) {
			int extracted = inv.extract(index, wanted, maxAmount, tx);
			tx.commit();
			return extracted;
		}
	}

	public static int extract(ResourceHandler<ItemResource> inv, ItemResource wanted, int maxAmount) {
		try (var tx = Transaction.openRoot()) {
			int extracted = inv.extract(wanted, maxAmount, tx);
			tx.commit();
			return extracted;
		}
	}

	public static int extract(ResourceHandler<ItemResource> inv, ItemStack stackToExtract) {
		try (var tx = Transaction.openRoot()) {
			int extracted = inv.extract(ItemResource.of(stackToExtract), stackToExtract.getCount(), tx);
			if (extracted > 0) {
				tx.commit();
			}
			return extracted;
		}
	}

	/**
	 * Move resources between handlers with an optional filter, within the given transaction.
	 */
	public static int move(ResourceHandler<ItemResource> from, ResourceHandler<ItemResource> to, Predicate<ItemResource> filter, int maxAmount, Transaction tx) {
		return ResourceHandlerUtil.move(from, to, filter, maxAmount, tx);
	}

	/**
	 * Convenience: opens a transaction and commits if anything moved.
	 */
	public static int move(ResourceHandler<ItemResource> from, ResourceHandler<ItemResource> to, Predicate<ItemResource> filter, int maxAmount) {
		try (var tx = Transaction.openRoot()) {
			int moved = move(from, to, filter, maxAmount, tx);
			if (moved > 0) tx.commit();
			return moved;
		}
	}
}