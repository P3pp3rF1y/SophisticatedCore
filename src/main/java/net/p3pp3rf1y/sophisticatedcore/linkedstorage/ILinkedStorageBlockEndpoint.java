package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import javax.annotation.Nullable;

public interface ILinkedStorageBlockEndpoint {
	@Nullable
	LinkedStorageEndpointData getLinkedStorageEndpointData();

	ILinkedStorageEndpointAdapter<ILinkedStorageBlockEndpoint> getLinkedStorageBlockEndpointAdapter();
}
