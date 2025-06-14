package net.p3pp3rf1y.sophisticatedcore.api;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public interface IStorageSavedData {
	CompoundTag getContents(UUID storageId);
	void setContents(UUID storageId, CompoundTag contents);
	void markChanged();
}
