package net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsContainerBase;

import java.util.List;

public class ItemDisplaySettingsContainer extends SettingsContainerBase<ItemDisplaySettingsCategory> {
	private static final String COLOR_TAG = "color";
	private static final String DISPLAY_SIDE_TAG = "displaySide";
	private static final String SELECT_SLOT_TAG = "selectSlot";
	private static final String UNSELECT_SLOT_TAG = "unselectSlot";
	private static final String ROTATE_CLOCKWISE_TAG = "rotateClockwise";
	private static final String ROTATE_COUNTER_CLOCKWISE_TAG = "rotateCounterClockwise";
	private static final String CHANGE_Z_OFFSET_TAG = "changeZOffset";
	private static final String RESET_Z_OFFSET_TAG = "resetZOffset";
	private static final String SLOT_TAG = "slot";
	private static final String OFFSET_CHANGE_TAG = "offsetChange";

	public ItemDisplaySettingsContainer(SettingsContainerMenu<?> settingsContainer, String categoryName, ItemDisplaySettingsCategory category) {
		super(settingsContainer, categoryName, category);
	}

	@Override
	public void handlePacket(CompoundTag data) {
		data.getInt(SELECT_SLOT_TAG).ifPresent(this::selectSlot);
		data.getInt(UNSELECT_SLOT_TAG).ifPresent(this::unselectSlot);
		data.getInt(ROTATE_CLOCKWISE_TAG).ifPresent(this::rotateClockwise);
		data.getInt(ROTATE_COUNTER_CLOCKWISE_TAG).ifPresent(this::rotateCounterClockwise);
		data.getBoolean(CHANGE_Z_OFFSET_TAG).ifPresent(ignored -> changeZOffset(data.getIntOr(SLOT_TAG, -1), data.getIntOr(OFFSET_CHANGE_TAG, 0)));
		data.getInt(RESET_Z_OFFSET_TAG).ifPresent(this::resetZOffset);
		data.getInt(COLOR_TAG).ifPresent(colorId -> setColor(DyeColor.byId(colorId)));
		data.getString(DISPLAY_SIDE_TAG).ifPresent(sideName -> setDisplaySide(DisplaySide.fromName(sideName)));
	}

	public void unselectSlot(int slotIndex) {
		if (!isSlotSelected(slotIndex) || !canDeselectSlots()) {
			return;
		}

		if (isServer()) {
			getCategory().unselectSlot(slotIndex);
		} else {
			getCategory().unselectSlot(slotIndex); // need to do this on client as well so that selection highlight knows whether the slot was unselected and
													// can move to it
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
			getCategory().selectSlot(slotIndex); // need to do this on client as well so that selection highlight knows whether the slot was selected and can
													// move to it
			sendIntToServer(SELECT_SLOT_TAG, slotIndex);
		}
	}

	public void rotateClockwise(int slotIndex) {
		if (isServer()) {
			getCategory().rotate(slotIndex, true);
		} else {
			getCategory().rotate(slotIndex, true);
			sendIntToServer(ROTATE_CLOCKWISE_TAG, slotIndex);
		}
	}

	public void rotateCounterClockwise(int slotIndex) {
		if (isServer()) {
			getCategory().rotate(slotIndex, false);
		} else {
			getCategory().rotate(slotIndex, false);
			sendIntToServer(ROTATE_COUNTER_CLOCKWISE_TAG, slotIndex);
		}
	}

	public void changeZOffset(int slotIndex, int offsetChange) {
		if (isServer()) {
			getCategory().changeZOffset(slotIndex, offsetChange);
		} else {
			getCategory().changeZOffset(slotIndex, offsetChange);
			CompoundTag tag = new CompoundTag();
			tag.putBoolean(CHANGE_Z_OFFSET_TAG, true);
			tag.putInt(SLOT_TAG, slotIndex);
			tag.putInt(OFFSET_CHANGE_TAG, offsetChange);
			sendDataToServer(() -> tag);
		}
	}

	public void resetZOffset(int slotIndex) {
		if (isServer()) {
			getCategory().setZOffset(slotIndex, 0);
		} else {
			getCategory().setZOffset(slotIndex, 0);
			sendIntToServer(RESET_Z_OFFSET_TAG, slotIndex);
		}
	}

	public void setColor(DyeColor color) {
		if (isServer()) {
			getCategory().setColor(color);
		} else {
			sendIntToServer(COLOR_TAG, color.getId());
		}
	}

	public void setDisplaySide(DisplaySide displaySide) {
		if (isServer()) {
			getCategory().setDisplaySide(displaySide);
		} else {
			getCategory().setDisplaySide(displaySide);
			sendStringToServer(DISPLAY_SIDE_TAG, displaySide.getSerializedName());
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

	public int getZOffset(int slotIndex) {
		return getCategory().getZOffset(slotIndex);
	}

	public int getFirstSelectedSlot() {
		List<Integer> slots = getCategory().getSlots();

		return slots.isEmpty() ? -1 : slots.getFirst();
	}

	public DisplaySide getDisplaySide() {
		return getCategory().getDisplaySide();
	}

	public boolean supportsSideSelection() {
		return getSettingsContainer().supportsItemDisplaySideSelection();
	}

	public boolean canDeselectSlots() {
		return getCategory().canDeselectSlots();
	}
}
