package net.p3pp3rf1y.sophisticatedcore.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

import javax.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ControllerStorageKey(BlockPos position, @Nullable UUID groupId) {
	public static final Codec<ControllerStorageKey> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(BlockPos.CODEC.fieldOf("position").forGetter(ControllerStorageKey::position),
					UUIDUtil.CODEC.optionalFieldOf("groupId").forGetter(key -> Optional.ofNullable(key.groupId)))
			.apply(instance, (position, groupId) -> new ControllerStorageKey(position, groupId.orElse(null))));

	public ControllerStorageKey(BlockPos position) {
		this(position, null);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ControllerStorageKey other)) {
			return false;
		}
		return groupId == null && other.groupId == null ? position.equals(other.position) : groupId != null && groupId.equals(other.groupId);
	}

	@Override
	public int hashCode() {
		return groupId == null ? position.hashCode() : Objects.hash(groupId);
	}
}
