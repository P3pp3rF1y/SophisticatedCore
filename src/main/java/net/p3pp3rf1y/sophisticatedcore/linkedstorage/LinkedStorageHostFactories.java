package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class LinkedStorageHostFactories {
	private static final Map<Identifier, ILinkedStorageHostFactory> FACTORIES = new HashMap<>();
	private static final Map<Identifier, Set<Identifier>> COMPATIBLE_FACTORIES = new ConcurrentHashMap<>();

	private LinkedStorageHostFactories() {
	}

	public static void register(Identifier factoryId, ILinkedStorageHostFactory factory) {
		if (FACTORIES.putIfAbsent(factoryId, factory) != null) {
			throw new IllegalStateException("Linked storage host factory already registered: " + factoryId);
		}
	}

	public static Optional<ILinkedStorageHostFactory> get(Identifier factoryId) {
		return Optional.ofNullable(FACTORIES.get(factoryId));
	}

	public static void registerCompatibility(Identifier firstFactoryId, Identifier secondFactoryId) {
		COMPATIBLE_FACTORIES.computeIfAbsent(firstFactoryId, ignored -> ConcurrentHashMap.newKeySet()).add(secondFactoryId);
		COMPATIBLE_FACTORIES.computeIfAbsent(secondFactoryId, ignored -> ConcurrentHashMap.newKeySet()).add(firstFactoryId);
	}

	public static boolean areCompatible(Identifier endpointFactoryId, Identifier hostFactoryId) {
		return endpointFactoryId.equals(hostFactoryId) || COMPATIBLE_FACTORIES.getOrDefault(endpointFactoryId, Set.of()).contains(hostFactoryId);
	}
}
