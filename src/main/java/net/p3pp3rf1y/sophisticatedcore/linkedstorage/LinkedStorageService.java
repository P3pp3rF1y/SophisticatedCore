package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class LinkedStorageService {
	private LinkedStorageService() {
	}

	public static boolean link(ServerLevel level, ItemStack linker, ItemStack endpoint) {
		return linkWithResult(level, null, linker, endpoint) == LinkResult.SUCCESS;
	}

	public static boolean link(ServerLevel level, UUID ownerId, ItemStack linker, ItemStack endpoint) {
		return linkWithResult(level, ownerId, linker, endpoint) == LinkResult.SUCCESS;
	}

	public static LinkResult linkWithResult(ServerLevel level, @Nullable UUID playerId, ItemStack linker, ItemStack endpoint) {
		EnderLinkerStackState linkerState = LinkedStorageStackLifecycle.classifyLinker(linker);
		LinkedStorageEndpointStackState endpointState = LinkedStorageStackLifecycle.classifyEndpoint(endpoint);
		return linkWithResult(level, playerId, linker, endpoint, endpointState == LinkedStorageEndpointStackState.UNLINKED,
				LinkedStorageStackData.getEndpoint(endpoint), LinkedStorageEndpointAdapters.find(endpoint).orElse(null), linkerState);
	}

	public static LinkResult linkWithResult(ServerLevel level, @Nullable UUID playerId, ItemStack linker, ILinkedStorageBlockEndpoint endpoint) {
		EnderLinkerStackState linkerState = LinkedStorageStackLifecycle.classifyLinker(linker);
		LinkedStorageEndpointData endpointData = endpoint.getLinkedStorageEndpointData();
		return linkWithResult(level, playerId, linker, endpoint, endpointData == null, endpointData, endpoint.getLinkedStorageBlockEndpointAdapter(),
				linkerState);
	}

	private static <E> LinkResult linkWithResult(ServerLevel level, @Nullable UUID playerId, ItemStack linker, E endpoint, boolean endpointIsUnlinked,
			@Nullable LinkedStorageEndpointData endpointData, @Nullable ILinkedStorageEndpointAdapter<E> adapter, EnderLinkerStackState linkerState) {
		requireLinkableLinker(linkerState);
		if (linkerState == EnderLinkerStackState.UNLINKED && endpointIsUnlinked) {
			return adapter == null ? LinkResult.UNSUPPORTED_ENDPOINT : createGroup(level, playerId, linker, endpoint, adapter);
		}
		if (linkerState == EnderLinkerStackState.TARGET && endpointIsUnlinked) {
			return adapter == null ? LinkResult.UNSUPPORTED_ENDPOINT : addEndpoint(level, linker, endpoint, adapter);
		}
		if (linkerState == EnderLinkerStackState.UNLINKED && endpointData != null) {
			return bindLinkerToEndpoint(level, linker, endpointData);
		}
		return endpointData != null ? LinkResult.ALREADY_LINKED : LinkResult.UNSUPPORTED_ENDPOINT;
	}

	public static boolean isLinkCandidate(ItemStack endpoint) {
		LinkedStorageEndpointStackState endpointState = LinkedStorageStackLifecycle.classifyEndpoint(endpoint);
		return endpointState == LinkedStorageEndpointStackState.ENDPOINT
				|| endpointState == LinkedStorageEndpointStackState.UNLINKED && LinkedStorageEndpointAdapters.find(endpoint).isPresent();
	}

	public static boolean canAddEndpoint(ServerLevel level, ItemStack linker, ItemStack endpoint) {
		return getAddEndpointResult(level, linker, endpoint) == LinkResult.SUCCESS;
	}

	public static LinkResult getAddEndpointResult(ServerLevel level, ItemStack linker, ItemStack endpoint) {
		if (LinkedStorageStackLifecycle.classifyEndpoint(endpoint) != LinkedStorageEndpointStackState.UNLINKED) {
			return LinkResult.ALREADY_LINKED;
		}
		Optional<ILinkedStorageItemEndpointAdapter> adapter = LinkedStorageEndpointAdapters.find(endpoint);
		if (adapter.isEmpty()) {
			return LinkResult.UNSUPPORTED_ENDPOINT;
		}
		UUID groupId = getLinkerTargetGroup(level, linker).orElseThrow();
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		LinkedStorageHostDescriptor hostDescriptor = manager.getHostDescriptor(groupId).orElseThrow();
		return getAddEndpointResult(level, endpoint, adapter.get(), groupId, manager, hostDescriptor);
	}

	private static <E> LinkResult getAddEndpointResult(ServerLevel level, E endpoint, ILinkedStorageEndpointAdapter<E> adapter, UUID groupId,
			LinkedStorageGroupManager manager, LinkedStorageHostDescriptor hostDescriptor) {
		if (!manager.usesHostFactory(groupId, adapter.factoryId())) {
			return LinkResult.INCOMPATIBLE_ENDPOINT;
		}
		return switch (adapter.getCompatibility(level, endpoint, hostDescriptor)) {
			case COMPATIBLE -> LinkResult.SUCCESS;
			case INCOMPATIBLE -> LinkResult.INCOMPATIBLE_ENDPOINT;
			case HAS_CONTENTS -> LinkResult.ENDPOINT_HAS_CONTENTS;
		};
	}

	private static Optional<UUID> getLinkerTargetGroup(ServerLevel level, ItemStack linker) {
		if (LinkedStorageStackLifecycle.classifyLinker(linker) == EnderLinkerStackState.TARGET) {
			return Optional.of(LinkedStorageStackData.getLinkerTarget(linker).groupId());
		}
		EnderLinkPendingCraftData pendingCraft = LinkedStorageStackData.getPendingCraft(linker);
		if (pendingCraft == null || !pendingCraft.resolvesToLinkerTarget() || pendingCraft.claimId() == null) {
			return Optional.empty();
		}
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		return manager.getActivePendingCraftClaim(pendingCraft.claimId()).filter(claim -> claim.plan() == pendingCraft.plan())
				.map(ActivePendingCraftClaim::groupId);
	}

	private static LinkResult bindLinkerToEndpoint(ServerLevel level, ItemStack linker, LinkedStorageEndpointData endpointData) {
		LinkedStorageStackData.setLinkerTarget(linker, createTarget(LinkedStorageGroupsSavedData.get(level).manager(), endpointData.groupId()));
		return LinkResult.SUCCESS;
	}

	public static Optional<ItemStack> createSecondaryEndpointCopy(ServerLevel level, ItemStack endpoint) {
		if (LinkedStorageStackLifecycle.classifyEndpoint(endpoint) != LinkedStorageEndpointStackState.ENDPOINT) {
			return Optional.empty();
		}
		LinkedStorageEndpointData sourceEndpoint = LinkedStorageStackData.getEndpoint(endpoint);
		ILinkedStorageItemEndpointAdapter adapter = LinkedStorageEndpointAdapters.find(endpoint).orElseThrow();
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		UUID endpointId = UUID.randomUUID();
		ItemStack copiedEndpoint = endpoint.copyWithCount(1);
		adapter.bindEndpoint(level, copiedEndpoint, new LinkedStorageEndpointData(sourceEndpoint.groupId(), endpointId));
		manager.registerEndpoint(sourceEndpoint.groupId(), endpointId);
		adapter.onEndpointLinked(level, copiedEndpoint);
		return Optional.of(copiedEndpoint);
	}

	private static <E> LinkResult createGroup(ServerLevel level, @Nullable UUID playerId, ItemStack linker, E endpoint,
			ILinkedStorageEndpointAdapter<E> adapter) {
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		UUID endpointId = UUID.randomUUID();
		UUID groupId = manager.createGroup(playerId == null ? UUID.randomUUID() : playerId, endpointId, adapter.createHostDescriptor(level, endpoint),
				adapter.copyCanonicalContents(level, endpoint));
		adapter.bindEndpoint(level, endpoint, new LinkedStorageEndpointData(groupId, endpointId));
		adapter.onEndpointLinked(level, endpoint);
		LinkedStorageStackData.setLinkerTarget(linker, createTarget(manager, groupId));
		return LinkResult.SUCCESS;
	}

	private static <E> LinkResult addEndpoint(ServerLevel level, ItemStack linker, E endpoint, ILinkedStorageEndpointAdapter<E> adapter) {
		EnderLinkerTargetData target = LinkedStorageStackData.getLinkerTarget(linker);
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		LinkedStorageHostDescriptor hostDescriptor = manager.getHostDescriptor(target.groupId()).orElseThrow();
		LinkResult addEndpointResult = getAddEndpointResult(level, endpoint, adapter, target.groupId(), manager, hostDescriptor);
		if (addEndpointResult != LinkResult.SUCCESS) {
			return addEndpointResult;
		}
		UUID endpointId = UUID.randomUUID();
		adapter.bindEndpoint(level, endpoint, new LinkedStorageEndpointData(target.groupId(), endpointId));
		manager.registerEndpoint(target.groupId(), endpointId);
		adapter.onEndpointLinked(level, endpoint);
		linker.shrink(1);
		return LinkResult.SUCCESS;
	}

	private static EnderLinkerTargetData createTarget(LinkedStorageGroupManager manager, UUID groupId) {
		Component groupName = manager.resolveVirtualHost(groupId).orElseThrow().getLinkedStorageDisplayName().orElse(Component.empty());
		return new EnderLinkerTargetData(groupId, groupName);
	}

	private static void requireLinkableLinker(EnderLinkerStackState linkerState) {
		if (linkerState == EnderLinkerStackState.PENDING_CRAFT) {
			throw new IllegalStateException("Cannot link an Ender Linker with an unfinished craft claim");
		}
	}

	public enum LinkResult {
		SUCCESS, ALREADY_LINKED, UNSUPPORTED_ENDPOINT, INCOMPATIBLE_ENDPOINT, ENDPOINT_HAS_CONTENTS
	}
}
