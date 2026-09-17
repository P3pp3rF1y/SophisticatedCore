package net.p3pp3rf1y.sophisticatedcore.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
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
			tag.store(GROUP_ID_TAG, UUIDUtil.CODEC, groupId);
		}
		return tag;
	}

	public static ControllerStorageKey load(CompoundTag tag) {
		return new ControllerStorageKey(BlockPos.of(tag.getLongOr(POSITION_TAG, 0)), tag.read(GROUP_ID_TAG, UUIDUtil.CODEC).orElse(null));
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
