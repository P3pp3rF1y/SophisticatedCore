package net.p3pp3rf1y.sophisticatedcore.compat.create;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MountedStorageData extends SavedData implements IStorageSavedData {
	private static final String SAVED_DATA_PREFIX = SophisticatedCore.MOD_ID + "_mounted/";

	private CompoundTag movingStorageContents = new CompoundTag();

	private boolean toRemove = false;
	private static final Cache<UUID, MountedStorageData> clientStorageCopy = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
	private final Set<UUID> updatedStorageSettingsFlags = new HashSet<>();

	private MountedStorageData() {
	}

	public static MountedStorageData get(UUID storageId) {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server != null) {
				ServerLevel overworld = server.getLevel(Level.OVERWORLD);
				// noinspection ConstantConditions - by this time overworld is loaded
				DimensionDataStorage storage = overworld.getDataStorage();
				return storage.computeIfAbsent(new Factory<>(MountedStorageData::new, MountedStorageData::load), SAVED_DATA_PREFIX + storageId);
			}
		}
		MountedStorageData storageData = clientStorageCopy.getIfPresent(storageId);
		if (storageData == null) {
			storageData = new MountedStorageData();
			clientStorageCopy.put(storageId, storageData);
		}

		return storageData;
	}

	public static MountedStorageData load(CompoundTag nbt, HolderLookup.Provider registries) {
		MountedStorageData storageData = new MountedStorageData();
		storageData.movingStorageContents = nbt;
		return storageData;
	}

	@Override
	public CompoundTag save(CompoundTag compound, HolderLookup.Provider registries) {
		if (movingStorageContents != null) {
			return movingStorageContents;
		}
		return new CompoundTag();
	}

	public void removeStorageContents() {
		toRemove = true;
		setDirty();
	}

	@Override
	public void save(File file, HolderLookup.Provider registries) {
		if (toRemove) {
			file.delete();
		} else {
			try {
				Files.createDirectories(file.toPath().getParent());
			} catch (IOException e) {
				SophisticatedCore.LOGGER.error("Failed to create directories for moving storage data", e);
			}
			super.save(file, registries);
		}
	}

	public void setContents(UUID storageUuid, CompoundTag contents) {
		for (String key : contents.getAllKeys()) {
			// noinspection ConstantConditions - the key is one of the tag keys so there's no reason it wouldn't exist here
			movingStorageContents.put(key, contents.get(key));

			if (key.equals(IStorageWrapper.SETTINGS_TAG)) {
				updatedStorageSettingsFlags.add(storageUuid);
			}
		}
		setDirty();
	}

	public CompoundTag getContents() {
		return movingStorageContents;
	}

	public void setContents(CompoundTag contents) {
		movingStorageContents = contents;
		setDirty();
	}

	@Override
	public void markChanged() {
		setDirty();
	}

	public boolean removeUpdatedStorageSettingsFlag(UUID backpackUuid) {
		return updatedStorageSettingsFlags.remove(backpackUuid);
	}
}
