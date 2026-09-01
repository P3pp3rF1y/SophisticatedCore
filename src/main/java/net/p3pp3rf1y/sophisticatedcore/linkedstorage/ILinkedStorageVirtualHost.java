package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public interface ILinkedStorageVirtualHost {
	default void onLinkedStorageContentsChanged() {
	}

	default void onLinkedStorageLayoutChanged() {
	}

	default void onVirtualCarrierChanged(CompoundTag virtualCarrier) {
	}

	default Optional<CompoundTag> getVirtualCarrierSnapshot() {
		return Optional.empty();
	}

	default Optional<Component> getLinkedStorageDisplayName() {
		return Optional.empty();
	}
}
