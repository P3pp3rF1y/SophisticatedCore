package net.p3pp3rf1y.sophisticatedcore.settings.nosort;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class NoSortSettingsCategoryData implements ContainerContents.ISettingsCategoryData<NoSortSettingsCategoryData> {
	public static final Codec<NoSortSettingsCategoryData> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					CodecHelper.setOf(Codec.INT).xmap(c -> (Set<Integer>) new HashSet<>(c), Function.identity()).fieldOf("selectedSlots").forGetter(NoSortSettingsCategoryData::selectedSlots),
					DyeColor.CODEC.fieldOf("color").forGetter(NoSortSettingsCategoryData::color)
			).apply(instance, NoSortSettingsCategoryData::new)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, NoSortSettingsCategoryData> STREAM_CODEC = StreamCodec.composite(
			StreamCodecHelper.ofCollection(ByteBufCodecs.VAR_INT, HashSet::new),
			NoSortSettingsCategoryData::selectedSlots,
			DyeColor.STREAM_CODEC,
			NoSortSettingsCategoryData::color,
			NoSortSettingsCategoryData::new
	);

	private Set<Integer> selectedSlots = new HashSet<>();
	private DyeColor color = DyeColor.LIME;

	public NoSortSettingsCategoryData() {}

	public NoSortSettingsCategoryData(Set<Integer> selectedSlots, DyeColor color) {
		this.selectedSlots.addAll(selectedSlots);
		this.color = color;
	}

	public Set<Integer> selectedSlots() {
		return selectedSlots;
	}

	public DyeColor color() {
		return color;
	}

	@Override
	public String id() {
		return NoSortSettingsCategory.NAME;
	}

	@Override
	public NoSortSettingsCategoryData copy() {
		return new NoSortSettingsCategoryData(selectedSlots, color);
	}

	@Override
	public void reloadFrom(NoSortSettingsCategoryData other) {
		this.selectedSlots = new HashSet<>(other.selectedSlots);
		this.color = other.color;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof NoSortSettingsCategoryData that)) return false;
		return Objects.equals(selectedSlots, that.selectedSlots) && color == that.color;
	}

	@Override
	public int hashCode() {
		return Objects.hash(selectedSlots, color);
	}

	public void clearSelectedSlots() {
		selectedSlots.clear();
	}

	public void addSelectedSlot(int slot) {
		selectedSlots.add(slot);
	}

	public void removeSelectedSlot(int slotNumber) {
		selectedSlots.remove(slotNumber);
	}

	public void setColor(DyeColor color) {
		this.color = color;
	}

	public void addSelectedSlots(Set<Integer> noSortSlots) {
		selectedSlots.addAll(noSortSlots);
	}

	public void removeSelectedSlotAtOrAfter(int slotIndex) {
		selectedSlots.removeIf(slot -> slot >= slotIndex);
	}
}
