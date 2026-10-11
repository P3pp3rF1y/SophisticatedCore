package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.p3pp3rf1y.sophisticatedcore.api.IStorageSavedData;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;

import java.util.UUID;

public interface ILinkedStorageContents extends IStorageSavedData {
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

	default void onHandlersInvalidated() {
		// Client snapshots and detached contents have no live capability caches.
	}

	int getColumnsTaken();

	void setColumnsTaken(int columnsTaken);
}
