package net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;

import java.util.*;

public class ItemDisplaySettingsCategoryData implements ContainerContents.ISettingsCategoryData<ItemDisplaySettingsCategoryData> {
	public static Codec<ItemDisplaySettingsCategoryData> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(DyeColor.CODEC.fieldOf("color").forGetter(data -> data.color),
					Codec.list(Codec.INT).fieldOf("slotIndexes").forGetter(data -> data.slotIndexes),
					Codec.unboundedMap(CodecHelper.STRING_ENCODED_INT, Codec.INT).fieldOf("slotRotations").forGetter(data -> data.slotRotations),
					Codec.unboundedMap(CodecHelper.STRING_ENCODED_INT, Codec.INT).optionalFieldOf("slotZOffsets", Map.of())
							.forGetter(data -> data.slotZOffsets),
					DisplaySide.CODEC.fieldOf("displaySide").forGetter(data -> data.displaySide))
			.apply(instance, ItemDisplaySettingsCategoryData::new));
	public static StreamCodec<RegistryFriendlyByteBuf, ItemDisplaySettingsCategoryData> STREAM_CODEC = StreamCodec.composite(DyeColor.STREAM_CODEC,
			ItemDisplaySettingsCategoryData::color, ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ItemDisplaySettingsCategoryData::slotIndexes,
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.VAR_INT), data -> data.slotRotations,
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.VAR_INT), data -> data.slotZOffsets, DisplaySide.STREAM_CODEC,
			data -> data.displaySide, ItemDisplaySettingsCategoryData::new);

	private DyeColor color = DyeColor.RED;
	private List<Integer> slotIndexes = new LinkedList<>();
	private Map<Integer, Integer> slotRotations = new HashMap<>();
	private Map<Integer, Integer> slotZOffsets = new HashMap<>();
	private DisplaySide displaySide = DisplaySide.FRONT;

	public ItemDisplaySettingsCategoryData() {
	}

	public ItemDisplaySettingsCategoryData(DyeColor color, List<Integer> slotIndexes, Map<Integer, Integer> slotRotations, Map<Integer, Integer> slotZOffsets,
			DisplaySide displaySide) {
		this.color = color;
		this.slotIndexes.addAll(slotIndexes);
		this.slotRotations.putAll(slotRotations);
		this.slotZOffsets.putAll(slotZOffsets);
		this.displaySide = displaySide;
	}

	@Override
	public String id() {
		return ItemDisplaySettingsCategory.NAME;
	}

	@Override
	public ItemDisplaySettingsCategoryData copy() {
		return new ItemDisplaySettingsCategoryData(color, new LinkedList<>(slotIndexes), new HashMap<>(slotRotations), new HashMap<>(slotZOffsets),
				displaySide);
	}

	@Override
	public void reloadFrom(ItemDisplaySettingsCategoryData other) {
		this.color = other.color;
		this.slotIndexes = new LinkedList<>(other.slotIndexes);
		this.slotRotations = new HashMap<>(other.slotRotations);
		this.slotZOffsets = new HashMap<>(other.slotZOffsets);
		this.displaySide = other.displaySide;
	}

	public DyeColor color() {
		return color;
	}

	public List<Integer> slotIndexes() {
		return slotIndexes;
	}

	public void removeSlotIndex(int slotIndex) {
		slotIndexes.removeIf(index -> index == slotIndex);
		slotRotations.remove(slotIndex);
		slotZOffsets.remove(slotIndex);
	}

	public Map<Integer, Integer> slotRotations() {
		return slotRotations;
	}

	public Map<Integer, Integer> slotZOffsets() {
		return slotZOffsets;
	}

	public DisplaySide displaySide() {
		return displaySide;
	}

	public void setColor(DyeColor color) {
		this.color = color;
	}

	public void setRotation(int slotIndex, int rotation) {
		slotRotations.put(slotIndex, rotation);
	}

	public void setZOffset(int slotIndex, int zOffset) {
		if (zOffset == 0) {
			slotZOffsets.remove(slotIndex);
		} else {
			slotZOffsets.put(slotIndex, zOffset);
		}
	}

	public void setDisplaySide(DisplaySide displaySide) {
		this.displaySide = displaySide;
	}

	public void addSlot(int slotIndex) {
		slotIndexes.add(slotIndex);
	}

	public void removeSlot(int slotIndex) {
		slotIndexes.removeIf(slot -> slot >= slotIndex);
		slotRotations.keySet().removeIf(slot -> slot >= slotIndex);
		slotZOffsets.keySet().removeIf(slot -> slot >= slotIndex);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ItemDisplaySettingsCategoryData that))
			return false;
		return color == that.color && Objects.equals(slotIndexes, that.slotIndexes) && Objects.equals(slotRotations, that.slotRotations)
				&& Objects.equals(slotZOffsets, that.slotZOffsets) && displaySide == that.displaySide;
	}

	@Override
	public int hashCode() {
		return Objects.hash(color, slotIndexes, slotRotations, slotZOffsets, displaySide);
	}
}
