package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.p3pp3rf1y.sophisticatedcore.api.IStorageSavedData;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;

import java.util.UUID;

public interface ILinkedStorageContentsBinding extends IStorageSavedData {
	UUID groupId();

	default ContainerContents contents() {
		return getContents(groupId());
	}

	default void markDirty() {
		markChanged();
	}

	default void markRenderDirty() {
		markDirty();
	}

	int getColumnsTaken();

	void setColumnsTaken(int columnsTaken);
}
