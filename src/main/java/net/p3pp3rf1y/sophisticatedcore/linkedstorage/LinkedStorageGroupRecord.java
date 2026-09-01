package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class LinkedStorageGroupRecord {
	private final UUID id;
	private final UUID ownerId;
	private final UUID primaryEndpointId;
	private final Map<UUID, LinkedStorageEndpointRecord> endpoints;
	private LinkedStorageHostDescriptor hostDescriptor;
	private ContainerContents contents;
	private long revision;
	private long renderRevision;
	private int columnsTaken;

	LinkedStorageGroupRecord(UUID id, UUID ownerId, UUID primaryEndpointId, LinkedStorageHostDescriptor hostDescriptor, ContainerContents contents) {
		this(id, ownerId, primaryEndpointId, List.of(LinkedStorageEndpointRecord.create(primaryEndpointId)), hostDescriptor, contents, 0, 0, 0);
	}

	LinkedStorageGroupRecord(UUID id, UUID ownerId, UUID primaryEndpointId, List<LinkedStorageEndpointRecord> endpoints,
			LinkedStorageHostDescriptor hostDescriptor, ContainerContents contents, long revision, long renderRevision, int columnsTaken) {
		this.id = id;
		this.ownerId = ownerId;
		this.primaryEndpointId = primaryEndpointId;
		this.endpoints = new LinkedHashMap<>();
		endpoints.forEach(endpoint -> this.endpoints.put(endpoint.endpointId(), endpoint));
		this.hostDescriptor = hostDescriptor;
		this.contents = contents;
		this.revision = revision;
		this.renderRevision = renderRevision;
		this.columnsTaken = columnsTaken;
	}

	UUID id() {
		return id;
	}

	UUID ownerId() {
		return ownerId;
	}

	UUID primaryEndpointId() {
		return primaryEndpointId;
	}

	List<LinkedStorageEndpointRecord> endpoints() {
		return List.copyOf(endpoints.values());
	}

	boolean hasEndpoint(UUID endpointId) {
		return endpoints.containsKey(endpointId);
	}

	void addEndpoint(UUID endpointId) {
		endpoints.put(endpointId, LinkedStorageEndpointRecord.create(endpointId));
	}

	boolean removeEndpoint(UUID endpointId) {
		return !primaryEndpointId.equals(endpointId) && endpoints.remove(endpointId) != null;
	}

	void recordEndpointOpened(UUID endpointId, UUID playerId, long gameTime) {
		LinkedStorageEndpointRecord endpoint = endpoints.get(endpointId);
		endpoints.put(endpointId, endpoint.withLastOpenedBy(playerId, gameTime));
	}

	LinkedStorageHostDescriptor hostDescriptor() {
		return hostDescriptor;
	}

	void setHostDescriptor(LinkedStorageHostDescriptor hostDescriptor) {
		this.hostDescriptor = hostDescriptor;
	}

	ContainerContents contents() {
		return contents;
	}

	void setContents(ContainerContents contents) {
		this.contents = contents;
	}

	long revision() {
		return revision;
	}

	void incrementRevision() {
		revision++;
	}

	long renderRevision() {
		return renderRevision;
	}

	void incrementRenderRevision() {
		renderRevision++;
	}

	int columnsTaken() {
		return columnsTaken;
	}

	boolean setColumnsTaken(int columnsTaken) {
		if (this.columnsTaken == columnsTaken) {
			return false;
		}
		this.columnsTaken = columnsTaken;
		return true;
	}

}
