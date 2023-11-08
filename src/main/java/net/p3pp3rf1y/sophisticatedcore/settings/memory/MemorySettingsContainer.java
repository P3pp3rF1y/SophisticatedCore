package net.p3pp3rf1y.sophisticatedcore.settings.memory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsContainerBase;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTemplateStorage;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.Map;

public class MemorySettingsContainer extends SettingsContainerBase<MemorySettingsCategory> {
	private static final String ACTION_TAG = "action";
	private static final String SELECT_ALL_ACTION = "selectAll";
	private static final String UNSELECT_ALL_ACTION = "unselectAll";
	private static final String UNSELECT_SLOT_TAG = "unselectSlot";
	private static final String SELECT_SLOT_TAG = "selectSlot";
	private static final String IGNORE_NBT_TAG = "ignoreNbt";

	public MemorySettingsContainer(SettingsContainerMenu<?> settingsContainer, String categoryName, MemorySettingsCategory category) {
		super(settingsContainer, categoryName, category);
	}

	@Override
	public void handleMessage(CompoundTag data) {
		if (data.contains(ACTION_TAG)) {
			switch (data.getString(ACTION_TAG)) {
				case SELECT_ALL_ACTION -> selectAllSlots();
				case UNSELECT_ALL_ACTION -> unselectAllSlots();
				default -> {
					//noop
				}
			}
		} else if (data.contains(SELECT_SLOT_TAG)) {
			selectSlot(data.getInt(SELECT_SLOT_TAG));
		} else if (data.contains(UNSELECT_SLOT_TAG)) {
			unselectSlot(data.getInt(UNSELECT_SLOT_TAG));
		} else if (data.contains(IGNORE_NBT_TAG)) {
			setIgnoreNbt(data.getBoolean(IGNORE_NBT_TAG));
		}
	}

	public void unselectSlot(int slotNumber) {
		if (!isSlotSelected(slotNumber)) {
			return;
		}
		if (isServer()) {
			getCategory().unselectSlot(slotNumber);
			getSettingsContainer().onMemorizedStackRemoved(slotNumber);
		} else {
			sendIntToServer(UNSELECT_SLOT_TAG, slotNumber);
		}
	}

	public void selectSlot(int slotNumber) {
		if (isSlotSelected(slotNumber)) {
			return;
		}
		if (isServer()) {
			getCategory().selectSlot(slotNumber);
			getSettingsContainer().onMemorizedStackAdded(slotNumber);
		} else {
			sendIntToServer(SELECT_SLOT_TAG, slotNumber);
		}
	}

	public void unselectAllSlots() {
		if (isServer()) {
			getCategory().unselectAllSlots();
		} else {
			sendStringToServer(ACTION_TAG, UNSELECT_ALL_ACTION);
		}
	}

	public void selectAllSlots() {
		if (isServer()) {
			getCategory().selectSlots(0, getSettingsContainer().getNumberOfSlots());
		} else {
			sendStringToServer(ACTION_TAG, SELECT_ALL_ACTION);
		}
	}

	public boolean isSlotSelected(int slotNumber) {
		return getCategory().isSlotSelected(slotNumber);
	}

	public boolean ignoresNbt() {
		return getCategory().ignoresNbt();
	}

	public void setIgnoreNbt(boolean ignoreNbt) {
		if (isServer()) {
			getCategory().setIgnoreNbt(ignoreNbt);
			getSettingsContainer().onMemorizedItemsChanged();
		} else {
			sendBooleanToServer(IGNORE_NBT_TAG, ignoreNbt);
		}
	}

	public ItemStack getMemorizedStack(int slotNumber) {
		return getCategory().getSlotFilterStack(slotNumber, false).orElse(ItemStack.EMPTY);
	}

	public ItemStack getSelectedTemplatesMemorizedStack(int slotNumber) {
		return getSettingsContainer().getSelectedTemplatesCategory(MemorySettingsCategory.class).flatMap(cat -> cat.getSlotFilterStack(slotNumber, false))
				.orElse(ItemStack.EMPTY);
	}

}
