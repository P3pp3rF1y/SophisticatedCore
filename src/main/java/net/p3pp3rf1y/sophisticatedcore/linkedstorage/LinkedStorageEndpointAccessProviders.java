package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.network.ItemStackReference;

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

	public static boolean hasGroupEndpoint(ServerPlayer player, LinkedStorageGroupManager manager, UUID groupId, ItemStackReference reference) {
		if (isGroupEndpoint(reference.resolve(player), manager, groupId)) {
			return true;
		}
		return PROVIDERS.stream().anyMatch(provider -> isGroupEndpoint(provider.resolveEndpointStack(player, reference), manager, groupId));
	}

	private static boolean isGroupEndpoint(ItemStack stack, LinkedStorageGroupManager manager, UUID groupId) {
		LinkedStorageEndpointData endpoint = LinkedStorageStackData.getEndpoint(stack);
		return !stack.isEmpty() && endpoint != null && endpoint.groupId().equals(groupId) && manager.isEndpointMember(groupId, endpoint.endpointId());
	}
}
