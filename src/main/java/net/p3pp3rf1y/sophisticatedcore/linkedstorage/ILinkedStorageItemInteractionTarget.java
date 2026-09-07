package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

import java.util.UUID;

public interface ILinkedStorageItemInteractionTarget extends ILinkedStorageInteractionTarget {
	ItemStack getLinkedStorageItem();

	@Override
	default boolean isLinkedStorageLinkCandidate() {
		return LinkedStorageService.isLinkCandidate(getLinkedStorageItem());
	}

	@Override
	default LinkedStorageEndpointData getLinkedStorageEndpointData() {
		return getLinkedStorageItem().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
	}

	@Override
	default LinkedStorageService.LinkResult link(ServerLevel level, UUID playerId, ItemStack linker) {
		return LinkedStorageService.linkWithResult(level, playerId, linker, getLinkedStorageItem());
	}
}
