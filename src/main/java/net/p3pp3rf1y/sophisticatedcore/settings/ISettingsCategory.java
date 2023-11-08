package net.p3pp3rf1y.sophisticatedcore.settings;

import net.minecraft.nbt.CompoundTag;

public interface ISettingsCategory<T extends ISettingsCategory<?>> {
	void reloadFrom(CompoundTag categoryNbt);

	void overwriteWith(T otherCategory);

	boolean isLargerThanNumberOfSlots(int slots);
}
