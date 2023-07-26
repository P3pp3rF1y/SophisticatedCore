package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ItemStackKey {
	private static final Field CAP_NBT = ObfuscationReflectionHelper.findField(ItemStack.class, "capNBT");
	private final ItemStack stack;

	private static final Map<ItemStack, ItemStackKey> CACHE = new HashMap<>();

	public static ItemStackKey of(ItemStack stack) {
		return CACHE.computeIfAbsent(stack, ItemStackKey::new);
	}

	private boolean hashInitialized = false;
	private int hash;

	private ItemStackKey(ItemStack stack) {
		this.stack = stack.copy();
		this.stack.setCount(1);
	}

	public static void clearCacheOnTickEnd(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		CACHE.clear();
	}

	public ItemStack getStack() {
		return stack;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {return true;}
		if (o == null || getClass() != o.getClass()) {return false;}
		ItemStackKey that = (ItemStackKey) o;
		return canItemStacksStack(stack, that.stack);
	}

	public static boolean canItemStacksStack(ItemStack a, ItemStack b) {
		if (a.isEmpty() || a.getItem() != b.getItem() || a.hasTag() != b.hasTag()) {
			return false;
		}

		//noinspection DataFlowIssue
		return (!a.hasTag() || a.getTag().equals(b.getTag())) && Objects.equals(getCapNbt(a), getCapNbt(b));
	}

	public boolean hashCodeNotEquals(ItemStack otherStack) {
		return hashCode() != getHashCode(otherStack);
	}

	@Override
	public int hashCode() {
		if (!hashInitialized) {
			hashInitialized = true;
			hash = getHashCode(stack);
		}
		return hash;
	}

	public static int getHashCode(ItemStack stack) {
		int hash = stack.getItem().hashCode();
		if (stack.hasTag()) {
			//noinspection ConstantConditions - hasTag call makes sure getTag doesn't return null
			hash = hash * 31 + stack.getTag().hashCode();
		}
		CompoundTag capNbt = getCapNbt(stack);
		if (capNbt != null && !capNbt.isEmpty()) {
			hash = hash * 31 + capNbt.hashCode();
		}
		return hash;
	}

	@Nullable
	private static CompoundTag getCapNbt(ItemStack stack) {
		try {
			return (CompoundTag) CAP_NBT.get(stack);
		}
		catch (IllegalAccessException e) {
			SophisticatedCore.LOGGER.error("Error getting capNBT of stack ", e);
			return null;
		}
	}

	public boolean matches(ItemStack stack) {
		return hashCode() == getHashCode(stack);
	}

	public ItemStack stack() {return stack;}

	@Override
	public String toString() {
		return "ItemStackKey[stack=" + stack + ']';
	}

}
