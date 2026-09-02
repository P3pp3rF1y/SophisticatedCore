package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public record EnderLinkerTargetData(UUID groupId, Component groupName) {
	private static final String GROUP_ID_TAG = "group_id";
	private static final String GROUP_NAME_TAG = "group_name";

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putUUID(GROUP_ID_TAG, groupId);
		tag.putString(GROUP_NAME_TAG, Component.Serializer.toJson(groupName));
		return tag;
	}

	public static EnderLinkerTargetData load(CompoundTag tag) {
		return new EnderLinkerTargetData(tag.getUUID(GROUP_ID_TAG), Component.Serializer.fromJson(tag.getString(GROUP_NAME_TAG)));
	}
}
