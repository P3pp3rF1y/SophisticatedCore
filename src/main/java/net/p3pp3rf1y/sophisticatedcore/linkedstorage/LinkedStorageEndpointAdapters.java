package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LinkedStorageEndpointAdapters {
	private static final List<ILinkedStorageItemEndpointAdapter> ADAPTERS = new ArrayList<>();

	private LinkedStorageEndpointAdapters() {
	}

	public static void register(ILinkedStorageItemEndpointAdapter adapter) {
		if (!ADAPTERS.contains(adapter)) {
			ADAPTERS.add(adapter);
		}
	}

	public static Optional<ILinkedStorageItemEndpointAdapter> find(ItemStack stack) {
		return ADAPTERS.stream().filter(adapter -> adapter.supports(stack)).findFirst();
	}
}
