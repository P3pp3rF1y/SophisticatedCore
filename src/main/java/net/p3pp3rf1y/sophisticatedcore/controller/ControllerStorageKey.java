package net.p3pp3rf1y.sophisticatedcore.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class ControllerStorageKey {
	public static final Codec<ControllerStorageKey> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(BlockPos.CODEC.fieldOf("position").forGetter(ControllerStorageKey::position),
					UUIDUtil.CODEC.optionalFieldOf("groupId").forGetter(key -> Optional.ofNullable(key.groupId)))
			.apply(instance, (position, groupId) -> new ControllerStorageKey(position, groupId.orElse(null))));
	private final BlockPos position;
	@Nullable
	private final UUID groupId;

	public ControllerStorageKey(BlockPos position, @Nullable UUID groupId) {
		this.position = position;
		this.groupId = groupId;
	}

	public ControllerStorageKey(BlockPos position) {
		this(position, null);
	}

	public BlockPos position() {
		return position;
	}

	@Nullable
	public UUID groupId() {
		return groupId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof ControllerStorageKey other)) {
			return false;
		}
		if (groupId != null && other.groupId != null) {
			return groupId.equals(other.groupId);
		}
		return groupId == null && other.groupId == null && position.equals(other.position);
	}

	@Override
	public int hashCode() {
		return groupId != null ? groupId.hashCode() : position.hashCode();
	}
}
