package net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsContainerBase;

import java.util.List;

public class ItemDisplaySettingsContainer extends SettingsContainerBase<ItemDisplaySettingsCategory> {
	private static final String COLOR_TAG = "color";
	private static final String SELECT_SLOT_TAG = "selectSlot";
	private static final String UNSELECT_SLOT_TAG = "unselectSlot";
	private static final String ROTATE_CLOCKWISE_TAG = "rotateClockwise";
	private static final String ROTATE_COUNTER_CLOCKWISE_TAG = "rotateCounterClockwise";

	public ItemDisplaySettingsContainer(SettingsContainerMenu<?> settingsContainer, String categoryName, ItemDisplaySettingsCategory category) {
		super(settingsContainer, categoryName, category);
	}

	@Override
	public void handleMessage(CompoundTag data) {
		if (data.contains(SELECT_SLOT_TAG)) {
			selectSlot(data.getInt(SELECT_SLOT_TAG));
		} else if (data.contains(UNSELECT_SLOT_TAG)) {
			unselectSlot(data.getInt(UNSELECT_SLOT_TAG));
		} else if (data.contains(ROTATE_CLOCKWISE_TAG)) {
			rotateClockwise(data.getInt(ROTATE_CLOCKWISE_TAG));
		} else if (data.contains(ROTATE_COUNTER_CLOCKWISE_TAG)) {
			rotateCounterClockwise(data.getInt(ROTATE_COUNTER_CLOCKWISE_TAG));
		} else if (data.contains(COLOR_TAG)) {
			setColor(DyeColor.byId(data.getInt(COLOR_TAG)));
		}
	}

	public void unselectSlot(int slotIndex) {
		if (!isSlotSelected(slotIndex)) {
			return;
		}

		if (isServer()) {
			getCategory().unselectSlot(slotIndex);
		} else {
			getCategory().unselectSlot(slotIndex); //need to do this on client as well so that selection highlight knows whether the slot was unselected and can move to it
			sendIntToServer(UNSELECT_SLOT_TAG, slotIndex);
		}
	}

	public void selectSlot(int slotIndex) {
		if (isSlotSelected(slotIndex)) {
			return;
		}
		if (isServer()) {
			getCategory().selectSlot(slotIndex);
		} else {
			getCategory().selectSlot(slotIndex); //need to do this on client as well so that selection highlight knows whether the slot was selected and can move to it
			sendIntToServer(SELECT_SLOT_TAG, slotIndex);
		}
	}

	public void rotateClockwise(int slotIndex) {
		if (isServer()) {
			getCategory().rotate(slotIndex, true);
		} else {
			sendIntToServer(ROTATE_CLOCKWISE_TAG, slotIndex);
		}
	}

	public void rotateCounterClockwise(int slotIndex) {
		if (isServer()) {
			getCategory().rotate(slotIndex, false);
		} else {
			sendIntToServer(ROTATE_COUNTER_CLOCKWISE_TAG, slotIndex);
		}
	}

	public void setColor(DyeColor color) {
		if (isServer()) {
			getCategory().setColor(color);
		} else {
			sendIntToServer(COLOR_TAG, color.getId());
		}
	}

	public boolean isSlotSelected(int slotIndex) {
		return getCategory().getSlots().contains(slotIndex);
	}

	public DyeColor getColor() {
		return getCategory().getColor();
	}

	public int getRotation(int slotIndex) {
		return getCategory().getRotation(slotIndex);
	}

	public int getFirstSelectedSlot() {
		List<Integer> slots = getCategory().getSlots();

		return slots.isEmpty() ? -1 : slots.get(0);
	}
}
