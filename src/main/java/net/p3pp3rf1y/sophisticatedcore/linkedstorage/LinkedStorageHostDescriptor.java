package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record LinkedStorageHostDescriptor(ResourceLocation factoryId, CompoundTag virtualCarrier) {
	public static final MapCodec<LinkedStorageHostDescriptor> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(ResourceLocation.CODEC.fieldOf("factory_id").forGetter(LinkedStorageHostDescriptor::factoryId),
					CompoundTag.CODEC.fieldOf("virtual_carrier").forGetter(LinkedStorageHostDescriptor::virtualCarrier))
			.apply(instance, LinkedStorageHostDescriptor::new));
	public static final Codec<LinkedStorageHostDescriptor> CODEC = MAP_CODEC.codec();

	public LinkedStorageHostDescriptor {
		virtualCarrier = virtualCarrier.copy();
	}

	@Override
	public CompoundTag virtualCarrier() {
		return virtualCarrier.copy();
	}
}
