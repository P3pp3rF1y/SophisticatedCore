package net.p3pp3rf1y.sophisticatedcore.api;

import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;

import java.util.UUID;

public interface IStorageSavedData {
	ContainerContents getContents(UUID storageId);
	void setContents(UUID storageId, ContainerContents contents);
	void markChanged();
}
