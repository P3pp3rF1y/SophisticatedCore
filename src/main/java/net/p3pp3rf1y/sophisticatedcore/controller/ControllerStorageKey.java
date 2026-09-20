package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

import java.util.Objects;
import java.util.UUID;

public record ControllerStorageKey(BlockPos position, @Nullable UUID groupId) {
	private static final String POSITION_TAG = "position";
	private static final String GROUP_ID_TAG = "groupId";

	public ControllerStorageKey(BlockPos position) {
		this(position, null);
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putLong(POSITION_TAG, position.asLong());
		if (groupId != null) {
			tag.putUUID(GROUP_ID_TAG, groupId);
		}
		return tag;
	}

	public static ControllerStorageKey load(CompoundTag tag) {
		return new ControllerStorageKey(BlockPos.of(tag.getLong(POSITION_TAG)), tag.hasUUID(GROUP_ID_TAG) ? tag.getUUID(GROUP_ID_TAG) : null);
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
