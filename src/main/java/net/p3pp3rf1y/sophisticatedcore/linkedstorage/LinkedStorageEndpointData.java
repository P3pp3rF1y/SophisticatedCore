package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record LinkedStorageEndpointData(UUID groupId, UUID endpointId) {
	public static final Codec<LinkedStorageEndpointData> CODEC = RecordCodecBuilder
			.create(instance -> instance
					.group(UUIDUtil.CODEC.fieldOf("group_id").forGetter(LinkedStorageEndpointData::groupId),
							UUIDUtil.CODEC.fieldOf("endpoint_id").forGetter(LinkedStorageEndpointData::endpointId))
					.apply(instance, LinkedStorageEndpointData::new));
	public static final StreamCodec<ByteBuf, LinkedStorageEndpointData> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC,
			LinkedStorageEndpointData::groupId, UUIDUtil.STREAM_CODEC, LinkedStorageEndpointData::endpointId, LinkedStorageEndpointData::new);
}
