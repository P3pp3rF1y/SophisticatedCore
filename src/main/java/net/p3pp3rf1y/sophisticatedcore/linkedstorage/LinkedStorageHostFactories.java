package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class LinkedStorageHostFactories {
	private static final Map<ResourceLocation, ILinkedStorageHostFactory> FACTORIES = new HashMap<>();

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
}
