package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import java.util.Optional;

public interface ILinkedStorageEndpointProvider {
	Optional<LinkedStorageEndpointData> getLinkedStorageEndpoint();

	default Optional<LinkedStorageEndpointRole> getLinkedStorageEndpointRole() {
		return Optional.empty();
	}
}
