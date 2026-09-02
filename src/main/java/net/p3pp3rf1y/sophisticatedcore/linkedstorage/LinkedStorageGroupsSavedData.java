package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LinkedStorageGroupsSavedData extends SavedData {
	private static final String SAVED_DATA_NAME = SophisticatedCore.MOD_ID + "_linked_storage_groups";
	private static final String GROUPS_TAG = "groups";
	private static final String ID_TAG = "id";
	private static final String REVISION_TAG = "revision";
	private static final String RENDER_REVISION_TAG = "render_revision";
	private static final String COLUMNS_TAKEN_TAG = "columns_taken";
	private static final String OWNER_ID_TAG = "owner_id";
	private static final String PRIMARY_ENDPOINT_ID_TAG = "primary_endpoint_id";
	private static final String ENDPOINTS_TAG = "endpoints";
	private static final String LAST_OPENED_BY_TAG = "last_opened_by";
	private static final String LAST_OPENED_AT_TAG = "last_opened_at";
	private static final String FACTORY_ID_TAG = "factory_id";
	private static final String VIRTUAL_CARRIER_TAG = "virtual_carrier";
	private static final String CONTENTS_TAG = "contents";
	private static final String ACTIVE_PENDING_CLAIMS_TAG = "active_pending_claims";
	private static final String CLAIM_ID_TAG = "claim_id";
	private static final String GROUP_ID_TAG = "group_id";
	private static final String ENDPOINT_ID_TAG = "endpoint_id";
	private static final String PLAN_KIND_TAG = "plan_kind";

	private final Map<UUID, LinkedStorageGroupRecord> groups;
	private final Map<UUID, ActivePendingCraftClaim> activePendingClaims;
	private final LinkedStorageGroupManager manager;

	LinkedStorageGroupsSavedData() {
		this(new HashMap<>(), new HashMap<>());
	}

	private LinkedStorageGroupsSavedData(Map<UUID, LinkedStorageGroupRecord> groups, Map<UUID, ActivePendingCraftClaim> activePendingClaims) {
		this.groups = groups;
		this.activePendingClaims = activePendingClaims;
		manager = new LinkedStorageGroupManager(this);
	}

	public static LinkedStorageGroupsSavedData get(ServerLevel level) {
		ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			throw new IllegalStateException("Linked storage groups require an Overworld");
		}
		DimensionDataStorage storage = overworld.getDataStorage();
		return storage.computeIfAbsent(LinkedStorageGroupsSavedData::load, LinkedStorageGroupsSavedData::new, SAVED_DATA_NAME);
	}

	public LinkedStorageGroupManager manager() {
		return manager;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag groupsTag = new ListTag();
		for (LinkedStorageGroupRecord group : groups.values()) {
			CompoundTag groupTag = new CompoundTag();
			groupTag.putUUID(ID_TAG, group.id());
			groupTag.putLong(REVISION_TAG, group.revision());
			groupTag.putLong(RENDER_REVISION_TAG, group.renderRevision());
			groupTag.putInt(COLUMNS_TAKEN_TAG, group.columnsTaken());
			groupTag.putUUID(OWNER_ID_TAG, group.ownerId());
			groupTag.putUUID(PRIMARY_ENDPOINT_ID_TAG, group.primaryEndpointId());
			ListTag endpointsTag = new ListTag();
			for (LinkedStorageEndpointRecord endpoint : group.endpoints()) {
				CompoundTag endpointTag = new CompoundTag();
				endpointTag.putUUID(ID_TAG, endpoint.endpointId());
				if (endpoint.lastOpenedBy() != null) {
					endpointTag.putUUID(LAST_OPENED_BY_TAG, endpoint.lastOpenedBy());
				}
				endpointTag.putLong(LAST_OPENED_AT_TAG, endpoint.lastOpenedAt());
				endpointsTag.add(endpointTag);
			}
			groupTag.put(ENDPOINTS_TAG, endpointsTag);
			groupTag.putString(FACTORY_ID_TAG, group.hostDescriptor().factoryId().toString());
			groupTag.put(VIRTUAL_CARRIER_TAG, group.hostDescriptor().virtualCarrier());
			groupTag.put(CONTENTS_TAG, group.contents().copy());
			groupsTag.add(groupTag);
		}
		tag.put(GROUPS_TAG, groupsTag);
		ListTag claimsTag = new ListTag();
		for (ActivePendingCraftClaim claim : activePendingClaims.values()) {
			CompoundTag claimTag = new CompoundTag();
			claimTag.putUUID(CLAIM_ID_TAG, claim.claimId());
			claimTag.putUUID(GROUP_ID_TAG, claim.groupId());
			claimTag.putUUID(ENDPOINT_ID_TAG, claim.endpointId());
			claimTag.putString(PLAN_KIND_TAG, claim.plan().getSerializedName());
			claimsTag.add(claimTag);
		}
		tag.put(ACTIVE_PENDING_CLAIMS_TAG, claimsTag);
		return tag;
	}

	static LinkedStorageGroupsSavedData load(CompoundTag tag) {
		Map<UUID, LinkedStorageGroupRecord> groups = new HashMap<>();
		Map<UUID, ActivePendingCraftClaim> activePendingClaims = new HashMap<>();
		for (Tag value : tag.getList(GROUPS_TAG, Tag.TAG_COMPOUND)) {
			LinkedStorageGroupRecord group = loadGroup((CompoundTag) value);
			groups.put(group.id(), group);
		}
		for (Tag value : tag.getList(ACTIVE_PENDING_CLAIMS_TAG, Tag.TAG_COMPOUND)) {
			ActivePendingCraftClaim claim = loadActivePendingClaim((CompoundTag) value);
			activePendingClaims.put(claim.claimId(), claim);
		}
		return new LinkedStorageGroupsSavedData(groups, activePendingClaims);
	}

	private static ActivePendingCraftClaim loadActivePendingClaim(CompoundTag tag) {
		return new ActivePendingCraftClaim(tag.getUUID(CLAIM_ID_TAG), tag.getUUID(GROUP_ID_TAG), tag.getUUID(ENDPOINT_ID_TAG),
				EnderLinkPendingCraftPlan.fromSerializedName(tag.getString(PLAN_KIND_TAG)));
	}

	private static LinkedStorageGroupRecord loadGroup(CompoundTag tag) {
		List<LinkedStorageEndpointRecord> endpoints = new ArrayList<>();
		for (Tag endpointValue : tag.getList(ENDPOINTS_TAG, Tag.TAG_COMPOUND)) {
			CompoundTag endpointTag = (CompoundTag) endpointValue;
			endpoints.add(new LinkedStorageEndpointRecord(endpointTag.getUUID(ID_TAG),
					endpointTag.hasUUID(LAST_OPENED_BY_TAG) ? endpointTag.getUUID(LAST_OPENED_BY_TAG) : null, endpointTag.getLong(LAST_OPENED_AT_TAG)));
		}
		ResourceLocation factoryId = new ResourceLocation(tag.getString(FACTORY_ID_TAG));
		LinkedStorageHostDescriptor descriptor = new LinkedStorageHostDescriptor(factoryId, tag.getCompound(VIRTUAL_CARRIER_TAG));
		return new LinkedStorageGroupRecord(tag.getUUID(ID_TAG), tag.getUUID(OWNER_ID_TAG), tag.getUUID(PRIMARY_ENDPOINT_ID_TAG), endpoints, descriptor,
				tag.getCompound(CONTENTS_TAG), tag.getLong(REVISION_TAG), tag.getLong(RENDER_REVISION_TAG), tag.getInt(COLUMNS_TAKEN_TAG));
	}

	Optional<LinkedStorageGroupRecord> findGroup(UUID groupId) {
		return Optional.ofNullable(groups.get(groupId));
	}

	void addGroup(LinkedStorageGroupRecord group) {
		groups.put(group.id(), group);
		setDirty();
	}

	boolean removeGroup(UUID groupId) {
		if (groups.remove(groupId) == null) {
			return false;
		}
		setDirty();
		return true;
	}

	void addActivePendingClaim(ActivePendingCraftClaim claim) {
		activePendingClaims.put(claim.claimId(), claim);
		setDirty();
	}

	Optional<ActivePendingCraftClaim> getActivePendingClaim(UUID claimId) {
		return Optional.ofNullable(activePendingClaims.get(claimId));
	}

	boolean removeActivePendingClaim(UUID claimId) {
		if (activePendingClaims.remove(claimId) == null) {
			return false;
		}
		setDirty();
		return true;
	}
}
