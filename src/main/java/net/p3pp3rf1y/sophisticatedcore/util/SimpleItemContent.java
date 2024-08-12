package net.p3pp3rf1y.sophisticatedcore.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class SimpleItemContent implements DataComponentHolder {
	public static final SimpleItemContent EMPTY = new SimpleItemContent(ItemStack.EMPTY);
	public static final Codec<SimpleItemContent> CODEC = ItemStack.OPTIONAL_CODEC
			.xmap(SimpleItemContent::new, content -> content.itemStack);
	public static final StreamCodec<RegistryFriendlyByteBuf, SimpleItemContent> STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC
			.map(SimpleItemContent::new, content -> content.itemStack);

	private final ItemStack itemStack;

	private SimpleItemContent(ItemStack itemStack) {
		this.itemStack = itemStack;
	}

	public static SimpleItemContent copyOf(ItemStack itemStack) {
		return itemStack.isEmpty() ? EMPTY : new SimpleItemContent(itemStack.copy());
	}

	public ItemStack copy() {
		return this.itemStack.copy();
	}

	public boolean isEmpty() {
		return this.itemStack.isEmpty();
	}

	public Item getItem() {
		return itemStack.getItem();
	}

	public boolean is(TagKey<Item> tag) {
		return itemStack.is(tag);
	}

	public boolean is(Item item) {
		return itemStack.is(item);
	}

	public boolean is(Predicate<Holder<Item>> predicate) {
		return itemStack.is(predicate);
	}

	public boolean is(Holder<Item> holder) {
		return itemStack.is(holder);
	}

	public boolean is(HolderSet<Item> holders) {
		return itemStack.is(holders);
	}

	public int getCount() {
		return itemStack.getCount();
	}

	public boolean matches(ItemStack other) {
		return ItemStack.isSameItemSameComponents(itemStack, other);
	}

	public boolean isSameItem(ItemStack other) {
		return ItemStack.isSameItem(itemStack, other);
	}

	public boolean isSameItemSameComponents(ItemStack other) {
		return ItemStack.isSameItemSameComponents(itemStack, other);
	}

	public boolean isSameItemSameComponents(SimpleItemContent content) {
		return isSameItemSameComponents(content.itemStack);
	}

	@Override
	public DataComponentMap getComponents() {
		return itemStack.getComponents();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof SimpleItemContent o)) {
			return false;
		} else {
			return ItemStack.isSameItemSameComponents(this.itemStack, o.itemStack);
		}
	}

	@Override
	public int hashCode() {
		return this.itemStack.getCount() * 31 + ItemStack.hashItemAndComponents(this.itemStack);
	}
}
