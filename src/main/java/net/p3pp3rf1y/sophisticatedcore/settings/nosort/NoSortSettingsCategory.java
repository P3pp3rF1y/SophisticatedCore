package net.p3pp3rf1y.sophisticatedcore.settings.nosort;

import net.minecraft.world.item.DyeColor;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.ISlotColorCategory;

import java.util.Optional;
import java.util.Set;

public class NoSortSettingsCategory implements ISettingsCategory<NoSortSettingsCategory, NoSortSettingsCategoryData>, ISlotColorCategory {
	public static final String NAME = "no_sort";
	private final Runnable save;
	private NoSortSettingsCategoryData data;

	public NoSortSettingsCategory(NoSortSettingsCategoryData data, Runnable save) {
		this.data = data;
		this.save = save;
	}

	public boolean isSlotSelected(int slotNumber) {
		return data.selectedSlots().contains(slotNumber);
	}

	public void unselectAllSlots() {
		data.clearSelectedSlots();
		save();
	}

	/**
	 * Selects slots that shouldn't be sorted
	 *
	 * @param minSlot inclusive
	 * @param maxSlot exclusive
	 */

	public void selectSlots(int minSlot, int maxSlot) {
		for (int slot = minSlot; slot < maxSlot; slot++) {
			data.addSelectedSlot(slot);
		}
		save();
	}

	public void selectSlot(int slotNumber) {
		selectSlots(slotNumber, slotNumber + 1);
	}

	public void unselectSlot(int slotNumber) {
		data.removeSelectedSlot(slotNumber);
		save();
	}

	private void save() {
		save.run();
	}

	public void setColor(DyeColor color) {
		data.setColor(color);
		save();
	}

	public DyeColor getColor() {
		return data.color();
	}

	@Override
	public Optional<Integer> getSlotColor(int slotNumber) {
		return data.selectedSlots().contains(slotNumber) ? Optional.of(data.color().getTextureDiffuseColor()) : Optional.empty();
	}

	public Set<Integer> getNoSortSlots() {
		return data.selectedSlots();
	}

	@Override
	public void reloadFrom(NoSortSettingsCategoryData data) {
		this.data = data;
	}

	@Override
	public void overwriteWith(NoSortSettingsCategory otherCategory) {
		data.clearSelectedSlots();
		data.addSelectedSlots(otherCategory.getNoSortSlots());
		data.setColor(otherCategory.getColor());
		save();
	}

	@Override
	public boolean isLargerThanNumberOfSlots(int slots) {
		return data.selectedSlots().stream().anyMatch(slotIndex -> slotIndex >= slots);
	}

	@Override
	public void copyTo(NoSortSettingsCategory otherCategory, int startFromSlot, int slotOffset) {
		data.selectedSlots().forEach(slotIndex -> {
			if (slotIndex < startFromSlot) {
				return;
			}
			otherCategory.data.addSelectedSlot(slotIndex + slotOffset);
		});
		otherCategory.save();
	}

	@Override
	public void deleteSlotSettingsFrom(int slotIndex) {
		data.removeSelectedSlotAtOrAfter(slotIndex);
		save();
	}
}
