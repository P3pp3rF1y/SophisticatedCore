package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LinkedStorageGroupsSavedData extends SavedData {
	private static final String LEGACY_SAVED_DATA_NAME = SophisticatedCore.MOD_ID + "_linked_storage_groups";
	static final Codec<LinkedStorageGroupsSavedData> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(Codec.list(SerializedGroup.CODEC).optionalFieldOf("groups", List.of()).forGetter(LinkedStorageGroupsSavedData::serializeGroups),
					Codec.list(ActivePendingCraftClaim.CODEC).optionalFieldOf("active_pending_claims", List.of())
							.forGetter(LinkedStorageGroupsSavedData::serializeActivePendingClaims))
			.apply(instance, LinkedStorageGroupsSavedData::fromSerializedData));
	private static final SavedDataType<LinkedStorageGroupsSavedData> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "linked_storage_groups"), LinkedStorageGroupsSavedData::new, CODEC);
	private static final SavedDataType<LinkedStorageGroupsSavedData> LEGACY_TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, LEGACY_SAVED_DATA_NAME), LinkedStorageGroupsSavedData::new, CODEC);

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
		SavedDataStorage storage = overworld.getDataStorage();
		LinkedStorageGroupsSavedData savedData = storage.get(TYPE);
		if (savedData != null) {
			return savedData;
		}
		LinkedStorageGroupsSavedData legacySavedData = storage.get(LEGACY_TYPE);
		if (legacySavedData != null) {
			SophisticatedCore.LOGGER.info("Migrating legacy linked storage groups to current saved data path");
			storage.set(TYPE, legacySavedData);
			return legacySavedData;
		}
		Optional<LinkedStorageGroupsSavedData> legacyRawSavedData = readLegacyRawSavedData(storage, level.getServer());
		if (legacyRawSavedData.isPresent()) {
			LinkedStorageGroupsSavedData migratedSavedData = legacyRawSavedData.get();
			SophisticatedCore.LOGGER.info("Migrating legacy linked storage groups to current saved data path");
			storage.set(TYPE, migratedSavedData);
			return migratedSavedData;
		}
		return storage.computeIfAbsent(TYPE);
	}

	private static Optional<LinkedStorageGroupsSavedData> readLegacyRawSavedData(SavedDataStorage storage, MinecraftServer server) {
		Path legacyDataFile = server.getWorldPath(LevelResource.DATA).resolve(LEGACY_SAVED_DATA_NAME + ".dat");
		if (!Files.exists(legacyDataFile)) {
			return Optional.empty();
		}

		try {
			CompoundTag tag = storage.readTagFromDisk(legacyDataFile, null, SharedConstants.getCurrentVersion().dataVersion().version());
			RegistryOps<Tag> ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
			return deserializeLegacySavedData(tag, ops);
		} catch (IOException e) {
			SophisticatedCore.LOGGER.error("Failed to read legacy linked storage groups from {}", legacyDataFile, e);
			return Optional.empty();
		}
	}

	static Optional<LinkedStorageGroupsSavedData> deserializeLegacySavedData(CompoundTag savedDataTag, DynamicOps<Tag> ops) {
		return savedDataTag.getCompound("data").flatMap(data -> CODEC.parse(ops, data).result());
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
		serializedGroups.forEach(group -> groups.put(group.id(), group.toGroupRecord()));
		Map<UUID, ActivePendingCraftClaim> claims = new HashMap<>();
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
