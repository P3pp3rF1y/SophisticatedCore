package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Objects;

public record ItemStackKey(ItemStack stack) {
	public ItemStack getStack() {
		return stack;
	}

	public ItemStackKey(ItemStack stack) {
		this.stack = stack.copy();
		this.stack.setCount(1);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {return true;}
		if (o == null || getClass() != o.getClass()) {return false;}
		ItemStackKey that = (ItemStackKey) o;
		return canItemStacksStack(stack, that.stack);
	}

	public static boolean canItemStacksStack(ItemStack a, ItemStack b) {
		if (a.isEmpty() || !a.sameItem(b) || a.hasTag() != b.hasTag()) {
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
		return getHashCode(stack);
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

	private static final Field CAP_NBT = ObfuscationReflectionHelper.findField(ItemStack.class, "capNBT");

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
}
