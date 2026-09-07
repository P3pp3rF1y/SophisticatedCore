package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import java.util.UUID;

public interface ILinkedStorageBlockEndpoint extends ILinkedStorageInteractionTarget {
	@Override
	default boolean isLinkedStorageLinkCandidate() {
		return true;
	}

	@Nullable
	@Override
	LinkedStorageEndpointData getLinkedStorageEndpointData();

	ILinkedStorageEndpointAdapter<ILinkedStorageBlockEndpoint> getLinkedStorageBlockEndpointAdapter();

	@Override
	default LinkedStorageService.LinkResult link(ServerLevel level, UUID playerId, ItemStack linker) {
		return LinkedStorageService.linkWithResult(level, playerId, linker, this);
	}
}
