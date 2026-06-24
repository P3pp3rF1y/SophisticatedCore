package net.p3pp3rf1y.sophisticatedcore.settings;

import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;

public interface ISettingsCategory<T extends ISettingsCategory<T, D>, D extends ContainerContents.ISettingsCategoryData<D>> {
	void reloadFrom(D data);

	void overwriteWith(T otherCategory); // TODO probably replace uses of this with reloadFrom?

	boolean isLargerThanNumberOfSlots(int slots);

	void copyTo(T otherCategory, int startFromSlot, int slotOffset);

	void deleteSlotSettingsFrom(int slotIndex);
}
