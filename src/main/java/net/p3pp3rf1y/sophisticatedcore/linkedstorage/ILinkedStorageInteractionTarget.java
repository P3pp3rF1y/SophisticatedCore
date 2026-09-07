package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import java.util.UUID;

public interface ILinkedStorageInteractionTarget {
	boolean isLinkedStorageLinkCandidate();

	@Nullable
	LinkedStorageEndpointData getLinkedStorageEndpointData();

	LinkedStorageService.LinkResult link(ServerLevel level, UUID playerId, ItemStack linker);

	default void onLinkedStorageEndpointChanged(ServerPlayer player, @Nullable LinkedStorageEndpointData previousEndpoint,
			@Nullable LinkedStorageEndpointData currentEndpoint) {
	}
}
