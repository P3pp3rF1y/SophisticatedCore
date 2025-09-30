package net.p3pp3rf1y.sophisticatedcore.common;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ItemActionHandlerRegistry {
	private ItemActionHandlerRegistry() {
	}

	private static final Map<ResourceLocation, IItemActionPayloadHandler<?>> registry = new HashMap<>();

	public static void register(IItemActionPayloadHandler<?> handler) {
		registry.put(handler.id(), handler);
	}

	public static Optional<IItemActionPayloadHandler<?>> get(ResourceLocation id) {
		return Optional.ofNullable(registry.get(id));
	}
}
