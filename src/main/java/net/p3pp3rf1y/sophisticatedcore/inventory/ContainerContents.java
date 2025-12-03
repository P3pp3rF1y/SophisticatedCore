package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.settings.main.Context;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ContainerContents(InventoryData inventory, PartitionerData partitioner, UpgradeData upgrades,
								SettingsData settings) {
	public static Codec<ContainerContents> CODEC = Codec.withAlternative(
			RecordCodecBuilder.create(
					instance -> instance.group(
							InventoryData.CODEC.fieldOf("inventory").forGetter(ContainerContents::inventory),
							PartitionerData.CODEC.fieldOf("partitioner").forGetter(ContainerContents::partitioner),
							UpgradeData.CODEC.fieldOf("upgrades").forGetter(ContainerContents::upgrades),
							SettingsData.CODEC.fieldOf("settings").forGetter(ContainerContents::settings)
					).apply(instance, ContainerContents::new)),
			CompoundTag.CODEC, LegacyDeserialization::legacyDeserialize
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, ContainerContents> STREAM_CODEC = StreamCodec.composite(
			InventoryData.STREAM_CODEC,
			ContainerContents::inventory,
			PartitionerData.STREAM_CODEC,
			ContainerContents::partitioner,
			UpgradeData.STREAM_CODEC,
			ContainerContents::upgrades,
			SettingsData.STREAM_CODEC,
			ContainerContents::settings,
			ContainerContents::new
	);

	public ContainerContents() {
		this(new InventoryData(),
				new PartitionerData(),
				new UpgradeData(),
				new SettingsData()
		);
	}

	public ContainerContents copy() {
		return new ContainerContents(inventory.copy(), partitioner.copy(), upgrades.copy(), settings.copy());
	}

	public void reloadFrom(ContainerContents contents) {
		inventory.reloadFrom(contents.inventory);
		partitioner.reloadFrom(contents.partitioner);
		upgrades.reloadFrom(contents.upgrades);
		settings.reloadFrom(contents.settings);
	}

	public static class InventoryData {
		public static final Codec<InventoryData> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						CodecHelper.OPTIONAL_OVERSIZED_ITEM_STACK_CODEC.listOf().xmap(CodecHelper::toMutableNonnullItemStackList, Function.identity()).fieldOf("stacks").forGetter(InventoryData::stacks)
				).apply(instance, InventoryData::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, InventoryData> STREAM_CODEC = StreamCodec.composite(
				ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).map(CodecHelper::toMutableNonnullItemStackList, Function.identity()),
				InventoryData::stacks,
				InventoryData::new
		);

		private NonNullList<ItemStack> stacks;

		public InventoryData() {
			this.stacks = NonNullList.create();
		}

		public InventoryData(NonNullList<ItemStack> stacks) {
			this.stacks = stacks;
		}

		public NonNullList<ItemStack> stacks() {
			return stacks;
		}

		public InventoryData copy() {
			return new InventoryData(stacks.stream().map(ItemStack::copy).collect(Collectors.toCollection(NonNullList::create)));
		}

		public void reloadFrom(InventoryData inventory) {
			stacks = inventory.stacks.stream().map(ItemStack::copy).collect(Collectors.toCollection(NonNullList::create));
		}

		public void resize(int newSize) {
			NonNullList<ItemStack> newStacks = NonNullList.withSize(newSize, ItemStack.EMPTY);
			for (int i = 0; i < Math.min(stacks.size(), newSize); i++) {
				newStacks.set(i, stacks.get(i));
			}
			stacks = newStacks;
		}
	}

	public static final class PartitionerData {
		private static final Codec<PartitionerData> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Codec.INT.listOf().fieldOf("baseIndexes").forGetter(data -> Arrays.stream(data.baseIndexes).boxed().toList()),
						Codec.STRING.listOf().fieldOf("partNames").forGetter(data -> data.partNames)
				).apply(instance, (baseIndexesList, partNames) -> new PartitionerData(
						baseIndexesList.stream().mapToInt(Integer::intValue).toArray(),
						partNames
				))
		);
		private static final StreamCodec<RegistryFriendlyByteBuf, PartitionerData> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).map(list -> list.stream().mapToInt(Integer::intValue).toArray(), arr -> Arrays.stream(arr).boxed().toList()),
						PartitionerData::baseIndexes,
						ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
						PartitionerData::partNames,
						PartitionerData::new
				);

		private int[] baseIndexes;
		private List<String> partNames;

		public PartitionerData() {
			this.baseIndexes = new int[]{0};
			this.partNames = List.of(IInventoryPartHandler.Default.NAME);
		}

		public PartitionerData(int[] baseIndexes, List<String> partNames) {
			this.baseIndexes = baseIndexes;
			this.partNames = partNames;
		}

		public int[] baseIndexes() {
			return baseIndexes;
		}

		public List<String> partNames() {
			return partNames;
		}

		public PartitionerData copy() {
			return new PartitionerData(Arrays.copyOf(baseIndexes, baseIndexes.length), List.copyOf(partNames));
		}

		public void reloadFrom(PartitionerData partitioner) {
			baseIndexes = partitioner.baseIndexes;
			partNames = partitioner.partNames;
		}

		public void setPartBaseIndexesAndNames(int[] baseIndexes, List<String> partNames) {
			this.baseIndexes = baseIndexes;
			this.partNames = partNames;
		}
	}

	public static class UpgradeData {
		public static final Codec<UpgradeData> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						ItemStack.OPTIONAL_CODEC.listOf().xmap(CodecHelper::toMutableNonnullItemStackList, Function.identity()).fieldOf("stacks").forGetter(UpgradeData::stacks)
				).apply(instance, UpgradeData::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeData> STREAM_CODEC = StreamCodec.composite(
				ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).map(CodecHelper::toMutableNonnullItemStackList, Function.identity()),
				UpgradeData::stacks,
				UpgradeData::new
		);

		private NonNullList<ItemStack> stacks;

		public UpgradeData() {
			this.stacks = NonNullList.create();
		}

		public UpgradeData(NonNullList<ItemStack> stacks) {
			this.stacks = stacks;
		}

		public NonNullList<ItemStack> stacks() {
			return stacks;
		}

		public UpgradeData copy() {
			return new UpgradeData(stacks.stream().map(ItemStack::copy).collect(Collectors.toCollection(NonNullList::create)));
		}

		public void reloadFrom(UpgradeData upgrades) {
			stacks = upgrades.stacks.stream().map(ItemStack::copy).collect(Collectors.toCollection(NonNullList::create));
		}

		public void setStacks(NonNullList<ItemStack> stacks) {
			this.stacks = stacks;
		}
	}

	public static class SettingsData {
		public static final Codec<SettingsData> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Codec.dispatchedMap(Codec.STRING, SettingsCategoryDataRegistry::<ISettingsCategoryData<?>>getCodecOrThrow)
								.xmap(CodecHelper::toMutable, Function.identity()).fieldOf("categories").forGetter(SettingsData::categories),
						Context.CODEC.fieldOf("mainSettingsContext").forGetter(SettingsData::mainSettingsContext),
						Codec.STRING.fieldOf("searchPhrase").forGetter(data -> data.searchPhrase)
				).apply(instance, SettingsData::new)
		);

		public static final StreamCodec<RegistryFriendlyByteBuf, SettingsData> STREAM_CODEC =
				StreamCodec.composite(
						StreamCodec.of(
								(buf, categories) -> {
									buf.writeVarInt(categories.size());
									for (Map.Entry<String, ISettingsCategoryData<?>> entry : categories.entrySet()) {
										buf.writeUtf(entry.getKey());
										SettingsCategoryDataRegistry.getStreamCodecOrThrow(entry.getKey()).encode(buf, entry.getValue());
									}
								},
								buf -> {
									int size = buf.readVarInt();
									Map<String, ISettingsCategoryData<?>> categories = new HashMap<>();
									for (int i = 0; i < size; i++) {
										String id = buf.readUtf();
										categories.put(id, SettingsCategoryDataRegistry.getStreamCodecOrThrow(id).decode(buf));
									}
									return categories;
								}
						),
						SettingsData::categories,
						Context.STREAM_CODEC,
						SettingsData::mainSettingsContext,
						ByteBufCodecs.STRING_UTF8,
						SettingsData::searchPhrase,
						SettingsData::new
				);

		private final Map<String, ISettingsCategoryData<?>> categories;
		private Context mainSettingsContext = Context.PLAYER;
		private String searchPhrase = "";

		public SettingsData(Map<String, ISettingsCategoryData<?>> categories, Context mainSettingsContext, String searchPhrase) {
			this.categories = categories;
			this.mainSettingsContext = mainSettingsContext;
			this.searchPhrase = searchPhrase;
		}

		public SettingsData() {
			this.categories = new HashMap<>();
		}

		public Map<String, ISettingsCategoryData<?>> categories() {
			return categories;
		}

		public <D extends ISettingsCategoryData<D>> D getCategoryData(String id) {
			//noinspection unchecked
			return (D) categories.get(id);
		}

		public Context mainSettingsContext() {
			return mainSettingsContext;
		}

		public void setMainSettingsContext(Context context) {
			mainSettingsContext = context;
		}

		public String searchPhrase() {
			return searchPhrase;
		}

		public void setSearchPhrase(String searchPhrase) {
			this.searchPhrase = searchPhrase;
		}

		public SettingsData copy() {
			Map<String, ISettingsCategoryData<?>> copiedCategories = new HashMap<>();
			categories.forEach((key, value) -> copiedCategories.put(key, value.copy()));
			return new SettingsData(copiedCategories, mainSettingsContext, searchPhrase);
		}

		public void reloadFrom(SettingsData settings) {
			categories.forEach((name, categoryData) -> {
				if (settings.categories.containsKey(name)) {
					categoryData.reloadFromAny(settings.categories.get(name));
				}
			});
			mainSettingsContext = settings.mainSettingsContext;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			SettingsData that = (SettingsData) obj;
			return Objects.equals(categories, that.categories);
		}
	}

	public interface ISettingsCategoryData<T extends ISettingsCategoryData<T>> {
		String id();

		T copy();

		void reloadFrom(T other);

		@SuppressWarnings("unchecked")
		default void reloadFromAny(ISettingsCategoryData<?> src) {
			if (getClass().isInstance(src)) {
				reloadFrom((T) src);
			}
		}
	}

	public static class SettingsCategoryDataRegistry {
		private static final Map<String, Codec<? extends ISettingsCategoryData<?>>> CODECS = new HashMap<>();
		private static final Map<String, StreamCodec<RegistryFriendlyByteBuf, ? extends ISettingsCategoryData<?>>> STREAM_CODECS = new HashMap<>();

		private SettingsCategoryDataRegistry() {
		}

		public static void register(Codec<? extends ISettingsCategoryData<?>> codec, StreamCodec<RegistryFriendlyByteBuf, ? extends ISettingsCategoryData<?>> streamCodec, String id) {
			CODECS.put(id, codec);
			STREAM_CODECS.put(id, streamCodec);
		}

		public static <T extends ISettingsCategoryData<?>> Codec<T> getCodecOrThrow(String id) {
			if (!CODECS.containsKey(id)) {
				throw new IllegalArgumentException("SettingsCategoryData codec not found for id: " + id);
			}

			//noinspection unchecked
			return (Codec<T>) CODECS.get(id);
		}

		public static <T extends ISettingsCategoryData<?>> StreamCodec<RegistryFriendlyByteBuf, T> getStreamCodecOrThrow(String id) {
			if (!STREAM_CODECS.containsKey(id)) {
				throw new IllegalArgumentException("SettingsCategoryData stream codec not found for id: " + id);
			}
			//noinspection unchecked
			return (StreamCodec<RegistryFriendlyByteBuf, T>) STREAM_CODECS.get(id);
		}
	}


	public static class LegacyDeserialization {
		private static final String INVENTORY_TAG = "inventory";
		private static final String PARTITIONER_TAG = "partitioner";
		private static final String BASE_INDEXES_TAG = "baseIndexes";
		private static final String UPGRADE_INVENTORY_TAG = "upgradeInventory";
		public static final String SETTINGS_TAG = "settings";
		private static final String COLOR_TAG = "color";
		private static final String SELECTED_SLOTS_TAG = "selectedSlots";
		private static final String SLOT_FILTER_ITEMS_TAG = "slotFilterItems";
		private static final String SLOT_FILTER_STACKS_TAG = "slotFilterStacks";
		private static final String IGNORE_NBT_TAG = "ignoreNbt";
		private static final String SLOTS_TAG = "slots";
		private static final String ROTATIONS_TAG = "rotations";
		private static final String DISPLAY_SIDE_TAG = "displaySide";

		private static ContainerContents legacyDeserialize(CompoundTag contentsNbt) {
			return new ContainerContents(
					deserializeInventoryData(contentsNbt.getCompoundOrEmpty(INVENTORY_TAG)),
					deserializePartitionerData(contentsNbt.getCompoundOrEmpty(PARTITIONER_TAG)),
					deserializeUpgradeData(contentsNbt.getCompoundOrEmpty(UPGRADE_INVENTORY_TAG)),
					deserializeSettingsData(contentsNbt.getCompoundOrEmpty(SETTINGS_TAG))
			);
		}

		public static SettingsData deserializeSettingsData(CompoundTag settingsNbt) {
			Map<String, ISettingsCategoryData<?>> categories = new HashMap<>();
			settingsNbt.getCompound(NoSortSettingsCategory.NAME).ifPresent(categoryNbt ->
					categories.put(NoSortSettingsCategory.NAME, deserializeNoSort(categoryNbt))
			);
			settingsNbt.getCompound(MemorySettingsCategory.NAME).ifPresent(categoryNbt ->
					categories.put(MemorySettingsCategory.NAME, deserializeMemory(categoryNbt))
			);
			settingsNbt.getCompound(ItemDisplaySettingsCategory.NAME).ifPresent(categoryNbt -> {
				categories.put(ItemDisplaySettingsCategory.NAME, deserializeItemDisplay(categoryNbt));
			});

			return new SettingsData(categories, Context.PLAYER, "");
		}

		private static ItemDisplaySettingsCategoryData deserializeItemDisplay(CompoundTag categoryNbt) {
			List<Integer> slotIndexes = NBTHelper.getIntArray(categoryNbt, SLOTS_TAG).map(arr -> Arrays.stream(arr).boxed().collect(Collectors.toCollection(ArrayList::new))).orElseGet(ArrayList::new);
			Map<Integer, Integer> slotRotations = NBTHelper.getMap(categoryNbt, ROTATIONS_TAG, Integer::valueOf, (k, v) -> v.asInt()).orElseGet(HashMap::new);
			DyeColor color = NBTHelper.getInt(categoryNbt, COLOR_TAG).map(DyeColor::byId).orElse(DyeColor.RED);
			DisplaySide displaySide = NBTHelper.getEnumConstant(categoryNbt, DISPLAY_SIDE_TAG, DisplaySide::fromName).orElse(DisplaySide.FRONT);
			return new ItemDisplaySettingsCategoryData(color, slotIndexes, slotRotations, displaySide);
		}

		private static NoSortSettingsCategoryData deserializeNoSort(CompoundTag categoryNbt) {
			Set<Integer> selectedSlots = new HashSet<>();
			categoryNbt.getIntArray(SELECTED_SLOTS_TAG).ifPresent(slotNumbers -> {
				for (int slotNumber : slotNumbers) {
					selectedSlots.add(slotNumber);
				}
			});
			DyeColor color = NBTHelper.getInt(categoryNbt, COLOR_TAG).map(DyeColor::byId).orElse(DyeColor.LIME);
			return new NoSortSettingsCategoryData(selectedSlots, color);
		}

		private static MemorySettingsCategoryData deserializeMemory(CompoundTag categoryNbt) {
			Map<Integer, Item> slotFilterItems = NBTHelper.getMap(categoryNbt, SLOT_FILTER_ITEMS_TAG,
					Integer::valueOf,
					(k, v) -> BuiltInRegistries.ITEM.getOptional(v.asString().map(ResourceLocation::parse).orElse(null))).orElseGet(HashMap::new);

			Map<Integer, ItemStackKey> slotFilterStacks = NBTHelper.getMap(categoryNbt, SLOT_FILTER_STACKS_TAG,
					Integer::valueOf,
					(k, v) -> v instanceof CompoundTag tag ? NBTHelper.deserializeStackFromTag(tag).map(ItemStackKey::of) : Optional.empty()).orElseGet(HashMap::new);
			boolean ignoreNbt = NBTHelper.getBoolean(categoryNbt, IGNORE_NBT_TAG).orElse(true);
			return new MemorySettingsCategoryData(slotFilterItems, slotFilterStacks, ignoreNbt);
		}

		private static UpgradeData deserializeUpgradeData(CompoundTag upgradeInventoryNbt) {
			return RegistryHelper.getRegistryAccess().map(registryAccess -> {
				ValueInput input = ValueIOHelper.inputFromCompoundTag(registryAccess, upgradeInventoryNbt);
				int size = input.getIntOr("Size", 0);
				NonNullList<ItemStack> stacks = NonNullList.withSize(size, ItemStack.EMPTY);
				input.listOrEmpty("Items", ItemStackWithSlot.CODEC).forEach((slot) -> {
					if (slot.isValidInContainer(stacks.size())) {
						stacks.set(slot.slot(), slot.stack());
					}
				});
				return new UpgradeData(stacks);
			}).orElse(new UpgradeData());
		}

		private static PartitionerData deserializePartitionerData(CompoundTag partitionerNbt) {
			int[] baseIndexes = partitionerNbt.getIntArray(BASE_INDEXES_TAG).orElse(new int[]{0});
			List<String> partNames = partitionerNbt.getListOrEmpty("inventoryPartNames")
					.stream().map(Tag::asString).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toCollection(ArrayList::new));
			return new PartitionerData(baseIndexes, partNames);
		}

		private static InventoryData deserializeInventoryData(CompoundTag nbt) {
			int size = nbt.getIntOr("Size", 0);
			NonNullList<ItemStack> stacks = NonNullList.withSize(size, ItemStack.EMPTY);
			ListTag tagList = nbt.getListOrEmpty("Items");
			RegistryHelper.getRegistryAccess().ifPresent(registryAccess -> {
				for (int i = 0; i < tagList.size(); i++) {
					tagList.getCompound(i).ifPresent(itemTag -> {
						int slot = itemTag.getIntOr("Slot", 0);
						if (slot >= 0 && slot < stacks.size()) {
							getStackFromNbt(itemTag, registryAccess).ifPresent(stack -> stacks.set(slot, stack));
						}
					});
				}
			});
			return new InventoryData(stacks);
		}

		private static Optional<ItemStack> getStackFromNbt(Tag itemTag, RegistryAccess registryAccess) {
			return CodecHelper.OVERSIZED_ITEM_STACK_CODEC.parse(registryAccess.createSerializationContext(NbtOps.INSTANCE), itemTag)
					.resultOrPartial(itemName -> SophisticatedCore.LOGGER.error("Tried to load invalid item: '{}'", itemName));
		}
	}
}
