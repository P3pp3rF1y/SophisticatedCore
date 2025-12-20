package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class NBTHelper {
		private NBTHelper() {
	}


	public static Optional<Integer> getInt(CompoundTag tag, String key) {
		return getTagValue(tag, key, CompoundTag::getInt);
	}

	public static Optional<int[]> getIntArray(CompoundTag tag, String key) {
		return getTagValue(tag, key, CompoundTag::getIntArray);
	}

	public static Optional<Boolean> getBoolean(CompoundTag tag, String key) {
		return getTagValue(tag, key, CompoundTag::getBoolean);
	}

	public static Optional<CompoundTag> getCompound(CompoundTag tag, String key) {
		return getTagValue(tag, key, CompoundTag::getCompound);
	}

	public static <T> Optional<T> getTagValue(CompoundTag tag, String key, BiFunction<CompoundTag, String, Optional<T>> getValue) {
		if (!tag.contains(key)) {
			return Optional.empty();
		}

		return getValue.apply(tag, key);
	}

	public static <E, C extends Collection<E>> Optional<C> getCollection(CompoundTag tag, String key, Function<Tag, Optional<E>> getElement, Supplier<C> initCollection) {
		return getTagValue(tag, key, CompoundTag::getList).map(listNbt -> {
			C ret = initCollection.get();
			listNbt.forEach(elementNbt -> getElement.apply(elementNbt).ifPresent(ret::add));
			return ret;
		});
	}

	public static <T extends Enum<T>> Optional<T> getEnumConstant(CompoundTag tag, String key, Function<String, T> deserialize) {
		return getTagValue(tag, key, (t, k) -> t.getString(k).map(deserialize));
	}

	public static Optional<Long> getLong(CompoundTag tag, String key) {
		return getTagValue(tag, key, CompoundTag::getLong);
	}

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

	public static Optional<String> getString(CompoundTag tag, String key) {
		return getTagValue(tag, key, CompoundTag::getString);
	}

	public static <K, V> Optional<Map<K, V>> getMap(CompoundTag tag, String key, Function<String, K> getKey, BiFunction<String, Tag, Optional<V>> getValue) {
		return getMap(tag, key, getKey, getValue, HashMap::new);
	}

	public static <K, V> Optional<Map<K, V>> getMap(CompoundTag tag, String key, Function<String, K> getKey, BiFunction<String, Tag, Optional<V>> getValue, Supplier<Map<K, V>> initMap) {
		return tag.getCompound(key).map(mapNbt -> {
			Map<K, V> map = initMap.get();

			for (String tagName : mapNbt.keySet()) {
				getValue.apply(tagName, mapNbt.get(tagName)).ifPresent(value -> map.put(getKey.apply(tagName), value));
			}
			return map;
		});
	}

	public static <K, V> CompoundTag putMap(CompoundTag tag, String key, Map<K, V> map, Function<K, String> getStringKey, Function<V, Tag> getNbtValue) {
		CompoundTag mapNbt = new CompoundTag();
		for (Map.Entry<K, V> entry : map.entrySet()) {
			mapNbt.put(getStringKey.apply(entry.getKey()), getNbtValue.apply(entry.getValue()));
		}
		tag.put(key, mapNbt);
		return tag;
	}

	public static <T> void putList(CompoundTag tag, String key, Collection<T> values, Function<T, Tag> getNbtValue) {
		ListTag list = new ListTag();
		values.forEach(v -> list.add(getNbtValue.apply(v)));
		tag.put(key, list);
	}

	public static Optional<Tag> serializeStackToTag(ItemStack stack) {
		return RegistryHelper.getRegistryAccess().map(registries -> {
					RegistryOps<Tag> registryops = registries.createSerializationContext(NbtOps.INSTANCE);
					return ItemStack.OPTIONAL_CODEC.encodeStart(registryops, stack).getOrThrow();
				}
		);
	}

	public static Optional<ItemStack> deserializeStackFromTag(Tag tag) {
		return RegistryHelper.getRegistryAccess().map(registries -> {
			RegistryOps<Tag> registryops = registries.createSerializationContext(NbtOps.INSTANCE);
			return ItemStack.OPTIONAL_CODEC.orElse(ItemStack.EMPTY).parse(registryops, tag).getOrThrow();
		});
	}

	public static Optional<Tag> serializeFluidToTag(FluidStack fluidStack) {
		return RegistryHelper.getRegistryAccess().map(registryAccess -> {
			RegistryOps<Tag> registryops = registryAccess.createSerializationContext(NbtOps.INSTANCE);
			return FluidStack.OPTIONAL_CODEC.encodeStart(registryops, fluidStack).getOrThrow();
		});
	}

	public static Optional<FluidStack> deserializeFluidFromTag(Tag tag) {
		return RegistryHelper.getRegistryAccess().map(registryAccess -> {
			RegistryOps<Tag> registryops = registryAccess.createSerializationContext(NbtOps.INSTANCE);
			return FluidStack.OPTIONAL_CODEC.parse(registryops, tag).getOrThrow();
		});
	}
}
