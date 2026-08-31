package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import javax.annotation.Nullable;

import java.util.UUID;

public record LinkedStorageEndpointRecord(UUID endpointId, @Nullable UUID lastOpenedBy, long lastOpenedAt) {
	static LinkedStorageEndpointRecord create(UUID endpointId) {
		return new LinkedStorageEndpointRecord(endpointId, null, -1L);
	}

	LinkedStorageEndpointRecord withLastOpenedBy(UUID playerId, long gameTime) {
		return new LinkedStorageEndpointRecord(endpointId, playerId, gameTime);
	}
}
