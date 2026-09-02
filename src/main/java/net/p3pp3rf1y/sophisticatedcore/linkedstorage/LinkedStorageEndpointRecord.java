package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import javax.annotation.Nullable;

import java.util.UUID;

record LinkedStorageEndpointRecord(UUID endpointId, @Nullable UUID lastOpenedBy, long lastOpenedAt) {
	static LinkedStorageEndpointRecord create(UUID endpointId) {
		return new LinkedStorageEndpointRecord(endpointId, null, 0);
	}

	LinkedStorageEndpointRecord withLastOpenedBy(UUID playerId, long gameTime) {
		return new LinkedStorageEndpointRecord(endpointId, playerId, gameTime);
	}
}
