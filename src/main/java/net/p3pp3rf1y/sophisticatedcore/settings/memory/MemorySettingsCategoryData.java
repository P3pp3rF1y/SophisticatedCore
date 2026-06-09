package net.p3pp3rf1y.sophisticatedcore.settings.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MemorySettingsCategoryData implements ContainerContents.ISettingsCategoryData<MemorySettingsCategoryData> {
	public static final Codec<MemorySettingsCategoryData> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Codec.unboundedMap(CodecHelper.STRING_ENCODED_INT, Item.CODEC.orElse(Items.AIR.builtInRegistryHolder())).xmap(MemorySettingsCategoryData::toSlotFilterItems, MemorySettingsCategoryData::fromSlotFilterItems).fieldOf("slotFilterItems").forGetter(MemorySettingsCategoryData::slotFilterItems),
					Codec.unboundedMap(CodecHelper.STRING_ENCODED_INT, ItemStack.OPTIONAL_CODEC.orElse(ItemStack.EMPTY)).xmap(MemorySettingsCategoryData::toSlotFilterStacks, MemorySettingsCategoryData::fromSlotFilterStacks).fieldOf("slotFilterStacks").forGetter(MemorySettingsCategoryData::slotFilterStacks),
					Codec.BOOL.fieldOf("ignoreNbt").forGetter(MemorySettingsCategoryData::ignoreNbt)
			).apply(instance, MemorySettingsCategoryData::new)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, MemorySettingsCategoryData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, Item.STREAM_CODEC.map(Holder::value, Item::builtInRegistryHolder)),
			MemorySettingsCategoryData::slotFilterItems,
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ItemStack.STREAM_CODEC.map(ItemStackKey::of, ItemStackKey::stack)),
			MemorySettingsCategoryData::slotFilterStacks,
			ByteBufCodecs.BOOL,
			MemorySettingsCategoryData::ignoreNbt,
			MemorySettingsCategoryData::new
	);

	private Map<Integer, Item> slotFilterItems = new LinkedHashMap<>();
	private Map<Integer, ItemStackKey> slotFilterStacks = new LinkedHashMap<>();
	private boolean ignoreNbt = true;

	public MemorySettingsCategoryData() {}

	private static Map<Integer, Item> toSlotFilterItems(Map<Integer, Holder<Item>> items) {
		Map<Integer, Item> slotFilterItems = new LinkedHashMap<>();
		items.forEach((slot, itemHolder) -> {
			Item item = itemHolder.value();
			if (item != Items.AIR) {
				slotFilterItems.put(slot, item);
			}
		});
		return slotFilterItems;
	}

	private static Map<Integer, Holder<Item>> fromSlotFilterItems(Map<Integer, Item> slotFilterItems) {
		Map<Integer, Holder<Item>> items = new LinkedHashMap<>();
		slotFilterItems.forEach((slot, item) -> {
			if (item != Items.AIR) {
				items.put(slot, item.builtInRegistryHolder());
			}
		});
		return items;
	}

	private static Map<Integer, ItemStackKey> toSlotFilterStacks(Map<Integer, ItemStack> stacks) {
		Map<Integer, ItemStackKey> slotFilterStacks = new LinkedHashMap<>();
		stacks.forEach((slot, stack) -> {
			if (!stack.isEmpty()) {
				slotFilterStacks.put(slot, ItemStackKey.of(stack));
			}
		});
		return slotFilterStacks;
	}

	private static Map<Integer, ItemStack> fromSlotFilterStacks(Map<Integer, ItemStackKey> slotFilterStacks) {
		Map<Integer, ItemStack> stacks = new LinkedHashMap<>();
		slotFilterStacks.forEach((slot, stackKey) -> {
			ItemStack stack = stackKey.stack();
			if (!stack.isEmpty()) {
				stacks.put(slot, stack);
			}
		});
		return stacks;
	}

	public MemorySettingsCategoryData(Map<Integer, Item> slotFilterItems, Map<Integer, ItemStackKey> slotFilterStacks, boolean ignoreNbt) {
		this.slotFilterItems.putAll(slotFilterItems);
		this.slotFilterStacks.putAll(slotFilterStacks);
		this.ignoreNbt = ignoreNbt;
	}

	public Map<Integer, Item> slotFilterItems() {
		return slotFilterItems;
	}

	public Map<Integer, ItemStackKey> slotFilterStacks() {
		return slotFilterStacks;
	}

	public boolean ignoreNbt() {
		return ignoreNbt;
	}

	@Override
	public String id() {
		return MemorySettingsCategory.NAME;
	}

	@Override
	public MemorySettingsCategoryData copy() {
		return new MemorySettingsCategoryData(new LinkedHashMap<>(slotFilterItems), new LinkedHashMap<>(slotFilterStacks), ignoreNbt);
	}

	@Override
	public void reloadFrom(MemorySettingsCategoryData other) {
		this.ignoreNbt = other.ignoreNbt;
		this.slotFilterItems = new LinkedHashMap<>(other.slotFilterItems);
		this.slotFilterStacks = new LinkedHashMap<>(other.slotFilterStacks);
	}

	public void addSlotStack(int slot, ItemStackKey stackKey) {
		slotFilterStacks.put(slot, stackKey);
	}

	public void clearSlotFilterStacks() {
		slotFilterStacks.clear();
	}

	public void clearSlotFilterItems() {
		slotFilterItems.clear();
	}

	public void setIgnoreNbt(boolean ignoreNbt) {
		this.ignoreNbt = ignoreNbt;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MemorySettingsCategoryData that)) return false;
		return ignoreNbt == that.ignoreNbt && Objects.equals(slotFilterItems, that.slotFilterItems) && Objects.equals(slotFilterStacks, that.slotFilterStacks);
	}

	@Override
	public int hashCode() {
		return Objects.hash(slotFilterItems, slotFilterStacks, ignoreNbt);
	}
}
