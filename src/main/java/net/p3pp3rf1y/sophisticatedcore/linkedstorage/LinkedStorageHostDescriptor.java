package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

public record LinkedStorageHostDescriptor(Identifier factoryId, CompoundTag virtualCarrier) {
	public LinkedStorageHostDescriptor {
		virtualCarrier = virtualCarrier.copy();
	}

	@Override
	public CompoundTag virtualCarrier() {
		return virtualCarrier.copy();
	}
}
