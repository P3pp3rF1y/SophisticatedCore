package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.resources.Identifier;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class LinkedStorageGroupManager {
	private final LinkedStorageGroupsSavedData savedData;
	private final Map<UUID, ILinkedStorageVirtualHost> virtualHosts = new HashMap<>();
	private final Map<UUID, Set<Runnable>> groupChangeListeners = new HashMap<>();

	LinkedStorageGroupManager(LinkedStorageGroupsSavedData savedData) {
		this.savedData = savedData;
	}

	public UUID createGroup(UUID ownerId, UUID primaryEndpointId, LinkedStorageHostDescriptor hostDescriptor, ContainerContents initialContents) {
		if (LinkedStorageHostFactories.get(hostDescriptor.factoryId()).isEmpty()) {
			throw new IllegalStateException("No linked storage host factory registered for " + hostDescriptor.factoryId());
		}
		UUID groupId = UUID.randomUUID();
		savedData.addGroup(new LinkedStorageGroupRecord(groupId, ownerId, primaryEndpointId, hostDescriptor, initialContents.copy()));
		return groupId;
	}

	public Optional<ILinkedStorageVirtualHost> resolveVirtualHost(UUID groupId) {
		return savedData.findGroup(groupId).flatMap(group -> resolveVirtualHost(groupId, group));
	}

	public Optional<ILinkedStorageVirtualHost> resolveVirtualHost(LinkedStorageEndpointData endpoint, boolean requirePrimary) {
		return savedData.findGroup(endpoint.groupId()).filter(group -> group.hasEndpoint(endpoint.endpointId()))
				.filter(group -> !requirePrimary || group.primaryEndpointId().equals(endpoint.endpointId()))
				.flatMap(group -> resolveVirtualHost(endpoint.groupId(), group));
	}

	private Optional<ILinkedStorageVirtualHost> resolveVirtualHost(UUID groupId, LinkedStorageGroupRecord group) {
		return LinkedStorageHostFactories.get(group.hostDescriptor().factoryId())
				.map(factory -> virtualHosts.computeIfAbsent(groupId, id -> factory.create(new Binding(groupId), group.hostDescriptor().virtualCarrier())));
	}

	public Optional<ILinkedStorageContentsBinding> resolveContents(UUID groupId) {
		return savedData.findGroup(groupId).map(group -> new Binding(groupId));
	}

	public long getRevision(UUID groupId) {
		return getGroup(groupId).revision();
	}

	public long getRenderRevision(UUID groupId) {
		return getGroup(groupId).renderRevision();
	}

	public Runnable subscribeToGroupChanges(UUID groupId, Runnable listener) {
		getGroup(groupId);
		groupChangeListeners.computeIfAbsent(groupId, id -> new LinkedHashSet<>()).add(listener);
		return () -> groupChangeListeners.computeIfPresent(groupId, (id, groupListeners) -> {
			groupListeners.remove(listener);
			return groupListeners.isEmpty() ? null : groupListeners;
		});
	}

	private void replaceContents(UUID groupId, ContainerContents contents) {
		getGroup(groupId).setContents(contents);
		markDirty(groupId, true, false);
	}

	private ContainerContents contents(UUID groupId) {
		return getGroup(groupId).contents();
	}

	private LinkedStorageGroupRecord getGroup(UUID groupId) {
		return savedData.findGroup(groupId).orElseThrow(() -> new IllegalStateException("Unknown linked storage group " + groupId));
	}

	private void markDirty(UUID groupId) {
		markDirty(groupId, false, false);
	}

	private void markRenderDirty(UUID groupId) {
		markDirty(groupId, false, true);
	}

	private void setColumnsTaken(UUID groupId, int columnsTaken) {
		if (getGroup(groupId).setColumnsTaken(columnsTaken)) {
			markLayoutDirty(groupId);
		}
	}

	private void markLayoutDirty(UUID groupId) {
		commit(groupId, false, false, true);
	}

	public boolean usesHostFactory(UUID groupId, Identifier factoryId) {
		return savedData.findGroup(groupId).map(group -> group.hostDescriptor().factoryId().equals(factoryId)).orElse(false);
	}

	public boolean isPrimaryEndpoint(UUID groupId, UUID endpointId) {
		return savedData.findGroup(groupId).map(group -> group.primaryEndpointId().equals(endpointId) && group.hasEndpoint(endpointId)).orElse(false);
	}

	public boolean isEndpointMember(UUID groupId, UUID endpointId) {
		return savedData.findGroup(groupId).map(group -> group.hasEndpoint(endpointId)).orElse(false);
	}

	public void recordEndpointOpened(UUID groupId, UUID endpointId, UUID playerId, long gameTime) {
		LinkedStorageGroupRecord group = getGroup(groupId);
		if (!group.hasEndpoint(endpointId)) {
			throw new IllegalStateException("Endpoint " + endpointId + " is not registered in linked storage group " + groupId);
		}
		group.recordEndpointOpened(endpointId, playerId, gameTime);
		savedData.setDirty();
	}

	public void registerEndpoint(UUID groupId, UUID endpointId) {
		savedData.findGroup(groupId).orElseThrow().addEndpoint(endpointId);
		commit(groupId, false, false, false);
	}

	public boolean unregisterEndpoint(UUID groupId, UUID endpointId) {
		return savedData.findGroup(groupId).map(group -> {
			if (!group.removeEndpoint(endpointId)) {
				return false;
			}
			commit(groupId, false, false, false);
			return true;
		}).orElse(false);
	}

	public void discardUnboundGroup(UUID groupId, UUID primaryEndpointId) {
		if (!isPrimaryEndpoint(groupId, primaryEndpointId)) {
			return;
		}
		virtualHosts.remove(groupId);
		groupChangeListeners.remove(groupId);
		savedData.removeGroup(groupId);
	}

	public void activatePendingCraftClaim(ActivePendingCraftClaim claim) {
		savedData.addActivePendingClaim(claim);
	}

	public Optional<ActivePendingCraftClaim> getActivePendingCraftClaim(UUID claimId) {
		return savedData.getActivePendingClaim(claimId);
	}

	public boolean consumeActivePendingCraftClaim(UUID claimId) {
		return savedData.removeActivePendingClaim(claimId);
	}

	public boolean updatePrimaryHostDescriptor(UUID groupId, UUID endpointId, LinkedStorageHostDescriptor hostDescriptor) {
		return savedData.findGroup(groupId).filter(group -> group.primaryEndpointId().equals(endpointId) && group.hasEndpoint(endpointId))
				.filter(group -> group.hostDescriptor().factoryId().equals(hostDescriptor.factoryId())).map(group -> {
					if (group.hostDescriptor().equals(hostDescriptor)) {
						return false;
					}
					group.setHostDescriptor(hostDescriptor);
					ILinkedStorageVirtualHost virtualHost = virtualHosts.get(groupId);
					if (virtualHost != null) {
						virtualHost.onVirtualCarrierChanged(hostDescriptor.virtualCarrier());
					}
					commit(groupId, false, false, false);
					return true;
				}).orElse(false);
	}

	public Optional<LinkedStorageHostDescriptor> getHostDescriptor(UUID groupId) {
		return savedData.findGroup(groupId).map(LinkedStorageGroupRecord::hostDescriptor);
	}

	private void markDirty(UUID groupId, boolean contentsReplaced, boolean renderChanged) {
		commit(groupId, contentsReplaced, renderChanged, false);
	}

	private void commit(UUID groupId, boolean contentsReplaced, boolean renderChanged, boolean layoutChanged) {
		LinkedStorageGroupRecord group = getGroup(groupId);
		ILinkedStorageVirtualHost virtualHost = virtualHosts.get(groupId);
		if (renderChanged && virtualHost != null) {
			virtualHost.getVirtualCarrierSnapshot()
					.ifPresent(virtualCarrier -> group.setHostDescriptor(new LinkedStorageHostDescriptor(group.hostDescriptor().factoryId(), virtualCarrier)));
		}
		group.incrementRevision();
		if (renderChanged) {
			group.incrementRenderRevision();
		}
		savedData.setDirty();
		if (contentsReplaced && virtualHost != null) {
			virtualHost.onLinkedStorageContentsChanged();
		}
		if (layoutChanged && virtualHost != null) {
			virtualHost.onLinkedStorageLayoutChanged();
		}
		for (Runnable listener : Set.copyOf(groupChangeListeners.getOrDefault(groupId, Set.of()))) {
			listener.run();
		}
	}

	private class Binding implements ILinkedStorageContentsBinding {
		private final UUID groupId;

		private Binding(UUID groupId) {
			this.groupId = groupId;
		}

		@Override
		public UUID groupId() {
			return groupId;
		}

		@Override
		public ContainerContents getContents(UUID storageId) {
			return LinkedStorageGroupManager.this.contents(groupId);
		}

		@Override
		public void setContents(UUID storageId, ContainerContents contents) {
			replaceContents(groupId, contents);
		}

		@Override
		public void markChanged() {
			LinkedStorageGroupManager.this.markDirty(groupId);
		}

		@Override
		public void markRenderDirty() {
			LinkedStorageGroupManager.this.markRenderDirty(groupId);
		}

		@Override
		public int getColumnsTaken() {
			return getGroup(groupId).columnsTaken();
		}

		@Override
		public void setColumnsTaken(int columnsTaken) {
			LinkedStorageGroupManager.this.setColumnsTaken(groupId, columnsTaken);
		}
	}
}
