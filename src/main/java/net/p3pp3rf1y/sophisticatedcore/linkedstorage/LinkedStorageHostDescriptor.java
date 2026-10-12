package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record LinkedStorageHostDescriptor(ResourceLocation factoryId, CompoundTag virtualCarrier) {
	public static final String FACTORY_ID_TAG = "factoryId";
	public static final String VIRTUAL_CARRIER_TAG = "virtualCarrier";

	public LinkedStorageHostDescriptor {
		virtualCarrier = virtualCarrier.copy();
	}

	@Override
	public CompoundTag virtualCarrier() {
		return virtualCarrier.copy();
	}
}
