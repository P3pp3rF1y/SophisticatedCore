package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public interface ILinkedStorageEndpointAdapter<E> {
	ResourceLocation factoryId();

	default boolean isCompatible(ServerLevel level, E endpoint, LinkedStorageHostDescriptor hostDescriptor) {
		return true;
	}

	default Compatibility getCompatibility(ServerLevel level, E endpoint, LinkedStorageHostDescriptor hostDescriptor) {
		return isCompatible(level, endpoint, hostDescriptor) ? Compatibility.COMPATIBLE : Compatibility.INCOMPATIBLE;
	}

	LinkedStorageHostDescriptor createHostDescriptor(ServerLevel level, E endpoint);

	CompoundTag copyCanonicalContents(ServerLevel level, E endpoint);

	void bindEndpoint(ServerLevel level, E endpoint, LinkedStorageEndpointData endpointData);

	default void onEndpointLinked(ServerLevel level, E endpoint) {
	}

	enum Compatibility {
		COMPATIBLE, INCOMPATIBLE, HAS_CONTENTS
	}
}
