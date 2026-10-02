package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import javax.annotation.Nullable;

import java.util.Optional;
import java.util.UUID;

public record LinkedStorageEndpointRecord(UUID endpointId, @Nullable UUID lastOpenedBy, long lastOpenedAt) {
	static final Codec<LinkedStorageEndpointRecord> CODEC = RecordCodecBuilder
			.create(instance -> instance
					.group(UUIDUtil.CODEC.fieldOf("id").forGetter(LinkedStorageEndpointRecord::endpointId),
							UUIDUtil.CODEC.optionalFieldOf("last_opened_by").forGetter(record -> Optional.ofNullable(record.lastOpenedBy)),
							Codec.LONG.optionalFieldOf("last_opened_at", 0L).forGetter(LinkedStorageEndpointRecord::lastOpenedAt))
					.apply(instance,
							(endpointId, lastOpenedBy, lastOpenedAt) -> new LinkedStorageEndpointRecord(endpointId, lastOpenedBy.orElse(null), lastOpenedAt)));

	static LinkedStorageEndpointRecord create(UUID endpointId) {
		return new LinkedStorageEndpointRecord(endpointId, null, -1L);
	}

	LinkedStorageEndpointRecord withLastOpenedBy(UUID playerId, long gameTime) {
		return new LinkedStorageEndpointRecord(endpointId, playerId, gameTime);
	}
}
