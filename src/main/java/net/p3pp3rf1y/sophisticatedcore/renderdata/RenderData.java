
package net.p3pp3rf1y.sophisticatedcore.renderdata;

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
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class RenderData {
	private static final Map<String, UpgradeClientDataType<?>> CLIENT_DATA_TYPES = new HashMap<>();
	public static final RenderData EMPTY = new RenderData();

	static {
		register(CookingUpgradeClientData.TYPE);
		register(JukeboxUpgradeClientData.TYPE);
	}

	public static <T extends IUpgradeClientData> void register(UpgradeClientDataType<T> type) {
		CLIENT_DATA_TYPES.put(type.getName(), type);
	}

	public static final Codec<RenderData> CODEC =
			Codec.withAlternative(
					RecordCodecBuilder.create(inst -> inst.group(
							ItemStack.OPTIONAL_CODEC.listOf().xmap(CodecHelper::toMutable, Function.identity()).fieldOf("upgradeItems").forGetter(RenderData::upgradeItems),
							Codec.<UpgradeClientDataType<?>, IUpgradeClientData>dispatchedMap(Codec.STRING.xmap(CLIENT_DATA_TYPES::get, UpgradeClientDataType::getName), UpgradeClientDataType::codec)
									.xmap(CodecHelper::toMutable, Function.identity()).fieldOf("upgradeData").forGetter(RenderData::upgradeData),
							Codec.unboundedMap(TankPosition.CODEC, TankRenderData.CODEC).fieldOf("tanks").xmap(CodecHelper::toMutable, Function.identity()).forGetter(RenderData::tanks),
							BatteryRenderData.CODEC.optionalFieldOf("battery").forGetter(RenderData::battery),
							DisplayData.CODEC.fieldOf("display").forGetter(RenderData::display)
					).apply(inst, RenderData::new)),
					CompoundTag.CODEC, LegacyDeserialization::legacyDeserialize
			);

	public static final StreamCodec<RegistryFriendlyByteBuf, RenderData> STREAM_CODEC =
			StreamCodec.composite(
					ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), RenderData::upgradeItems,
					StreamCodecHelper.ofMap(ByteBufCodecs.STRING_UTF8.map(CLIENT_DATA_TYPES::get, UpgradeClientDataType::getName),
							(UpgradeClientDataType<?> type) -> new StreamCodec<>() {
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
							}, HashMap::new), RenderData::upgradeData,
					StreamCodecHelper.ofMap(TankPosition.STREAM_CODEC, TankRenderData.STREAM_CODEC, HashMap::new), RenderData::tanks,
					ByteBufCodecs.optional(BatteryRenderData.STREAM_CODEC), RenderData::battery,
					DisplayData.STREAM_CODEC, RenderData::display,
					RenderData::new
			);
	private final List<ItemStack> upgradeItems;
	private final Map<UpgradeClientDataType<?>, IUpgradeClientData> upgradeData;
	private final Map<TankPosition, TankRenderData> tanks;
	private Optional<BatteryRenderData> battery;
	private final DisplayData display;

	public RenderData() {
		this.upgradeItems = new ArrayList<>();
		this.upgradeData = new HashMap<>();
		this.tanks = new HashMap<>();
		this.battery = Optional.empty();
		this.display = new DisplayData();
	}

	public RenderData(List<ItemStack> upgradeItems,
					  Map<UpgradeClientDataType<?>, IUpgradeClientData> upgradeData,
					  Map<TankPosition, TankRenderData> tanks, Optional<BatteryRenderData> battery,
					  DisplayData display) {
		this.upgradeItems = upgradeItems;
		this.upgradeData = upgradeData;
		this.tanks = tanks;
		this.battery = battery;
		this.display = display;
	}

	public void setUpgradeItems(List<ItemStack> upgradeItems) {
		this.upgradeItems.clear();
		this.upgradeItems.addAll(upgradeItems);
	}

	public <T extends IUpgradeClientData> void putUpgradeData(UpgradeClientDataType<T> upgradeClientDataType, T clientData) {
		upgradeData.put(upgradeClientDataType, clientData);
	}

	public void removeUpgradeData(UpgradeClientDataType<?> type) {
		upgradeData.remove(type);
	}

	public void removeAllUpgradeData() {
		upgradeData.clear();
	}

	public void clearTanks() {
		tanks.clear();
	}

	public void clearBattery() {
		battery = Optional.empty();
	}

	public void setBattery(@Nullable BatteryRenderData data) {
		this.battery = Optional.ofNullable(data);
	}

	public List<ItemStack> upgradeItems() {
		return upgradeItems;
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

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (RenderData) obj;
		return Objects.equals(this.upgradeItems, that.upgradeItems) &&
				Objects.equals(this.upgradeData, that.upgradeData) &&
				Objects.equals(this.tanks, that.tanks) &&
				Objects.equals(this.battery, that.battery) &&
				Objects.equals(this.display, that.display);
	}

	@Override
	public int hashCode() {
		return Objects.hash(upgradeItems, upgradeData, tanks, battery, display);
	}

	@Override
	public String toString() {
		return "RenderData[" +
				"upgradeItems=" + upgradeItems + ", " +
				"upgradeData=" + upgradeData + ", " +
				"tanks=" + tanks + ", " +
				"battery=" + battery + ", " +
				"display=" + display + ']';
	}

	public void setTank(TankPosition tankPosition, TankRenderData data) {
		tanks.put(tankPosition, data);
	}

	public RenderData copy() {
		return new RenderData(
				upgradeItems.stream().map(ItemStack::copy).collect(Collectors.toCollection(ArrayList::new)),
				upgradeData.entrySet().stream()
						.map(e -> Map.entry(e.getKey(), e.getValue().copy()))
						.collect(Collectors.toMap(
								Map.Entry::getKey,
								e -> e.getValue().copy(),
								(a, b) -> b,
								HashMap::new
						)),
				tanks.entrySet().stream()
						.map(e -> Map.entry(e.getKey(), e.getValue().copy()))
						.collect(Collectors.toMap(
								Map.Entry::getKey,
								e -> e.getValue().copy(),
								(a, b) -> b,
								HashMap::new
						)),
				battery.map(BatteryRenderData::copy),
				display.copy()
		);
	}


	public record DisplayData(List<DisplayItemData> displayItems, List<Integer> inaccessibleSlots,
							  List<Integer> infiniteSlots, List<Integer> slotCounts, List<Float> slotFillRatios) {
		public static final Codec<DisplayData> CODEC =
				RecordCodecBuilder.create(inst -> inst.group(
								DisplayItemData.CODEC.listOf().xmap(CodecHelper::toMutable, Function.identity()).fieldOf("displayItems").forGetter(DisplayData::displayItems),
								Codec.INT.listOf().xmap(CodecHelper::toMutable, Function.identity()).fieldOf("inaccessibleSlots").forGetter(DisplayData::inaccessibleSlots),
								Codec.INT.listOf().xmap(CodecHelper::toMutable, Function.identity()).fieldOf("infiniteSlots").forGetter(DisplayData::infiniteSlots),
								Codec.INT.listOf().xmap(CodecHelper::toMutable, Function.identity()).fieldOf("slotCounts").forGetter(DisplayData::slotCounts),
								Codec.FLOAT.listOf().xmap(CodecHelper::toMutable, Function.identity()).fieldOf("slotFillRatios").forGetter(DisplayData::slotFillRatios)
						).apply(inst, DisplayData::new)
				);

		public static final StreamCodec<RegistryFriendlyByteBuf, DisplayData> STREAM_CODEC =
				StreamCodec.composite(
						DisplayItemData.STREAM_CODEC.apply(ByteBufCodecs.list()), DisplayData::displayItems,
						ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), DisplayData::inaccessibleSlots,
						ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), DisplayData::infiniteSlots,
						ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), DisplayData::slotCounts,
						ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()), DisplayData::slotFillRatios,
						DisplayData::new
				);

		public DisplayData() {
			this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
		}

		public void refreshData(List<DisplayItemData> displayItems, List<Integer> inaccessibleSlots, List<Integer> infiniteSlots, List<Integer> slotCounts, List<Float> slotFillRatios) {
			this.displayItems.clear();
			this.displayItems.addAll(displayItems);
			this.inaccessibleSlots.clear();
			this.inaccessibleSlots.addAll(inaccessibleSlots);
			this.infiniteSlots.clear();
			this.infiniteSlots.addAll(infiniteSlots);
			this.slotCounts.clear();
			this.slotCounts.addAll(slotCounts);
			this.slotFillRatios.clear();
			this.slotFillRatios.addAll(slotFillRatios);
		}

		public DisplayData copy() {
			return new DisplayData(
					displayItems.stream().map(DisplayItemData::copy).collect(Collectors.toCollection(ArrayList::new)),
					new ArrayList<>(inaccessibleSlots),
					new ArrayList<>(infiniteSlots),
					new ArrayList<>(slotCounts),
					new ArrayList<>(slotFillRatios)
			);
		}

		public void refreshDisplayItemsAndInaccessibleSlots(List<DisplayItemData> displayItems, List<Integer> inaccessibleSlots) {
			this.displayItems.clear();
			this.displayItems.addAll(displayItems);
			this.inaccessibleSlots.clear();
			this.inaccessibleSlots.addAll(inaccessibleSlots);
		}

		public void refreshSlotCountsFillRatiosAndInfiniteSlots(List<Integer> infiniteSlots, List<Integer> slotCounts, List<Float> slotFillRatios) {
			this.infiniteSlots.clear();
			this.infiniteSlots.addAll(infiniteSlots);
			this.slotCounts.clear();
			this.slotCounts.addAll(slotCounts);
			this.slotFillRatios.clear();
			this.slotFillRatios.addAll(slotFillRatios);
		}
	}

	public record TankRenderData(FluidStack fluidStack, float fillRatio) {
		public TankRenderData(FluidStack fluidStack, float fillRatio) {
			this.fluidStack = fluidStack;
			this.fillRatio = Math.max(0f, Math.min(1f, fillRatio));
		}

		public static final Codec<TankRenderData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				FluidStack.CODEC.optionalFieldOf("fluid", FluidStack.EMPTY).forGetter(TankRenderData::fluidStack),
				Codec.FLOAT.fieldOf("fillRatio").forGetter(TankRenderData::fillRatio)
		).apply(inst, TankRenderData::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, TankRenderData> STREAM_CODEC =
				StreamCodec.composite(
						FluidStack.OPTIONAL_STREAM_CODEC, TankRenderData::fluidStack,
						ByteBufCodecs.FLOAT, TankRenderData::fillRatio,
						TankRenderData::new
				);

		public TankRenderData copy() {
			return new TankRenderData(fluidStack.copy(), fillRatio);
		}

		public Optional<FluidStack> getFluid() {
			return fluidStack.isEmpty() ? Optional.empty() : Optional.of(fluidStack);
		}
	}

	public record BatteryRenderData(float chargeRatio) {
		public static final Codec<BatteryRenderData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.FLOAT.fieldOf("chargeRatio").forGetter(BatteryRenderData::chargeRatio)
		).apply(inst, BatteryRenderData::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, BatteryRenderData> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.FLOAT, BatteryRenderData::chargeRatio,
						BatteryRenderData::new
				);

		public BatteryRenderData copy() {
			return new BatteryRenderData(chargeRatio);
		}
	}

	public record DisplayItemData(ItemStack item, int rotation, int slotIndex, DisplaySide displaySide) {
		public static final Codec<DisplayItemData> CODEC = RecordCodecBuilder.create(instance ->
				instance.group(
						ItemStack.OPTIONAL_CODEC.orElse(ItemStack.EMPTY).fieldOf("item").forGetter(DisplayItemData::item),
						Codec.INT.fieldOf("rotation").forGetter(DisplayItemData::rotation),
						Codec.INT.fieldOf("slotIndex").forGetter(DisplayItemData::slotIndex),
						DisplaySide.CODEC.fieldOf("displaySide").forGetter(DisplayItemData::displaySide)
				).apply(instance, DisplayItemData::new)
		);
		public static final StreamCodec<RegistryFriendlyByteBuf, DisplayItemData> STREAM_CODEC =
				StreamCodec.composite(
						ItemStack.OPTIONAL_STREAM_CODEC,
						DisplayItemData::item,
						ByteBufCodecs.VAR_INT,
						DisplayItemData::rotation,
						ByteBufCodecs.VAR_INT,
						DisplayItemData::slotIndex,
						DisplaySide.STREAM_CODEC,
						DisplayItemData::displaySide,
						DisplayItemData::new
				);

		public DisplayItemData copy() {
			return new DisplayItemData(item.copy(), rotation, slotIndex, displaySide);
		}
	}

	//TODO remove legacy deserialization after next major release after 1.21.1
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
			List<ItemStack> upgradeItems = new ArrayList<>();
			RegistryHelper.getRegistryAccess().ifPresent(registryAccess -> {
				for (int i = 0; i < upgradeItemsTag.size(); i++) {
					upgradeItems.add(NBTHelper.deserializeStackFromTag(upgradeItemsTag.getCompoundOrEmpty(i)).orElse(ItemStack.EMPTY));
				}
			});
			CompoundTag upgrades = renderInfoTag.getCompoundOrEmpty(UPGRADES_TAG);
			Map<UpgradeClientDataType<?>, IUpgradeClientData> clientData = new HashMap<>();
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
			Map<TankPosition, TankRenderData> tankData = new HashMap<>();
			ListTag tanks = renderInfoTag.getListOrEmpty(TANKS_TAG);
			for (int i = 0; i < tanks.size(); i++) {
				CompoundTag tank = tanks.getCompoundOrEmpty(i);
				tankData.put(
						tank.getString(TANK_POSITION_TAG).map(s -> TankPosition.valueOf(s.toUpperCase(Locale.ROOT))).orElse(TankPosition.LEFT),
						legacyDeserializeTank(tank.getCompoundOrEmpty(TANK_INFO_TAG)));
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

			return new TankRenderData(FluidStack.EMPTY, tag.getFloatOr(FILL_RATIO_TAG, 0));
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
					tag.getIntOr(ROTATION_TAG, 0), tag.getIntOr(SLOT_INDEX_TAG, 0), tag.getString(DISPLAY_SIDE_TAG).map(DisplaySide::fromName).orElse(DisplaySide.FRONT));
		}
	}
}
