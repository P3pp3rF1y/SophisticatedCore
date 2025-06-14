package net.p3pp3rf1y.sophisticatedcore.compat.create;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageSavedData;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;

import java.util.*;

public class MountedStorageData extends SavedData implements IStorageSavedData {
	private static final String SAVED_DATA_NAME = SophisticatedCore.MOD_ID + "_mounted_storage";
	private static final String STORAGE_CONTENTS_TAG = "storageContents";
	private static final MountedStorageData clientStorageCopy = new MountedStorageData();

	private final Map<UUID, CompoundTag> mountedStorageContents = new HashMap<>();
	private final Set<UUID> updatedStorageSettingsFlags = new HashSet<>();

	private MountedStorageData() {
	}

	public static MountedStorageData get() {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server != null) {
				ServerLevel overworld = server.getLevel(Level.OVERWORLD);
				//noinspection ConstantConditions - by this time overworld is loaded
				DimensionDataStorage storage = overworld.getDataStorage();
				return storage.computeIfAbsent(new Factory<>(MountedStorageData::new, MountedStorageData::load), SAVED_DATA_NAME);
			}
		}
		return clientStorageCopy;
	}

	public static MountedStorageData load(CompoundTag nbt, HolderLookup.Provider registries) {
		MountedStorageData storageData = new MountedStorageData();
		storageData.readStorageContents(nbt);
		return storageData;
	}

	private void readStorageContents(CompoundTag nbt) {
		mountedStorageContents.clear();
		ListTag list = nbt.getList(STORAGE_CONTENTS_TAG, Tag.TAG_COMPOUND);
		for (Tag storageNbt : list) {
			CompoundTag uuidContentsPair = (CompoundTag) storageNbt;
			UUID uuid = NbtUtils.loadUUID(Objects.requireNonNull(uuidContentsPair.get("uuid")));
			CompoundTag contents = uuidContentsPair.getCompound("contents");
			mountedStorageContents.put(uuid, contents);
		}
	}

	@Override
	public CompoundTag save(CompoundTag compound, HolderLookup.Provider registries) {
		CompoundTag ret = new CompoundTag();
		writeStorageContents(ret);
		return ret;
	}

	private void writeStorageContents(CompoundTag ret) {
		ListTag list = new ListTag();
		for (Map.Entry<UUID, CompoundTag> entry : mountedStorageContents.entrySet()) {
			CompoundTag uuidContentsPair = new CompoundTag();
			uuidContentsPair.putUUID("uuid", entry.getKey());
			uuidContentsPair.put("contents", entry.getValue());
			list.add(uuidContentsPair);
		}
		ret.put(STORAGE_CONTENTS_TAG, list);
		setDirty();
	}

	@Override
	public CompoundTag getContents(UUID storageId) {
		return mountedStorageContents.computeIfAbsent(storageId, k -> new CompoundTag());
	}

	public void removeStorageContents(UUID storageId) {
		mountedStorageContents.remove(storageId);
		setDirty();
	}

	public void setContentsClient(UUID storageId, CompoundTag contents) {
		for (String key : contents.getAllKeys()) {
			//noinspection ConstantConditions - the key is one of the tag keys so there's no reason it wouldn't exist here
			getContents(storageId).put(key, contents.get(key));

			if (key.equals(IStorageWrapper.SETTINGS_TAG)) {
				updatedStorageSettingsFlags.add(storageId);
			}
		}
		setDirty();
	}

	public void setContents(UUID storageId, CompoundTag contents) {
		mountedStorageContents.put(storageId, contents);
		setDirty();
	}

	@Override
	public void markChanged() {
		setDirty();
	}

	public boolean removeUpdatedStorageSettingsFlag(UUID storageId) {
		return updatedStorageSettingsFlags.remove(storageId);
	}
}
