package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemStackKey {
	private final ItemStack stack;

	private static final Map<ItemStack, ItemStackKey> CACHE = new ConcurrentHashMap<>();

	public static ItemStackKey of(ItemStack stack) {
		return CACHE.computeIfAbsent(stack, ItemStackKey::new);
	}

	private boolean hashInitialized = false;
	private int hash;

	private ItemStackKey(ItemStack stack) {
		this.stack = stack.copy();
		this.stack.setCount(1);
	}

	public static void clearCacheOnTickEnd(ServerTickEvent.Post event) {
		CACHE.clear();
	}

	public ItemStack getStack() {
		return stack;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ItemStackKey that = (ItemStackKey) o;
		return ItemStack.isSameItemSameComponents(stack, that.stack);
	}

	public boolean hashCodeNotEquals(ItemStack otherStack) {
		return hashCode() != ItemStack.hashItemAndComponents(otherStack);
	}

	@Override
	public int hashCode() {
		if (!hashInitialized) {
			hashInitialized = true;
			hash = ItemStack.hashItemAndComponents(stack);
		}
		return hash;
	}

	public boolean matches(ItemStack stack) {
		return hashCode() == ItemStack.hashItemAndComponents(stack);
	}

	public ItemStack stack() {
		return stack;
	}

	@Override
	public String toString() {
		return "ItemStackKey[stack=" + stack + ']';
	}
}
