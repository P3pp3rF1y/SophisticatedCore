package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ClientLinkedStorageContents {
	private static final long REQUEST_INTERVAL = 20;
	private static final Map<UUID, Snapshot> SNAPSHOTS = new HashMap<>();
	private static final Map<UUID, ClientContents> CONTENTS = new HashMap<>();
	private static final Map<UUID, Long> LAST_REQUEST_TIMES = new HashMap<>();
	private static final Set<UUID> UPDATED_GROUPS = new HashSet<>();

	private ClientLinkedStorageContents() {
	}

	public static Optional<ILinkedStorageContents> getContents(UUID groupId) {
		if (!SNAPSHOTS.containsKey(groupId)) {
			return Optional.empty();
		}
		return Optional.of(CONTENTS.computeIfAbsent(groupId, ClientContents::new));
	}

	public static void updateContents(UUID groupId, long revision, CompoundTag contents, Component groupName, int inventorySlots, int upgradeSlots,
			int columnsTaken) {
		Snapshot snapshot = new Snapshot(contents.copy(), revision, groupName, inventorySlots, upgradeSlots, columnsTaken);
		SNAPSHOTS.put(groupId, snapshot);
		ClientContents clientContents = CONTENTS.get(groupId);
		if (clientContents != null) {
			clientContents.update(snapshot);
		}
		UPDATED_GROUPS.add(groupId);
	}

	public static Optional<Component> getGroupName(UUID groupId) {
		return Optional.ofNullable(SNAPSHOTS.get(groupId)).map(Snapshot::groupName);
	}

	public static Optional<Long> getRevision(UUID groupId) {
		return Optional.ofNullable(SNAPSHOTS.get(groupId)).map(Snapshot::revision);
	}

	public static Optional<Integer> getInventorySlots(UUID groupId) {
		return Optional.ofNullable(SNAPSHOTS.get(groupId)).map(Snapshot::inventorySlots);
	}

	public static Optional<Integer> getUpgradeSlots(UUID groupId) {
		return Optional.ofNullable(SNAPSHOTS.get(groupId)).map(Snapshot::upgradeSlots);
	}

	public static Optional<Integer> getColumnsTaken(UUID groupId) {
		return Optional.ofNullable(SNAPSHOTS.get(groupId)).map(Snapshot::columnsTaken);
	}

	public static boolean shouldRequestSnapshot(UUID groupId, long gameTime) {
		Long lastRequestTime = LAST_REQUEST_TIMES.get(groupId);
		if (lastRequestTime != null && lastRequestTime + REQUEST_INTERVAL >= gameTime) {
			return false;
		}
		LAST_REQUEST_TIMES.put(groupId, gameTime);
		return true;
	}

	public static boolean removeUpdatedGroup(UUID groupId) {
		return UPDATED_GROUPS.remove(groupId);
	}

	public static void clear() {
		SNAPSHOTS.clear();
		CONTENTS.clear();
		LAST_REQUEST_TIMES.clear();
		UPDATED_GROUPS.clear();
	}

	private static final class ClientContents implements ILinkedStorageContents {
		private final UUID groupId;
		private Snapshot snapshot;

		private ClientContents(UUID groupId) {
			this.groupId = groupId;
			snapshot = SNAPSHOTS.get(groupId);
		}

		private void update(Snapshot snapshot) {
			this.snapshot = snapshot;
		}

		@Override
		public UUID groupId() {
			return groupId;
		}

		@Override
		public CompoundTag getContents() {
			return snapshot.contents();
		}

		@Override
		public void setContents(CompoundTag contents) {
			updateContents(groupId, snapshot.revision(), contents, snapshot.groupName(), snapshot.inventorySlots(), snapshot.upgradeSlots(),
					snapshot.columnsTaken());
		}

		@Override
		public void markChanged() {
		}

		@Override
		public int getColumnsTaken() {
			return snapshot.columnsTaken();
		}

		@Override
		public void setColumnsTaken(int columnsTaken) {
			updateContents(groupId, snapshot.revision(), snapshot.contents(), snapshot.groupName(), snapshot.inventorySlots(), snapshot.upgradeSlots(),
					columnsTaken);
		}
	}

	private record Snapshot(CompoundTag contents, long revision, Component groupName, int inventorySlots, int upgradeSlots, int columnsTaken) {
	}
}
