package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record LinkedStorageEndpointData(UUID groupId, UUID endpointId) {
	private static final String GROUP_ID_TAG = "group_id";
	private static final String ENDPOINT_ID_TAG = "endpoint_id";

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putUUID(GROUP_ID_TAG, groupId);
		tag.putUUID(ENDPOINT_ID_TAG, endpointId);
		return tag;
	}

	public static LinkedStorageEndpointData load(CompoundTag tag) {
		return new LinkedStorageEndpointData(tag.getUUID(GROUP_ID_TAG), tag.getUUID(ENDPOINT_ID_TAG));
	}
}
