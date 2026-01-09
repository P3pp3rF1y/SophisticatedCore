package net.p3pp3rf1y.sophisticatedcore.util;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.*;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IPickupResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

public class InventoryHelper {
	private InventoryHelper() {}

	private static final List<Function<Player, IItemHandler>> PLAYER_INVENTORY_PROVIDERS = new ArrayList<>();
	private static final List<Function<Player, IItemHandler>> PLAYER_EQUIPMENT_INVENTORY_PROVIDERS = new ArrayList<>();

	static {
		registerPlayerInventoryProvider(player -> player.getCapability(Capabilities.ItemHandler.ENTITY));
		registerEquipmentInventoryProvider(player -> new CombinedInvWrapper(
				new PlayerArmorInvWrapper(player.getInventory()),
				new PlayerOffhandInvWrapper(player.getInventory()),
				new RangedWrapper(new InvWrapper(player.getInventory()), player.getInventory().selected, player.getInventory().selected + 1))
				);
	}

	public static void registerEquipmentInventoryProvider(Function<Player, IItemHandler> provider) {
		PLAYER_EQUIPMENT_INVENTORY_PROVIDERS.add(provider);
	}

	public static void registerPlayerInventoryProvider(Function<Player, IItemHandler> provider) {
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

	public static boolean hasItem(IItemHandler inventory, Predicate<ItemStack> matches) {
		AtomicBoolean result = new AtomicBoolean(false);
		iterate(inventory, (slot, stack) -> {
			if (!stack.isEmpty() && matches.test(stack)) {
				result.set(true);
			}
		}, result::get);
		return result.get();
	}

	public static Set<Integer> getItemSlots(IItemHandler inventory, Predicate<ItemStack> matches) {
		Set<Integer> slots = new HashSet<>();
		iterate(inventory, (slot, stack) -> {
			if (!stack.isEmpty() && matches.test(stack)) {
				slots.add(slot);
			}
		});
		return slots;
	}

	public static void copyTo(IItemHandlerModifiable handlerA, IItemHandlerModifiable handlerB) {
		int slotsA = handlerA.getSlots();
		int slotsB = handlerB.getSlots();
		for (int slot = 0; slot < slotsA && slot < slotsB; slot++) {
			ItemStack slotStack = handlerA.getStackInSlot(slot);
			if (!slotStack.isEmpty()) {
				handlerB.setStackInSlot(slot, slotStack);
			}
		}
	}

	public static List<ItemStack> insertIntoInventory(List<ItemStack> stacks, IItemHandler inventory, boolean simulate) {
		if (stacks.isEmpty()) {
			return stacks;
		}
		IItemHandler targetInventory = inventory;
		if (simulate) {
			targetInventory = cloneInventory(inventory);
		}

		List<ItemStack> remaining = new ArrayList<>();
		for (ItemStack stack : stacks) {
			ItemStack result = insertIntoInventory(stack, targetInventory, false);
			if (!result.isEmpty()) {
				remaining.add(result);
			}
		}
		return remaining;
	}

	public static IItemHandler cloneInventory(IItemHandler inventory) {
		IItemHandler cloned = new ItemStackHandler(inventory.getSlots());
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			cloned.insertItem(slot, inventory.getStackInSlot(slot).copy(), false);
		}
		return cloned;
	}

	public static ItemStack insertIntoInventory(ItemStack stack, IItemHandler inventory, boolean simulate) {
		if (inventory instanceof IItemHandlerSimpleInserter itemHandlerSimpleInserter) {
			return itemHandlerSimpleInserter.insertItem(stack, simulate);
		}

		ItemStack remainingStack = stack.copy();
		int slots = inventory.getSlots();
		for (int slot = 0; slot < slots && !remainingStack.isEmpty(); slot++) {
			remainingStack = inventory.insertItem(slot, remainingStack, simulate);
		}
		return remainingStack;
	}

