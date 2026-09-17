package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;

import java.util.*;

public class LinkedStorageGroupsSavedData extends SavedData {
	static final Codec<LinkedStorageGroupsSavedData> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(Codec.list(SerializedGroup.CODEC).optionalFieldOf("groups", List.of()).forGetter(LinkedStorageGroupsSavedData::serializeGroups),
					Codec.list(ActivePendingCraftClaim.CODEC).optionalFieldOf("active_pending_claims", List.of())
							.forGetter(LinkedStorageGroupsSavedData::serializeActivePendingClaims))
			.apply(instance, LinkedStorageGroupsSavedData::fromSerializedData));
	private static final SavedDataType<LinkedStorageGroupsSavedData> TYPE = new SavedDataType<>(SophisticatedCore.MOD_ID + "_linked_storage_groups",
			LinkedStorageGroupsSavedData::new, CODEC);

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
		return storage.computeIfAbsent(TYPE);
	}

	public LinkedStorageGroupManager manager() {
		return manager;
	}

	public CompoundTag save() {
		return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
	}

	static LinkedStorageGroupsSavedData load(CompoundTag tag) {
		return CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
	}

	private static LinkedStorageGroupsSavedData fromSerializedData(List<SerializedGroup> serializedGroups, List<ActivePendingCraftClaim> activePendingClaims) {
		Map<UUID, LinkedStorageGroupRecord> groups = new HashMap<>();
		Map<UUID, ActivePendingCraftClaim> claims = new HashMap<>();
		serializedGroups.forEach(group -> groups.put(group.id(), group.toGroupRecord()));
		activePendingClaims.forEach(claim -> claims.put(claim.claimId(), claim));
		return new LinkedStorageGroupsSavedData(groups, claims);
	}

	private List<SerializedGroup> serializeGroups() {
		return groups.values().stream().map(SerializedGroup::fromGroupRecord).toList();
	}

	private List<ActivePendingCraftClaim> serializeActivePendingClaims() {
		return List.copyOf(activePendingClaims.values());
	}

	private record SerializedGroup(UUID id, UUID ownerId, UUID primaryEndpointId, List<LinkedStorageEndpointRecord> endpoints,
			LinkedStorageHostDescriptor hostDescriptor, ContainerContents contents, long revision, long renderRevision, int columnsTaken) {
		private static final Codec<SerializedGroup> CODEC = RecordCodecBuilder.create(instance -> instance
				.group(UUIDUtil.CODEC.fieldOf("id").forGetter(SerializedGroup::id), UUIDUtil.CODEC.fieldOf("owner_id").forGetter(SerializedGroup::ownerId),
						UUIDUtil.CODEC.fieldOf("primary_endpoint_id").forGetter(SerializedGroup::primaryEndpointId),
						Codec.list(LinkedStorageEndpointRecord.CODEC).fieldOf("endpoints").forGetter(SerializedGroup::endpoints),
						LinkedStorageHostDescriptor.MAP_CODEC.forGetter(SerializedGroup::hostDescriptor),
						ContainerContents.CODEC.fieldOf("contents").forGetter(SerializedGroup::contents),
						Codec.LONG.optionalFieldOf("revision", 0L).forGetter(SerializedGroup::revision),
						Codec.LONG.optionalFieldOf("render_revision", 0L).forGetter(SerializedGroup::renderRevision),
						Codec.INT.optionalFieldOf("columns_taken", 0).forGetter(SerializedGroup::columnsTaken))
				.apply(instance, SerializedGroup::new));

		private static SerializedGroup fromGroupRecord(LinkedStorageGroupRecord group) {
			return new SerializedGroup(group.id(), group.ownerId(), group.primaryEndpointId(), group.endpoints(), group.hostDescriptor(), group.contents(),
					group.revision(), group.renderRevision(), group.columnsTaken());
		}

		private LinkedStorageGroupRecord toGroupRecord() {
			return new LinkedStorageGroupRecord(id, ownerId, primaryEndpointId, endpoints, hostDescriptor, contents, revision, renderRevision, columnsTaken);
		}
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
