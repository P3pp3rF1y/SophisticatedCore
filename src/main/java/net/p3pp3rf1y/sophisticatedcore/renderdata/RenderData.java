
package net.p3pp3rf1y.sophisticatedcore.renderdata;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class RenderData {
	private static final Map<String, UpgradeClientDataType<?>> CLIENT_DATA_TYPES = new LinkedHashMap<>();
	public static final RenderData EMPTY = new RenderData();

	static {
		register(CookingUpgradeClientData.TYPE);
		register(JukeboxUpgradeClientData.TYPE);
	}

	public static <T extends IUpgradeClientData> void register(UpgradeClientDataType<T> type) {
		CLIENT_DATA_TYPES.put(type.getName(), type);
	}

	private static final Codec<Map<UpgradeClientDataType<?>, IUpgradeClientData>> UPGRADE_DATA_CODEC =
			Codec.<UpgradeClientDataType<?>, IUpgradeClientData>dispatchedMap(
					Codec.STRING.xmap(CLIENT_DATA_TYPES::get, UpgradeClientDataType::getName),
					UpgradeClientDataType::codec
			).xmap(RenderData::copyUpgradeData, RenderData::copyUpgradeData);

	private static final StreamCodec<RegistryFriendlyByteBuf, Map<UpgradeClientDataType<?>, IUpgradeClientData>> UPGRADE_DATA_STREAM_CODEC =
			StreamCodecHelper.ofMap(
					ByteBufCodecs.STRING_UTF8.map(CLIENT_DATA_TYPES::get, UpgradeClientDataType::getName),
					(UpgradeClientDataType<?> type) -> new StreamCodec<RegistryFriendlyByteBuf, IUpgradeClientData>() {
						@Override
						public IUpgradeClientData decode(RegistryFriendlyByteBuf buf) {
							//noinspection unchecked
							return ((StreamCodec<? super ByteBuf, ? extends IUpgradeClientData>) type.streamCodec()).decode(buf);
						}

						@Override
						public void encode(RegistryFriendlyByteBuf buf, IUpgradeClientData value) {
							@SuppressWarnings("unchecked")
							UpgradeClientDataType<IUpgradeClientData> typed = (UpgradeClientDataType<IUpgradeClientData>) type;
							IUpgradeClientData casted = typed.cast(value).orElseThrow();
							//noinspection unchecked
							((StreamCodec<? super ByteBuf, IUpgradeClientData>) typed.streamCodec()).encode(buf, casted);
						}
					},
					LinkedHashMap::new
			);

	private static final Codec<RenderData> CURRENT_STRUCTURE_CODEC = RecordCodecBuilder.create(inst -> inst.group(
			ItemStackTemplate.CODEC.listOf().xmap(RenderData::copyItemTemplates, Function.identity()).fieldOf("upgradeItems").forGetter(RenderData::upgradeItems),
			UPGRADE_DATA_CODEC.fieldOf("upgradeData").forGetter(RenderData::upgradeData),
			Codec.unboundedMap(TankPosition.CODEC, TankRenderData.CODEC).xmap(RenderData::copyTankData, RenderData::copyTankData).fieldOf("tanks").forGetter(RenderData::tanks),
			BatteryRenderData.CODEC.optionalFieldOf("battery").forGetter(RenderData::battery),
			DisplayData.CODEC.fieldOf("display").forGetter(RenderData::display)
	).apply(inst, RenderData::new));

	//TODO remove legacy current structure codec in 26.2
	private static final Codec<RenderData> LEGACY_CURRENT_CODEC = Codec.either(CURRENT_STRUCTURE_CODEC, LegacyCurrentStructure.CODEC)
			.xmap(either -> either.map(Function.identity(), LegacyCurrentStructure::toRenderData), Either::left);

	public static final Codec<RenderData> CODEC = Codec.withAlternative(LEGACY_CURRENT_CODEC, CompoundTag.CODEC, LegacyDeserialization::legacyDeserialize);

	public static final StreamCodec<RegistryFriendlyByteBuf, RenderData> STREAM_CODEC = StreamCodec.composite(
			ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()), RenderData::upgradeItems,
			UPGRADE_DATA_STREAM_CODEC, RenderData::upgradeData,
			StreamCodecHelper.ofMap(TankPosition.STREAM_CODEC, TankRenderData.STREAM_CODEC, LinkedHashMap::new), RenderData::tanks,
			ByteBufCodecs.optional(BatteryRenderData.STREAM_CODEC), RenderData::battery,
			DisplayData.STREAM_CODEC, RenderData::display,
			RenderData::new
	);

	private final List<ItemStackTemplate> upgradeItems;
	private final Map<UpgradeClientDataType<?>, IUpgradeClientData> upgradeData;
	private final Map<TankPosition, TankRenderData> tanks;
	private final Optional<BatteryRenderData> battery;
	private final DisplayData display;

	public RenderData() {
		this(List.of(), Map.of(), Map.of(), Optional.empty(), DisplayData.EMPTY);
	}

	public RenderData(List<ItemStackTemplate> upgradeItems,
					  Map<UpgradeClientDataType<?>, IUpgradeClientData> upgradeData,
					  Map<TankPosition, TankRenderData> tanks,
					  Optional<BatteryRenderData> battery,
					  DisplayData display) {
		this.upgradeItems = copyItemTemplates(upgradeItems);
		this.upgradeData = copyUpgradeData(upgradeData);
		this.tanks = copyTankData(tanks);
		this.battery = battery.map(BatteryRenderData::copy);
		this.display = display.copy();
	}

	public RenderData withUpgradeItems(List<ItemStack> upgradeItems) {
		return new RenderData(upgradeItems.stream().filter(stack -> !stack.isEmpty()).map(ItemStackTemplate::fromNonEmptyStack).toList(), upgradeData, tanks, battery, display);
	}

	public <T extends IUpgradeClientData> RenderData withUpgradeClientData(UpgradeClientDataType<T> upgradeClientDataType, T clientData) {
		Map<UpgradeClientDataType<?>, IUpgradeClientData> updated = new LinkedHashMap<>(upgradeData);
		updated.put(upgradeClientDataType, clientData.copy());
		return new RenderData(upgradeItems, updated, tanks, battery, display);
	}

	public RenderData withoutUpgradeData(UpgradeClientDataType<?> type) {
		if (!upgradeData.containsKey(type)) {
			return this;
		}
		Map<UpgradeClientDataType<?>, IUpgradeClientData> updated = new LinkedHashMap<>(upgradeData);
		updated.remove(type);
		return new RenderData(upgradeItems, updated, tanks, battery, display);
	}

	public RenderData withoutAllUpgradeData() {
		return upgradeData.isEmpty() ? this : new RenderData(upgradeItems, Map.of(), tanks, battery, display);
	}

	public RenderData validated(IStorageWrapper storageWrapper, Level level) {
		Map<UpgradeClientDataType<?>, IUpgradeClientData> validated = new LinkedHashMap<>();
		boolean changed = false;
		for (Map.Entry<UpgradeClientDataType<?>, IUpgradeClientData> entry : upgradeData.entrySet()) {
			if (isUpgradeDataValid(storageWrapper, level, entry.getKey(), entry.getValue())) {
				validated.put(entry.getKey(), entry.getValue().copy());
			} else {
				changed = true;
			}
		}
		return changed ? new RenderData(upgradeItems, validated, tanks, battery, display) : this;
	}

	private static <T extends IUpgradeClientData> boolean isUpgradeDataValid(IStorageWrapper storageWrapper, Level level, UpgradeClientDataType<?> type, IUpgradeClientData data) {
		//noinspection unchecked
		UpgradeClientDataType<T> typed = (UpgradeClientDataType<T>) type;
		return UpgradeRenderDataValidatorRegistry.getValidator(typed)
				.map(validator -> validator.isValid(storageWrapper, level, typed.cast(data).orElseThrow()))
				.orElse(true);
	}

	public RenderData withoutUpgradeRenderInfo() {
		return (tanks.isEmpty() && battery.isEmpty()) ? this : new RenderData(upgradeItems, upgradeData, Map.of(), Optional.empty(), display);
	}

	public RenderData withBattery(@Nullable BatteryRenderData data) {
		Optional<BatteryRenderData> updatedBattery = Optional.ofNullable(data).map(BatteryRenderData::copy);
		return Objects.equals(battery, updatedBattery) ? this : new RenderData(upgradeItems, upgradeData, tanks, updatedBattery, display);
	}

	public RenderData withTank(TankPosition tankPosition, TankRenderData data) {
		Map<TankPosition, TankRenderData> updated = new LinkedHashMap<>(tanks);
		updated.put(tankPosition, data.copy());
		return new RenderData(upgradeItems, upgradeData, updated, battery, display);
	}

	public RenderData withDisplayData(List<DisplayItemData> displayItems, List<Integer> inaccessibleSlots, List<Integer> infiniteSlots, List<Integer> slotCounts, List<Float> slotFillRatios) {
		return new RenderData(upgradeItems, upgradeData, tanks, battery, new DisplayData(displayItems, inaccessibleSlots, infiniteSlots, slotCounts, slotFillRatios));
	}

	public RenderData withDisplayItemsAndInaccessibleSlots(List<DisplayItemData> displayItems, List<Integer> inaccessibleSlots) {
		return new RenderData(upgradeItems, upgradeData, tanks, battery, display.withDisplayItemsAndInaccessibleSlots(displayItems, inaccessibleSlots));
	}

	public RenderData withSlotCountsFillRatiosAndInfiniteSlots(List<Integer> slotCounts, List<Float> slotFillRatios, List<Integer> infiniteSlots) {
		return new RenderData(upgradeItems, upgradeData, tanks, battery, display.withSlotCountsFillRatiosAndInfiniteSlots(infiniteSlots, slotCounts, slotFillRatios));
	}

	public List<ItemStackTemplate> upgradeItems() {
		return upgradeItems;
	}

	public List<ItemStack> getUpgradeItemStacks() {
		return upgradeItems.stream().map(ItemStackTemplate::create).toList();
	}

	public Map<UpgradeClientDataType<?>, IUpgradeClientData> upgradeData() {
		return upgradeData;
	}

	public Map<TankPosition, TankRenderData> tanks() {
		return tanks;
	}

	public Optional<BatteryRenderData> battery() {
		return battery;
	}

	public DisplayData display() {
		return display;
	}

	public RenderData copy() {
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof RenderData that)) {
			return false;
		}
		return Objects.equals(upgradeItems, that.upgradeItems)
				&& Objects.equals(upgradeData, that.upgradeData)
				&& Objects.equals(tanks, that.tanks)
				&& Objects.equals(battery, that.battery)
				&& Objects.equals(display, that.display);
	}

	@Override
	public int hashCode() {
		return Objects.hash(upgradeItems, upgradeData, tanks, battery, display);
	}

	@Override
	public String toString() {
		return "RenderData["
				+ "upgradeItems=" + upgradeItems + ", "
				+ "upgradeData=" + upgradeData + ", "
				+ "tanks=" + tanks + ", "
				+ "battery=" + battery + ", "
				+ "display=" + display + ']';
	}

	private static List<ItemStackTemplate> copyItemTemplates(List<ItemStackTemplate> upgradeItems) {
		return List.copyOf(upgradeItems.stream().filter(Objects::nonNull).toList());
	}

	private static Map<UpgradeClientDataType<?>, IUpgradeClientData> copyUpgradeData(Map<UpgradeClientDataType<?>, IUpgradeClientData> upgradeData) {
		LinkedHashMap<UpgradeClientDataType<?>, IUpgradeClientData> copied = new LinkedHashMap<>();
		upgradeData.forEach((type, data) -> copied.put(type, data.copy()));
		return Collections.unmodifiableMap(copied);
	}

	private static Map<TankPosition, TankRenderData> copyTankData(Map<TankPosition, TankRenderData> tanks) {
		LinkedHashMap<TankPosition, TankRenderData> copied = new LinkedHashMap<>();
		tanks.forEach((position, data) -> copied.put(position, data.copy()));
		return Collections.unmodifiableMap(copied);
	}

	public record DisplayData(List<DisplayItemData> displayItems, List<Integer> inaccessibleSlots,
							  List<Integer> infiniteSlots, List<Integer> slotCounts, List<Float> slotFillRatios) {
		public static final DisplayData EMPTY = new DisplayData(List.of(), List.of(), List.of(), List.of(), List.of());

		public static final Codec<DisplayData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				DisplayItemData.CODEC.listOf().fieldOf("displayItems").forGetter(DisplayData::displayItems),
				Codec.INT.listOf().fieldOf("inaccessibleSlots").forGetter(DisplayData::inaccessibleSlots),
				Codec.INT.listOf().fieldOf("infiniteSlots").forGetter(DisplayData::infiniteSlots),
				Codec.INT.listOf().fieldOf("slotCounts").forGetter(DisplayData::slotCounts),
				Codec.FLOAT.listOf().fieldOf("slotFillRatios").forGetter(DisplayData::slotFillRatios)
		).apply(inst, DisplayData::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, DisplayData> STREAM_CODEC = StreamCodec.composite(
				DisplayItemData.STREAM_CODEC.apply(ByteBufCodecs.list()), DisplayData::displayItems,
				ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), DisplayData::inaccessibleSlots,
				ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), DisplayData::infiniteSlots,
				ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), DisplayData::slotCounts,
				ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()), DisplayData::slotFillRatios,
				DisplayData::new
		);

		public DisplayData {
			displayItems = List.copyOf(displayItems.stream().map(DisplayItemData::copy).toList());
			inaccessibleSlots = List.copyOf(inaccessibleSlots);
			infiniteSlots = List.copyOf(infiniteSlots);
			slotCounts = List.copyOf(slotCounts);
			slotFillRatios = List.copyOf(slotFillRatios);
		}

		public DisplayData withDisplayItemsAndInaccessibleSlots(List<DisplayItemData> displayItems, List<Integer> inaccessibleSlots) {
			return new DisplayData(displayItems, inaccessibleSlots, infiniteSlots, slotCounts, slotFillRatios);
		}

		public DisplayData withSlotCountsFillRatiosAndInfiniteSlots(List<Integer> infiniteSlots, List<Integer> slotCounts, List<Float> slotFillRatios) {
			return new DisplayData(displayItems, inaccessibleSlots, infiniteSlots, slotCounts, slotFillRatios);
		}

		public DisplayData copy() {
			return this;
		}
	}

	public record TankRenderData(@Nullable FluidStackTemplate fluidStack, float fillRatio) {
		public static final Codec<TankRenderData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				FluidStackTemplate.CODEC.optionalFieldOf("fluid").forGetter(data -> Optional.ofNullable(data.fluidStack())),
				Codec.FLOAT.fieldOf("fillRatio").forGetter(TankRenderData::fillRatio)
		).apply(inst, (fluid, ratio) -> new TankRenderData(fluid.orElse(null), ratio)));

		public static final StreamCodec<RegistryFriendlyByteBuf, TankRenderData> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.optional(FluidStackTemplate.STREAM_CODEC), data -> Optional.ofNullable(data.fluidStack()),
				ByteBufCodecs.FLOAT, TankRenderData::fillRatio,
				(fluid, ratio) -> new TankRenderData(fluid.orElse(null), ratio)
		);

		public TankRenderData(FluidStack fluidStack, float fillRatio) {
			this(fluidStack.isEmpty() ? null : FluidStackTemplate.fromNonEmptyStack(fluidStack), fillRatio);
		}

		public TankRenderData {
			fillRatio = Math.max(0f, Math.min(1f, fillRatio));
		}

		public TankRenderData copy() {
			return this;
		}

		public Optional<FluidStack> getFluid() {
			return Optional.ofNullable(fluidStack).map(FluidStackTemplate::create);
		}

		public Optional<FluidStackTemplate> fluidTemplate() {
			return Optional.ofNullable(fluidStack);
		}
	}

	public record BatteryRenderData(float chargeRatio) {
		public static final Codec<BatteryRenderData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.FLOAT.fieldOf("chargeRatio").forGetter(BatteryRenderData::chargeRatio)
		).apply(inst, BatteryRenderData::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, BatteryRenderData> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.FLOAT, BatteryRenderData::chargeRatio,
				BatteryRenderData::new
		);

		public BatteryRenderData copy() {
			return this;
		}
	}

	public record DisplayItemData(@Nullable ItemStackTemplate item, int rotation, int slotIndex, DisplaySide displaySide) {
		public static final Codec<DisplayItemData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ItemStackTemplate.CODEC.optionalFieldOf("item").forGetter(data -> Optional.ofNullable(data.item())),
				Codec.INT.fieldOf("rotation").forGetter(DisplayItemData::rotation),
				Codec.INT.fieldOf("slotIndex").forGetter(DisplayItemData::slotIndex),
				DisplaySide.CODEC.fieldOf("displaySide").forGetter(DisplayItemData::displaySide)
		).apply(instance, (item, rotation, slotIndex, displaySide) -> new DisplayItemData(item.orElse(null), rotation, slotIndex, displaySide)));

		public static final StreamCodec<RegistryFriendlyByteBuf, DisplayItemData> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), data -> Optional.ofNullable(data.item()),
				ByteBufCodecs.VAR_INT, DisplayItemData::rotation,
				ByteBufCodecs.VAR_INT, DisplayItemData::slotIndex,
				DisplaySide.STREAM_CODEC, DisplayItemData::displaySide,
				(item, rotation, slotIndex, displaySide) -> new DisplayItemData(item.orElse(null), rotation, slotIndex, displaySide)
		);

		public DisplayItemData(ItemStack item, int rotation, int slotIndex, DisplaySide displaySide) {
			this(item.isEmpty() ? null : ItemStackTemplate.fromNonEmptyStack(item), rotation, slotIndex, displaySide);
		}

		public DisplayItemData copy() {
			return this;
		}

		public ItemStack createItemStack() {
			return item == null ? ItemStack.EMPTY : item.create();
		}
	}

	private record LegacyCurrentStructure(List<ItemStack> upgradeItems,
									Map<UpgradeClientDataType<?>, IUpgradeClientData> upgradeData,
									Map<TankPosition, LegacyTankRenderData> tanks,
									Optional<BatteryRenderData> battery,
									LegacyDisplayData display) {
		private static final Codec<LegacyCurrentStructure> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				ItemStack.OPTIONAL_CODEC.listOf().fieldOf("upgradeItems").forGetter(LegacyCurrentStructure::upgradeItems),
				UPGRADE_DATA_CODEC.fieldOf("upgradeData").forGetter(LegacyCurrentStructure::upgradeData),
				Codec.unboundedMap(TankPosition.CODEC, LegacyTankRenderData.CODEC).fieldOf("tanks").forGetter(LegacyCurrentStructure::tanks),
				BatteryRenderData.CODEC.optionalFieldOf("battery").forGetter(LegacyCurrentStructure::battery),
				LegacyDisplayData.CODEC.fieldOf("display").forGetter(LegacyCurrentStructure::display)
		).apply(inst, LegacyCurrentStructure::new));

		private RenderData toRenderData() {
			return new RenderData(
					upgradeItems.stream().filter(stack -> !stack.isEmpty()).map(ItemStackTemplate::fromNonEmptyStack).toList(),
					upgradeData,
					tanks.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toCurrent(), (a, b) -> b, LinkedHashMap::new)),
					battery,
					display.toCurrent()
			);
		}
	}

	private record LegacyDisplayData(List<LegacyDisplayItemData> displayItems, List<Integer> inaccessibleSlots,
									 List<Integer> infiniteSlots, List<Integer> slotCounts, List<Float> slotFillRatios) {
		private static final Codec<LegacyDisplayData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				LegacyDisplayItemData.CODEC.listOf().fieldOf("displayItems").forGetter(LegacyDisplayData::displayItems),
				Codec.INT.listOf().fieldOf("inaccessibleSlots").forGetter(LegacyDisplayData::inaccessibleSlots),
				Codec.INT.listOf().fieldOf("infiniteSlots").forGetter(LegacyDisplayData::infiniteSlots),
				Codec.INT.listOf().fieldOf("slotCounts").forGetter(LegacyDisplayData::slotCounts),
				Codec.FLOAT.listOf().fieldOf("slotFillRatios").forGetter(LegacyDisplayData::slotFillRatios)
		).apply(inst, LegacyDisplayData::new));

		private DisplayData toCurrent() {
			return new DisplayData(displayItems.stream().map(LegacyDisplayItemData::toCurrent).toList(), inaccessibleSlots, infiniteSlots, slotCounts, slotFillRatios);
		}
	}

	private record LegacyDisplayItemData(ItemStack item, int rotation, int slotIndex, DisplaySide displaySide) {
		private static final Codec<LegacyDisplayItemData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ItemStack.OPTIONAL_CODEC.orElse(ItemStack.EMPTY).fieldOf("item").forGetter(LegacyDisplayItemData::item),
				Codec.INT.fieldOf("rotation").forGetter(LegacyDisplayItemData::rotation),
				Codec.INT.fieldOf("slotIndex").forGetter(LegacyDisplayItemData::slotIndex),
				DisplaySide.CODEC.fieldOf("displaySide").forGetter(LegacyDisplayItemData::displaySide)
		).apply(instance, LegacyDisplayItemData::new));

		private DisplayItemData toCurrent() {
			return new DisplayItemData(item, rotation, slotIndex, displaySide);
		}
	}

	private record LegacyTankRenderData(FluidStack fluidStack, float fillRatio) {
		private static final Codec<LegacyTankRenderData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				FluidStack.CODEC.optionalFieldOf("fluid", FluidStack.EMPTY).forGetter(LegacyTankRenderData::fluidStack),
				Codec.FLOAT.fieldOf("fillRatio").forGetter(LegacyTankRenderData::fillRatio)
		).apply(inst, LegacyTankRenderData::new));

		private TankRenderData toCurrent() {
			return new TankRenderData(fluidStack, fillRatio);
		}
	}

	//TODO remove legacy deserialization in 26.2
	private static class LegacyDeserialization {
		private static final String TANKS_TAG = "tanks";
		private static final String BATTERY_TAG = "battery";
		private static final String TANK_POSITION_TAG = "position";
		private static final String TANK_INFO_TAG = "info";
		private static final String ITEM_DISPLAY_TAG = "itemDisplay";
		private static final String UPGRADES_TAG = "upgrades";
		private static final String UPGRADE_ITEMS_TAG = "upgradeItems";
		private static final Map<UpgradeClientDataType<?>, Function<CompoundTag, IUpgradeClientData>> LEGACY_CLIENT_DATA_TYPES;
		private static final String CHARGE_RATIO_TAG = "chargeRatio";
		private static final String ITEMS_TAG = "items";
		private static final String INACCESSIBLE_SLOTS_TAG = "inaccessibleSlots";
		private static final String INFINITE_SLOTS_TAG = "infiniteSlots";
		public static final String SLOT_COUNTS_TAG = "slotCounts";
		public static final String SLOT_FILL_RATIOS_TAG = "slotFillRatios";
		private static final String FLUID_TAG = "fluid";
		private static final String FILL_RATIO_TAG = "fillRatio";
		private static final String ITEM_TAG = "item";
		private static final String ROTATION_TAG = "rotation";
		private static final String SLOT_INDEX_TAG = "slotIndex";
		private static final String DISPLAY_SIDE_TAG = "displaySide";

		static {
			LEGACY_CLIENT_DATA_TYPES = Map.of(
					CookingUpgradeClientData.TYPE, nbt -> new CookingUpgradeClientData(nbt.getBooleanOr("burning", false)),
					JukeboxUpgradeClientData.TYPE, nbt -> new JukeboxUpgradeClientData(nbt.getBooleanOr("playing", false))
			);
		}

		private static RenderData legacyDeserialize(CompoundTag renderInfoTag) {
			DisplayData itemDisplayData = legacyDeserializeItemDisplay(renderInfoTag.getCompoundOrEmpty(ITEM_DISPLAY_TAG));
			ListTag upgradeItemsTag = renderInfoTag.getListOrEmpty(UPGRADE_ITEMS_TAG);
			List<ItemStackTemplate> upgradeItems = new ArrayList<>();
			RegistryHelper.getRegistryAccess().ifPresent(registryAccess -> {
				for (int i = 0; i < upgradeItemsTag.size(); i++) {
					NBTHelper.deserializeStackFromTag(upgradeItemsTag.getCompoundOrEmpty(i)).filter(stack -> !stack.isEmpty()).map(ItemStackTemplate::fromNonEmptyStack).ifPresent(upgradeItems::add);
				}
			});
			CompoundTag upgrades = renderInfoTag.getCompoundOrEmpty(UPGRADES_TAG);
			Map<UpgradeClientDataType<?>, IUpgradeClientData> clientData = new LinkedHashMap<>();
			upgrades.keySet().forEach(key -> {
				LEGACY_CLIENT_DATA_TYPES.entrySet().stream().filter(entry -> entry.getKey().getName().equals(key)).findFirst().ifPresent(entry -> {
					IUpgradeClientData data = entry.getValue().apply(upgrades.getCompoundOrEmpty(key));
					clientData.put(entry.getKey(), data);
				});
			});
			Map<TankPosition, TankRenderData> tanks = legacyDeserializeTanks(renderInfoTag);
			Optional<BatteryRenderData> battery = NBTHelper.getCompound(renderInfoTag, BATTERY_TAG).map(tag -> new BatteryRenderData(tag.getFloatOr(CHARGE_RATIO_TAG, 0)));
			return new RenderData(upgradeItems, clientData, tanks, battery, itemDisplayData);
		}

		private static Map<TankPosition, TankRenderData> legacyDeserializeTanks(CompoundTag renderInfoTag) {
			Map<TankPosition, TankRenderData> tankData = new LinkedHashMap<>();
			ListTag tanks = renderInfoTag.getListOrEmpty(TANKS_TAG);
			for (int i = 0; i < tanks.size(); i++) {
				CompoundTag tank = tanks.getCompoundOrEmpty(i);
				tankData.put(
						tank.getString(TANK_POSITION_TAG).map(s -> TankPosition.valueOf(s.toUpperCase(Locale.ROOT))).orElse(TankPosition.LEFT),
						legacyDeserializeTank(tank.getCompoundOrEmpty(TANK_INFO_TAG))
				);
			}
			return tankData;
		}

		public static TankRenderData legacyDeserializeTank(CompoundTag tag) {
			if (tag.contains(FLUID_TAG)) {
				FluidStack fluidStack = NBTHelper.deserializeFluidFromTag(tag.getCompoundOrEmpty(FLUID_TAG)).orElse(FluidStack.EMPTY);
				if (!fluidStack.isEmpty()) {
					return new TankRenderData(fluidStack, tag.getFloatOr(FILL_RATIO_TAG, 0));
				}
			}

			return new TankRenderData((FluidStackTemplate) null, tag.getFloatOr(FILL_RATIO_TAG, 0));
		}

		public static DisplayData legacyDeserializeItemDisplay(CompoundTag tag) {
			List<Integer> inaccessibleSlots = tag.getIntArray(INACCESSIBLE_SLOTS_TAG).<List<Integer>>map(array -> Arrays.stream(array).boxed().collect(Collectors.toCollection(ArrayList::new))).orElse(Collections.emptyList());
			List<Integer> infiniteSlots = tag.getIntArray(INFINITE_SLOTS_TAG).<List<Integer>>map(array -> Arrays.stream(array).boxed().collect(Collectors.toCollection(ArrayList::new))).orElse(Collections.emptyList());
			List<Integer> slotCounts = tag.getIntArray(SLOT_COUNTS_TAG).<List<Integer>>map(array -> Arrays.stream(array).boxed().collect(Collectors.toCollection(ArrayList::new))).orElse(Collections.emptyList());
			List<Float> slotFillRatios = NBTHelper.getCollection(tag, SLOT_FILL_RATIOS_TAG, Tag::asFloat, ArrayList::new).orElseGet(ArrayList::new);
			if (tag.contains(ITEM_TAG)) {
				return new DisplayData(List.of(legacyDeserializeDisplayItem(tag)), inaccessibleSlots, infiniteSlots, slotCounts, slotFillRatios);
			} else if (tag.contains(ITEMS_TAG)) {
				List<DisplayItemData> items = NBTHelper.getCollection(tag, ITEMS_TAG, stackTag -> Optional.of(legacyDeserializeDisplayItem((CompoundTag) stackTag)), ArrayList::new).orElseGet(ArrayList::new);
				return new DisplayData(items, inaccessibleSlots, infiniteSlots, slotCounts, slotFillRatios);
			}
			return new DisplayData(Collections.emptyList(), inaccessibleSlots, infiniteSlots, slotCounts, slotFillRatios);
		}

		private static DisplayItemData legacyDeserializeDisplayItem(CompoundTag tag) {
			return new DisplayItemData(
					tag.getCompound(ITEM_TAG).flatMap(NBTHelper::deserializeStackFromTag).orElse(ItemStack.EMPTY),
					tag.getIntOr(ROTATION_TAG, 0),
					tag.getIntOr(SLOT_INDEX_TAG, 0),
					tag.getString(DISPLAY_SIDE_TAG).map(DisplaySide::fromName).orElse(DisplaySide.FRONT)
			);
		}
	}
}