	public static ItemStack insertIntoInventoryMatchingFirst(ItemStack stack, IItemHandler inventory, boolean simulate) {
		ItemStack remainingStack = stack.copy();
		int slots = inventory.getSlots();
		for (int slot = 0; slot < slots && !remainingStack.isEmpty(); slot++) {
			ItemStack slotStack = inventory.getStackInSlot(slot);
			if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, remainingStack)) {
				remainingStack = inventory.insertItem(slot, remainingStack, simulate);
			}
		}
		for (int slot = 0; slot < slots && !remainingStack.isEmpty(); slot++) {
			ItemStack slotStack = inventory.getStackInSlot(slot);
			if (slotStack.isEmpty()) {
				remainingStack = inventory.insertItem(slot, remainingStack, simulate);
			}
		}
		return remainingStack;
	}

	public static ItemStack extractFromInventory(Item item, int count, IItemHandler inventory, boolean simulate) {
		return extractFromInventory(stack -> stack.getItem() == item, count, inventory, simulate);
	}

	public static ItemStack extractFromInventory(Predicate<ItemStack> stackMatcher, int count, IItemHandler inventory, boolean simulate) {
		ItemStack ret = ItemStack.EMPTY;
		int slots = inventory.getSlots();
		for (int slot = 0; slot < slots && ret.getCount() < count; slot++) {
			ItemStack slotStack = inventory.getStackInSlot(slot);
			if (stackMatcher.test(slotStack) && (ret.isEmpty() || ItemStack.isSameItemSameComponents(ret, slotStack))) {
				int toExtract = Math.min(slotStack.getCount(), count - ret.getCount());
				ItemStack extractedStack = inventory.extractItem(slot, toExtract, simulate);
				if (ret.isEmpty()) {
					ret = extractedStack;
				} else {
					ret.setCount(ret.getCount() + extractedStack.getCount());
				}
			}
		}
		return ret;
	}

	public static ItemStack extractFromInventory(ItemStack stack, IItemHandler inventory, boolean simulate) {
		int extractedCount = 0;
		int slots = inventory.getSlots();
		for (int slot = 0; slot < slots && extractedCount < stack.getCount(); slot++) {
			ItemStack slotStack = inventory.getStackInSlot(slot);
			if (ItemStack.isSameItemSameComponents(stack, slotStack)) {
				int toExtract = Math.min(slotStack.getCount(), stack.getCount() - extractedCount);
				extractedCount += inventory.extractItem(slot, toExtract, simulate).getCount();
			}
		}

		if (extractedCount == 0) {
			return ItemStack.EMPTY;
		}

		ItemStack result = stack.copy();
		result.setCount(extractedCount);

		return result;
	}

	public static ItemStack runPickupOnPickupResponseUpgrades(Level level, UpgradeHandler upgradeHandler, ItemStack remainingStack, boolean simulate) {
		return runPickupOnPickupResponseUpgrades(level, null, upgradeHandler, remainingStack, simulate);
	}

	public static ItemStack runPickupOnPickupResponseUpgrades(Level level,
			@Nullable Player player, UpgradeHandler upgradeHandler, ItemStack remainingStack, boolean simulate) {
		List<IPickupResponseUpgrade> pickupUpgrades = upgradeHandler.getWrappersThatImplement(IPickupResponseUpgrade.class);

		for (IPickupResponseUpgrade pickupUpgrade : pickupUpgrades) {
			int countBeforePickup = remainingStack.getCount();
			Item item = remainingStack.getItem();
			remainingStack = pickupUpgrade.pickup(level, remainingStack, simulate);
			if (!simulate && player != null && remainingStack.getCount() != countBeforePickup) {
				playPickupSound(level, player);
				player.awardStat(Stats.ITEM_PICKED_UP.get(item), countBeforePickup - remainingStack.getCount());
			}

			if (remainingStack.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}

		return remainingStack;
	}

	private static void playPickupSound(Level level, @Nonnull Player player) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, RandHelper.getRandomMinusOneToOne(level.random) * 1.4F + 2.0F);
	}

	public static void iterate(IItemHandler handler, BiConsumer<Integer, ItemStack> actOn) {
		iterate(handler, actOn, () -> false);
	}

	public static void iterate(IItemHandler handler, BiConsumer<Integer, ItemStack> actOn, BooleanSupplier shouldExit) {
		iterate(handler, actOn, shouldExit, true);
	}

	public static void iterate(IItemHandler handler, BiConsumer<Integer, ItemStack> actOn, BooleanSupplier shouldExit, boolean getVirtualCounts) {
		int slots = handler.getSlots();
		for (int slot = 0; slot < slots; slot++) {
			ItemStack stack = !getVirtualCounts && handler instanceof InventoryHandler inventoryHandler ? inventoryHandler.getSlotStack(slot) : handler.getStackInSlot(slot);
			actOn.accept(slot, stack);
			if (shouldExit.getAsBoolean()) {
				break;
			}
		}
	}

	public static int getCountMissingInHandler(IItemHandler itemHandler, ItemStack filter, int expectedCount) {
		MutableInt missingCount = new MutableInt(expectedCount);
		iterate(itemHandler, (slot, stack) -> {
			if (ItemStack.isSameItemSameComponents(stack, filter)) {
				missingCount.subtract(Math.min(stack.getCount(), missingCount.getValue()));
			}
		}, () -> missingCount.getValue() == 0);
		return missingCount.getValue();
	}

	public static <T> T iterate(IItemHandler handler, BiFunction<Integer, ItemStack, T> getFromSlotStack, Supplier<T> supplyDefault, Predicate<T> shouldExit) {
		T ret = supplyDefault.get();
		int slots = handler.getSlots();
		for (int slot = 0; slot < slots; slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			ret = getFromSlotStack.apply(slot, stack);
			if (shouldExit.test(ret)) {
				break;
			}
		}
		return ret;
	}

	public static void transfer(IItemHandler handlerA, IItemHandler handlerB, Consumer<Supplier<ItemStack>> onInserted) {
		int slotsA = handlerA.getSlots();
		for (int slot = 0; slot < slotsA; slot++) {
			ItemStack slotStack = handlerA.getStackInSlot(slot);
			if (slotStack.isEmpty()) {
				continue;
			}

			int countToTransfer = slotStack.getCount();
			while (countToTransfer > 0) {
				ItemStack toInsert = slotStack.copy();
				toInsert.setCount(Math.min(slotStack.getMaxStackSize(), countToTransfer));
				ItemStack remainingAfterInsert = insertIntoInventory(toInsert, handlerB, true);
				if (remainingAfterInsert.getCount() == toInsert.getCount()) {
					break;
				}
				int toExtract = toInsert.getCount() - remainingAfterInsert.getCount();

				ItemStack extractedStack = handlerA.extractItem(slot, toExtract, true);
				if (extractedStack.isEmpty()) {
					break;
				}

				insertIntoInventory(handlerA.extractItem(slot, extractedStack.getCount(), false), handlerB, false);

				onInserted.accept(() -> {
					ItemStack copiedStack = slotStack.copy();
					copiedStack.setCount(extractedStack.getCount());
					return copiedStack;
				});
				countToTransfer -= extractedStack.getCount();
			}
		}
	}

	public static boolean isEmpty(IItemHandler itemHandler) {
		int slots = itemHandler.getSlots();
		for (int slot = 0; slot < slots; slot++) {
			if (!itemHandler.getStackInSlot(slot).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public static ItemStack getAndRemove(IItemHandler itemHandler, int slot) {
		if (slot >= itemHandler.getSlots()) {
			return ItemStack.EMPTY;
		}
		return itemHandler.extractItem(slot, itemHandler.getStackInSlot(slot).getCount(), false);
	}

	public static void insertOrDropItem(Player player, ItemStack stack, IItemHandler... inventories) {
		ItemStack ret = stack;
		for (IItemHandler inventory : inventories) {
			ret = insertIntoInventory(ret, inventory, false);
			if (ret.isEmpty()) {
				return;
			}
		}
		if (!ret.isEmpty()) {
			player.drop(ret, true);
		}
	}

	public static ItemStack mergeIntoPlayerInventory(Player player, ItemStack stack, int startSlot) {
		ItemStack result = stack.copy();
		List<Integer> emptySlots = new ArrayList<>();
		for (int slot = startSlot; slot < player.getInventory().items.size(); slot++) {
			ItemStack slotStack = player.getInventory().getItem(slot);
			if (slotStack.isEmpty()) {
				emptySlots.add(slot);
			}
			if (ItemStack.isSameItemSameComponents(slotStack, result)) {
				int count = Math.min(slotStack.getMaxStackSize() - slotStack.getCount(), result.getCount());
				slotStack.grow(count);
				result.shrink(count);
				if (result.isEmpty()) {
					return ItemStack.EMPTY;
				}
			}
		}

		for (int slot : emptySlots) {
			ItemStack slotStack = result.copy();
			slotStack.setCount(Math.min(slotStack.getMaxStackSize(), result.getCount()));
			player.getInventory().setItem(slot, slotStack);
			result.shrink(slotStack.getCount());
			if (result.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}

		return result;
	}

	static Map<ItemStackKey, Integer> getCompactedStacks(IItemHandler handler) {
		return getCompactedStacks(handler, new HashSet<>());
	}

	static Map<ItemStackKey, Integer> getCompactedStacks(IItemHandler handler, Set<Integer> ignoreSlots) {
		return getCompactedStacks(handler, ignoreSlots, true);
	}

	static Map<ItemStackKey, Integer> getCompactedStacks(IItemHandler handler, Set<Integer> ignoreSlots, boolean getVirtualCounts) {
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

	public static List<ItemStack> getCompactedStacksSortedByCount(IItemHandler handler) {
		Map<ItemStackKey, Integer> compactedStacks = getCompactedStacks(handler);
		List<Map.Entry<ItemStackKey, Integer>> sortedList = new ArrayList<>(compactedStacks.entrySet());
		sortedList.sort(InventorySorter.BY_COUNT);

		List<ItemStack> ret = new ArrayList<>();
		sortedList.forEach(e -> {
			ItemStack stackCopy = e.getKey().getStack().copy();
			stackCopy.setCount(e.getValue());
			ret.add(stackCopy);
		});
		return ret;
	}

	public static List<ItemStack> getStacks(IItemHandler handler) {
		List<ItemStack> ret = new ArrayList<>();
		iterate(handler, (slot, stack) -> {
			ret.add(stack);
		});
		return ret;
	}

	public static Set<ItemStackKey> getUniqueStacks(IItemHandler handler) {
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

	public static List<Integer> getEmptySlotsRandomized(IItemHandler inventory) {
		List<Integer> list = Lists.newArrayList();

		for (int i = 0; i < inventory.getSlots(); ++i) {
			if (inventory.getStackInSlot(i).isEmpty()) {
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

	public static void dropItems(ItemStackHandler inventoryHandler, Level level, BlockPos pos) {
		dropItems(inventoryHandler, level, pos.getX(), pos.getY(), pos.getZ());
	}

	public static void dropItems(ItemStackHandler inventoryHandler, Level level, double x, double y, double z) {
		iterate(inventoryHandler, (slot, stack) -> dropItem(inventoryHandler, level, x, y, z, slot, stack), () -> false, false);
	}

	public static void dropItem(ItemStackHandler handler, Level level, double x, double y, double z, Integer slot, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		if (handler instanceof InventoryHandler inventoryHandler) {
			int countToExtract = stack.getCount();
			while (countToExtract > 0) {
				int countToDrop = Math.min(stack.getMaxStackSize(), countToExtract);
				Containers.dropItemStack(level, x, y, z, stack.copyWithCount(countToDrop));
				countToExtract -= countToDrop;
			}
			inventoryHandler.setSlotStack(slot, ItemStack.EMPTY);
		} else {
			ItemStack extractedStack = handler.extractItem(slot, stack.getMaxStackSize(), false);
			while (!extractedStack.isEmpty()) {
				Containers.dropItemStack(level, x, y, z, extractedStack);
				extractedStack = handler.extractItem(slot, stack.getMaxStackSize(), false);
			}
			handler.setStackInSlot(slot, ItemStack.EMPTY);
		}
	}

	public static int getAnalogOutputSignal(ITrackedContentsItemHandler handler) {
		AtomicDouble totalFilled = new AtomicDouble(0);
		AtomicBoolean isEmpty = new AtomicBoolean(true);
		iterate(handler, (slot, stack) -> {
			if (!stack.isEmpty()) {
				int slotLimit = handler.getSlotLimit(slot);
				totalFilled.addAndGet(stack.getCount() / (slotLimit / ((float) 64 / stack.getMaxStackSize())));
				isEmpty.set(false);
			}
		});
		double percentFilled = totalFilled.get() / handler.getSlots();
		return Mth.floor(percentFilled * 14.0F) + (isEmpty.get() ? 0 : 1);
	}

	public static List<IItemHandler> getItemHandlersFromPlayerIncludingContainers(Player player) {
		List<IItemHandler> itemHandlers = new ArrayList<>();
		PLAYER_INVENTORY_PROVIDERS.forEach(provider -> {
			IItemHandler itemHandler = provider.apply(player);
			itemHandlers.add(itemHandler);
			for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
				ItemStack slotStack = itemHandler.getStackInSlot(slot);
				if (slotStack.isEmpty()) {
					continue;
				}
				IItemHandler containerHandler = slotStack.getCapability(Capabilities.ItemHandler.ITEM);
				if (containerHandler != null) {
					itemHandlers.add(containerHandler);
				}
			}
		});
		return itemHandlers;
	}

	public static List<IItemHandler> getEquipmentItemHandlersFromPlayer(Player player) {
		List<IItemHandler> itemHandlers = new ArrayList<>();
		PLAYER_EQUIPMENT_INVENTORY_PROVIDERS.forEach(provider -> {
			IItemHandler itemHandler = provider.apply(player);
			itemHandlers.add(itemHandler);
		});
		return itemHandlers;
	}
}
