package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class LinkedStorageHostFactories {
	private static final Map<ResourceLocation, ILinkedStorageHostFactory> FACTORIES = new HashMap<>();
	private static final Map<ResourceLocation, Set<ResourceLocation>> COMPATIBLE_FACTORIES = new ConcurrentHashMap<>();

	private LinkedStorageHostFactories() {
	}

	public static void register(ResourceLocation factoryId, ILinkedStorageHostFactory factory) {
		if (FACTORIES.putIfAbsent(factoryId, factory) != null) {
			throw new IllegalStateException("Linked storage host factory already registered: " + factoryId);
		}
	}

	public static Optional<ILinkedStorageHostFactory> get(ResourceLocation factoryId) {
		return Optional.ofNullable(FACTORIES.get(factoryId));
	}

	public static void registerCompatibility(ResourceLocation firstFactoryId, ResourceLocation secondFactoryId) {
		COMPATIBLE_FACTORIES.computeIfAbsent(firstFactoryId, ignored -> ConcurrentHashMap.newKeySet()).add(secondFactoryId);
		COMPATIBLE_FACTORIES.computeIfAbsent(secondFactoryId, ignored -> ConcurrentHashMap.newKeySet()).add(firstFactoryId);
	}

	public static boolean areCompatible(ResourceLocation endpointFactoryId, ResourceLocation hostFactoryId) {
		return endpointFactoryId.equals(hostFactoryId) || COMPATIBLE_FACTORIES.getOrDefault(endpointFactoryId, Set.of()).contains(hostFactoryId);
	}
}
