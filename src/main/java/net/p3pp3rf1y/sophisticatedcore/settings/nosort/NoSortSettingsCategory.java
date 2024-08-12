package net.p3pp3rf1y.sophisticatedcore.settings.nosort;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.ISlotColorCategory;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class NoSortSettingsCategory implements ISettingsCategory<NoSortSettingsCategory>, ISlotColorCategory {
	public static final String NAME = "no_sort";
	private static final String COLOR_TAG = "color";
	private static final String SELECTED_SLOTS_TAG = "selectedSlots";
	private CompoundTag categoryNbt;
	private final Consumer<CompoundTag> saveNbt;
	private final Set<Integer> selectedSlots = new HashSet<>();
	private DyeColor color = DyeColor.LIME;

	public NoSortSettingsCategory(CompoundTag categoryNbt, Consumer<CompoundTag> saveNbt) {
		this.categoryNbt = categoryNbt;
		this.saveNbt = saveNbt;

		deserialize();
	}

	private void deserialize() {
		for (int slotNumber : categoryNbt.getIntArray(SELECTED_SLOTS_TAG)) {
			selectedSlots.add(slotNumber);
		}
		NBTHelper.getInt(categoryNbt, COLOR_TAG).ifPresent(c -> color = DyeColor.byId(c));
	}

	public boolean isSlotSelected(int slotNumber) {
		return selectedSlots.contains(slotNumber);
	}

	public void unselectAllSlots() {
		selectedSlots.clear();
		serializeSelectedSlots();
	}

	/**
	 * Selects slots that shouldn't be sorted
	 *
	 * @param minSlot inclusive
	 * @param maxSlot exclusive
	 */

	public void selectSlots(int minSlot, int maxSlot) {
		for (int slot = minSlot; slot < maxSlot; slot++) {
			selectedSlots.add(slot);
		}
		serializeSelectedSlots();
	}

	public void selectSlot(int slotNumber) {
		selectSlots(slotNumber, slotNumber + 1);
	}

	public void unselectSlot(int slotNumber) {
		selectedSlots.remove(slotNumber);
		serializeSelectedSlots();
	}

	private void serializeSelectedSlots() {
		int[] slots = new int[selectedSlots.size()];
		int i = 0;
		for (int slotNumber : selectedSlots) {
			slots[i++] = slotNumber;
		}
		categoryNbt.putIntArray(SELECTED_SLOTS_TAG, slots);
		saveNbt.accept(categoryNbt);
	}

	public void setColor(DyeColor color) {
		this.color = color;
		categoryNbt.putInt(COLOR_TAG, color.getId());
		saveNbt.accept(categoryNbt);
	}

	public DyeColor getColor() {
		return color;
	}

	@Override
	public Optional<Integer> getSlotColor(int slotNumber) {
		return selectedSlots.contains(slotNumber) ? Optional.of(color.getTextureDiffuseColor()) : Optional.empty();
	}

	public Set<Integer> getNoSortSlots() {
		return selectedSlots;
	}

	@Override
	public void reloadFrom(CompoundTag categoryNbt) {
		this.categoryNbt = categoryNbt;
		selectedSlots.clear();
		color = DyeColor.LIME;
		deserialize();
	}

	@Override
	public void overwriteWith(NoSortSettingsCategory otherCategory) {
		selectedSlots.clear();
		selectedSlots.addAll(otherCategory.getNoSortSlots());
		serializeSelectedSlots();
		setColor(otherCategory.getColor());
	}

	@Override
	public boolean isLargerThanNumberOfSlots(int slots) {
		return selectedSlots.stream().anyMatch(slotIndex -> slotIndex >= slots);
	}

	@Override
	public void copyTo(NoSortSettingsCategory otherCategory, int startFromSlot, int slotOffset) {
		selectedSlots.forEach(slotIndex -> {
			if (slotIndex < startFromSlot) {
				return;
			}
			otherCategory.selectedSlots.add(slotIndex + slotOffset);
		});
		otherCategory.serializeSelectedSlots();
	}

	@Override
	public void deleteSlotSettingsFrom(int slotIndex) {
		selectedSlots.removeIf(slot -> slot >= slotIndex);
		serializeSelectedSlots();
	}
}
