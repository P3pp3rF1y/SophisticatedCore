package net.p3pp3rf1y.sophisticatedcore.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Predicate;

public class SimpleItemContent implements DataComponentHolder {
	public static final SimpleItemContent EMPTY = new SimpleItemContent(Items.AIR.builtInRegistryHolder(), 0, DataComponentPatch.EMPTY);
	public static final Codec<SimpleItemContent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Item.CODEC.optionalFieldOf("id", Items.AIR.builtInRegistryHolder()).forGetter(SimpleItemContent::itemHolder),
			Codec.intRange(0, 99).optionalFieldOf("count", 0).forGetter(SimpleItemContent::getCount),
			DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(SimpleItemContent::componentsPatch)
	).apply(instance, SimpleItemContent::fromSerialized));
	public static final StreamCodec<RegistryFriendlyByteBuf, SimpleItemContent> STREAM_CODEC = StreamCodec.composite(
			Item.STREAM_CODEC,
			SimpleItemContent::itemHolder,
			ByteBufCodecs.VAR_INT,
			SimpleItemContent::getCount,
			DataComponentPatch.STREAM_CODEC,
			SimpleItemContent::componentsPatch,
			SimpleItemContent::fromSerialized
	);

	private final Holder<Item> item;
	private final int count;
	private final DataComponentPatch components;

	private SimpleItemContent(Holder<Item> item, int count, DataComponentPatch components) {
		this.item = item;
		this.count = count;
		this.components = components;
	}

	private static SimpleItemContent fromSerialized(Holder<Item> item, int count, DataComponentPatch components) {
		return count <= 0 || item.is(Items.AIR.builtInRegistryHolder()) ? EMPTY : new SimpleItemContent(item, count, components);
	}

	private Holder<Item> itemHolder() {
		return item;
	}

	private DataComponentPatch componentsPatch() {
		return components;
	}

	public static SimpleItemContent copyOf(ItemStack itemStack) {
		return itemStack.isEmpty() ? EMPTY : new SimpleItemContent(itemStack.getItem().builtInRegistryHolder(), itemStack.getCount(), itemStack.getComponentsPatch());
	}

	public static SimpleItemContent of(Item item, int count, DataComponentPatch components) {
		return count <= 0 ? EMPTY : new SimpleItemContent(item.builtInRegistryHolder(), count, components);
	}

	public ItemStack copy() {
		return isEmpty() ? ItemStack.EMPTY : new ItemStack(item, count, components);
	}

	public boolean isEmpty() {
		return count <= 0 || item.is(Items.AIR.builtInRegistryHolder());
	}

	public Item getItem() {
		return item.value();
	}

	public boolean is(TagKey<Item> tag) {
		return item.is(tag);
	}

	public boolean is(Item item) {
		return this.item.is(item.builtInRegistryHolder());
	}

	public boolean is(Predicate<Holder<Item>> predicate) {
		return predicate.test(item);
	}

	public boolean is(Holder<Item> holder) {
		return item.is(holder);
	}

	public boolean is(HolderSet<Item> holders) {
		return copy().is(holders);
	}

	public int getCount() {
		return count;
	}

	public boolean matches(ItemStack other) {
		return ItemStack.isSameItemSameComponents(copy(), other);
	}

	public boolean isSameItem(ItemStack other) {
		return ItemStack.isSameItem(copy(), other);
	}

	public boolean isSameItemSameComponents(ItemStack other) {
		return ItemStack.isSameItemSameComponents(copy(), other);
	}

	public boolean isSameItemSameComponents(SimpleItemContent content) {
		return isSameItemSameComponents(content.copy());
	}

	@Override
	public DataComponentMap getComponents() {
		return isEmpty() ? DataComponentMap.EMPTY : copy().getComponents();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof SimpleItemContent o)) {
			return false;
		} else {
			return ItemStack.isSameItemSameComponents(copy(), o.copy());
		}
	}

	@Override
	public int hashCode() {
		ItemStack itemStack = copy();
		return itemStack.getCount() * 31 + ItemStack.hashItemAndComponents(itemStack);
	}
}
