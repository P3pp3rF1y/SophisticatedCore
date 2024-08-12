package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class NBTHelper {
	private NBTHelper() {}

/*	public static Optional<Integer> getInt(ItemStack stack, String key) {
		return getTagValue(stack, key, CompoundTag::getInt);
	}*/

	public static Optional<Integer> getInt(CompoundTag tag, String key) {
		return getTagValue(tag, key, CompoundTag::getInt);
	}

	public static Optional<int[]> getIntArray(CompoundTag tag, String key) {
		return getTagValue(tag, key, CompoundTag::getIntArray);
	}

/*	private static <T> Optional<T> getTagValue(ItemStack stack, String key, BiFunction<CompoundTag, String, T> getValue) {
		return getTagValue(stack, "", key, getValue);
	}*/

/*	public static <T> Optional<T> getTagValue(ItemStack stack, String parentKey, String key, BiFunction<CompoundTag, String, T> getValue) {
		CompoundTag tag = stack.getTag();

		if (tag == null) {
			return Optional.empty();
		}

		if (!parentKey.isEmpty()) {
			Tag parentTag = tag.get(parentKey);
			if (!(parentTag instanceof CompoundTag)) {
				return Optional.empty();
			}
			tag = (CompoundTag) parentTag;
		}

		return getTagValue(tag, key, getValue);
	}*/

	public static Optional<Boolean> getBoolean(CompoundTag tag, String key) {
		return getTagValue(tag, key, CompoundTag::getBoolean);
	}

	public static Optional<CompoundTag> getCompound(CompoundTag tag, String key) {
		return getTagValue(tag, key, CompoundTag::getCompound);
	}

	public static <T> Optional<T> getTagValue(CompoundTag tag, String key, BiFunction<CompoundTag, String, T> getValue) {
		if (!tag.contains(key)) {
			return Optional.empty();
		}

		return Optional.of(getValue.apply(tag, key));
	}

/*	public static <E, C extends Collection<E>> Optional<C> getCollection(ItemStack stack, String parentKey, String tagName, byte listType, Function<Tag, Optional<E>> getElement, Supplier<C> initCollection) {
		return getTagValue(stack, parentKey, tagName, (c, n) -> c.getList(n, listType)).map(listNbt -> {
			C ret = initCollection.get();
			listNbt.forEach(elementNbt -> getElement.apply(elementNbt).ifPresent(ret::add));
			return ret;
		});
	}*/

	public static <E, C extends Collection<E>> Optional<C> getCollection(CompoundTag tag, String key, byte listType, Function<Tag, Optional<E>> getElement, Supplier<C> initCollection) {
		return getTagValue(tag, key, (c, n) -> c.getList(n, listType)).map(listNbt -> {
			C ret = initCollection.get();
			listNbt.forEach(elementNbt -> getElement.apply(elementNbt).ifPresent(ret::add));
			return ret;
		});
	}

/*	public static Optional<CompoundTag> getCompound(ItemStack stack, String parentKey, String tagName) {
		return getTagValue(stack, parentKey, tagName, CompoundTag::getCompound);
	}

	public static Optional<CompoundTag> getCompound(ItemStack stack, String tagName) {
		return getTagValue(stack, tagName, CompoundTag::getCompound);
	}

	public static <T extends Enum<T>> Optional<T> getEnumConstant(ItemStack stack, String parentKey, String key, Function<String, T> deserialize) {
		return getTagValue(stack, parentKey, key, (t, k) -> deserialize.apply(t.getString(k)));
	}

	public static <T extends Enum<T>> Optional<T> getEnumConstant(ItemStack stack, String key, Function<String, T> deserialize) {
		return getTagValue(stack, key, (t, k) -> deserialize.apply(t.getString(k)));
	}*/

	public static <T extends Enum<T>> Optional<T> getEnumConstant(CompoundTag tag, String key, Function<String, T> deserialize) {
		return getTagValue(tag, key, (t, k) -> deserialize.apply(t.getString(k)));
	}

/*	public static Optional<Boolean> getBoolean(ItemStack stack, String parentKey, String key) {
		return getTagValue(stack, parentKey, key, CompoundTag::getBoolean);
	}

	public static Optional<Boolean> getBoolean(ItemStack stack, String key) {
		return getTagValue(stack, key, CompoundTag::getBoolean);
	}*/

/*	public static Optional<Long> getLong(ItemStack stack, String key) {
		return getTagValue(stack, key, CompoundTag::getLong);
	}*/

	public static Optional<Long> getLong(CompoundTag tag, String key) {
		return getTagValue(tag, key, CompoundTag::getLong);
	}

/*	public static Optional<UUID> getUniqueId(ItemStack stack, String key) {
		//noinspection ConstantConditions - contains check is run before this get so it won't be null
		return getTagValue(stack, key, (compound, k) -> NbtUtils.loadUUID(compound.get(k)));
	}*/

/*	public static void setCompoundNBT(ItemStack stack, String key, CompoundTag tag) {
		setCompoundNBT(stack, "", key, tag);
	}*/

/*	public static void setCompoundNBT(ItemStack stack, String parentKey, String key, CompoundTag tag) {
		if (parentKey.isEmpty()) {
			stack.getOrCreateTag().put(key, tag);
			return;
		}
		stack.getOrCreateTagElement(parentKey).put(key, tag);
	}*/

/*	public static void setBoolean(ItemStack stack, String parentKey, String key, boolean value) {
		if (parentKey.isEmpty()) {
			setBoolean(stack, key, value);
			return;
		}
		putBoolean(stack.getOrCreateTagElement(parentKey), key, value);
	}*/

/*	public static void setBoolean(ItemStack stack, String key, boolean value) {
		putBoolean(stack.getOrCreateTag(), key, value);
	}*/

/*	public static <T extends Enum<T> & StringRepresentable> void setEnumConstant(ItemStack stack, String parentKey, String key, T enumConstant) {
		if (parentKey.isEmpty()) {
			setEnumConstant(stack, key, enumConstant);
			return;
		}
		putEnumConstant(stack.getOrCreateTagElement(parentKey), key, enumConstant);
	}*/

/*	public static <T extends Enum<T> & StringRepresentable> void setEnumConstant(ItemStack stack, String key, T enumConstant) {
		putEnumConstant(stack.getOrCreateTag(), key, enumConstant);
	}*/

	public static CompoundTag putBoolean(CompoundTag tag, String key, boolean value) {
		tag.putBoolean(key, value);
		return tag;
	}

	public static CompoundTag putInt(CompoundTag tag, String key, int value) {
		tag.putInt(key, value);
		return tag;
	}

	public static CompoundTag putString(CompoundTag tag, String key, String value) {
		tag.putString(key, value);
		return tag;
	}

	public static <T extends Enum<T> & StringRepresentable> CompoundTag putEnumConstant(CompoundTag tag, String key, T enumConstant) {
		tag.putString(key, enumConstant.getSerializedName());
		return tag;
	}

/*
	public static void setLong(ItemStack stack, String key, long value) {
		stack.getOrCreateTag().putLong(key, value);
	}

	public static void setInteger(ItemStack stack, String key, int value) {
		stack.getOrCreateTag().putInt(key, value);
	}

	public static void setUniqueId(ItemStack stack, String key, UUID uuid) {
		stack.getOrCreateTag().putIntArray(key, UUIDUtil.uuidToIntArray(uuid));
	}

	public static void removeTag(ItemStack stack, String key) {
		if (stack.getTag() == null) {
			return;
		}
		stack.getTag().remove(key);
	}
*/

	public static Optional<Component> getComponent(CompoundTag tag, String key, HolderLookup.Provider registries) {
		return getTagValue(tag, key, (t, k) -> Component.Serializer.fromJson(t.getString(k), registries));
	}

	public static Optional<String> getString(CompoundTag tag, String key) {
		return getTagValue(tag, key, CompoundTag::getString);
	}

/*
	public static Optional<String> getString(ItemStack stack, String key) {
		return getTagValue(stack, key, CompoundTag::getString);
	}

	public static Optional<Float> getFloat(ItemStack stack, String key) {
		return getTagValue(stack, key, CompoundTag::getFloat);
	}

	public static <K, V> Optional<Map<K, V>> getMap(ItemStack stack, String key, Function<String, K> getKey, BiFunction<String, Tag, Optional<V>> getValue) {
		CompoundTag tag = stack.getTag();

		if (tag == null) {
			return Optional.empty();
		}

		return getMap(tag, key, getKey, getValue);
	}
*/

	public static <K, V> Optional<Map<K, V>> getMap(CompoundTag tag, String key, Function<String, K> getKey, BiFunction<String, Tag, Optional<V>> getValue) {
		return getMap(tag, key, getKey, getValue, HashMap::new);
	}

	public static <K, V> Optional<Map<K, V>> getMap(CompoundTag tag, String key, Function<String, K> getKey, BiFunction<String, Tag, Optional<V>> getValue, Supplier<Map<K, V>> initMap) {
		CompoundTag mapNbt = tag.getCompound(key);

		Map<K, V> map = initMap.get();

		for (String tagName : mapNbt.getAllKeys()) {
			getValue.apply(tagName, mapNbt.get(tagName)).ifPresent(value -> map.put(getKey.apply(tagName), value));
		}

		return Optional.of(map);
	}

	public static <K, V> CompoundTag putMap(CompoundTag tag, String key, Map<K, V> map, Function<K, String> getStringKey, Function<V, Tag> getNbtValue) {
		CompoundTag mapNbt = new CompoundTag();
		for (Map.Entry<K, V> entry : map.entrySet()) {
			mapNbt.put(getStringKey.apply(entry.getKey()), getNbtValue.apply(entry.getValue()));
		}
		tag.put(key, mapNbt);
		return tag;
	}

/*
	public static <T> void setList(ItemStack stack, String parentKey, String key, Collection<T> values, Function<T, Tag> getNbtValue) {
		ListTag list = new ListTag();
		values.forEach(v -> list.add(getNbtValue.apply(v)));
		if (parentKey.isEmpty()) {
			stack.getOrCreateTag().put(key, list);
		} else {
			stack.getOrCreateTagElement(parentKey).put(key, list);
		}
	}
*/

	public static <T> void putList(CompoundTag tag, String key, Collection<T> values, Function<T, Tag> getNbtValue) {
		ListTag list = new ListTag();
		values.forEach(v -> list.add(getNbtValue.apply(v)));
		tag.put(key, list);
	}
}
