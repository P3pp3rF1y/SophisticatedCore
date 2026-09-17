package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LinkedStorageEndpointAccessProviders {
	private static final List<ILinkedStorageEndpointAccessProvider> PROVIDERS = new ArrayList<>();

	private LinkedStorageEndpointAccessProviders() {
	}

	public static void register(ILinkedStorageEndpointAccessProvider provider) {
		PROVIDERS.add(provider);
	}

	public static boolean hasGroupEndpoint(ServerPlayer player, LinkedStorageGroupManager manager, UUID groupId) {
		if (hasGroupEndpointInInventory(player, manager, groupId)) {
			return true;
		}
		return PROVIDERS.stream().anyMatch(provider -> provider.hasGroupEndpoint(player, manager, groupId));
	}

	private static boolean hasGroupEndpointInInventory(ServerPlayer player, LinkedStorageGroupManager manager, UUID groupId) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			LinkedStorageEndpointData endpoint = LinkedStorageStackData.getEndpoint(stack);
			if (endpoint != null && endpoint.groupId().equals(groupId) && manager.isEndpointMember(groupId, endpoint.endpointId())) {
				return true;
			}
		}
		return false;
	}
}
