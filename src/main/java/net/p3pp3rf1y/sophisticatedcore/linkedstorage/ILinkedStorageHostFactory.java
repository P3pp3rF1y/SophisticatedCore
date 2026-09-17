package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;

@FunctionalInterface
public interface ILinkedStorageHostFactory {
	ILinkedStorageVirtualHost create(ILinkedStorageContents contents, CompoundTag virtualCarrier);
}
