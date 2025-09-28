package net.p3pp3rf1y.sophisticatedcore.common;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HighlightRequestPayloadHandlerRegistry {
	private HighlightRequestPayloadHandlerRegistry() {}

	private static final Map<ResourceLocation, IHighlightRequestPayloadHandler<?>> registry = new HashMap<>();

	public static void register(IHighlightRequestPayloadHandler<?> handler) {
		registry.put(handler.id(), handler);
	}

	public static Optional<IHighlightRequestPayloadHandler<?>> get(ResourceLocation id) {
		return Optional.ofNullable(registry.get(id));
	}
}
