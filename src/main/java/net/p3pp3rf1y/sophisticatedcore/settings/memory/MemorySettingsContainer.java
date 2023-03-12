package net.p3pp3rf1y.sophisticatedcore.settings.memory;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
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
	private static final int TOTAL_SAVE_SLOTS = 10;
	private static final String SAVE_SLOT_TAG = "saveSlot";
	private static final String LOAD_SLOT_TAG = "loadSlot";

	private int saveSlot = 1;
	private int loadSlot = -1;

	public MemorySettingsContainer(SettingsContainerMenu<?> settingsContainer, String categoryName, MemorySettingsCategory category) {
		super(settingsContainer, categoryName, category);

		saveSlot = 1;
		if (getNumberOfSaves() >= 1) {
			selectLoadSlot(1);
		}
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
		} else if (data.contains(LOAD_SLOT_TAG)) {
			selectLoadSlot(data.getInt(LOAD_SLOT_TAG));
		} else if (data.contains(SAVE_SLOT_TAG)) {
			saveSlot = data.getInt(SAVE_SLOT_TAG);
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

	public void saveTemplate() {
		getSettingsContainer().saveTemplate(saveSlot);
		getSettingsContainer().getPlayer().sendMessage(new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsMessage("save_template"), saveSlot), Util.NIL_UUID);

		if (saveSlot == 1 && getNumberOfSaves() == 1) {
			selectLoadSlot(1);
		}
	}

	public void loadTemplate() {
		getSettingsContainer().loadTemplate();
		getSettingsContainer().getPlayer().sendMessage(new TranslatableComponent(TranslationHelper.INSTANCE.translSettingsMessage("load_template"), loadSlot), Util.NIL_UUID);
	}

	public void scrollSaveSlot(boolean next) {
		int size = getNumberOfSaves();

		if (size == 0) {
			saveSlot = 1;
			return;
		}

		saveSlot += next ? 1 : -1;

		int maxSlot = Math.min(TOTAL_SAVE_SLOTS, size + 1);
		if (saveSlot < 1) {
			saveSlot = maxSlot;
		}
		if (saveSlot > maxSlot) {
			saveSlot = 1;
		}

		sendDataToServer(() -> NBTHelper.putInt(new CompoundTag(), SAVE_SLOT_TAG, saveSlot));
	}

	private int getNumberOfSaves() {
		return SettingsTemplateStorage.get().getPlayerTemplates(getSettingsContainer().getPlayer()).size();
	}

	private void selectLoadSlot(int loadSlot) {
		this.loadSlot = loadSlot;
		updateSelectedTemplate(SettingsTemplateStorage.get().getPlayerTemplates(getSettingsContainer().getPlayer()));
	}

	public void scrollLoadSlot(boolean next) {
		Map<Integer, CompoundTag> playerTemplates = SettingsTemplateStorage.get().getPlayerTemplates(getSettingsContainer().getPlayer());
		int size = playerTemplates.size();

		if (size == 0) {
			loadSlot = -1;
			return;
		} else if (size == 1) {
			loadSlot = 1;
		} else {
			loadSlot += next ? 1 : -1;

			if (loadSlot < 1) {
				loadSlot = size;
			}
			if (loadSlot > size) {
				loadSlot = 1;
			}
		}

		updateSelectedTemplate(playerTemplates);
		sendDataToServer(() -> NBTHelper.putInt(new CompoundTag(), LOAD_SLOT_TAG, loadSlot));
	}

	private void updateSelectedTemplate(Map<Integer, CompoundTag> playerTemplates) {
		getSettingsContainer().updateSelectedTemplate(playerTemplates.get(loadSlot));
	}

	public int getSaveSlot() {
		return saveSlot;
	}

	public int getLoadSlot() {
		return loadSlot;
	}

	public ItemStack getSelectedTemplatesMemorizedStack(int slotNumber) {
		return getSettingsContainer().getSelectedTemplatesCategory(MemorySettingsCategory.class).flatMap(cat -> cat.getSlotFilterStack(slotNumber, false))
				.orElse(ItemStack.EMPTY);
	}

}
