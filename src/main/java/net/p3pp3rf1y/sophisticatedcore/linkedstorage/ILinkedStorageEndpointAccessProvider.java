package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

@FunctionalInterface
public interface ILinkedStorageEndpointAccessProvider {
	boolean hasGroupEndpoint(ServerPlayer player, LinkedStorageGroupManager manager, UUID groupId);
}
