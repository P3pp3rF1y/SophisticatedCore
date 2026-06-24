package net.p3pp3rf1y.sophisticatedcore.compat.create;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageSavedData;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;

import java.util.*;

public class MountedStorageData extends SavedData implements IStorageSavedData {
	private static final SavedDataType<MountedStorageData> TYPE = new SavedDataType<>(SophisticatedCore.MOD_ID + "_mounted_storage", MountedStorageData::new,
			RecordCodecBuilder.create(builder -> builder.group(Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), CompoundTag.CODEC)
					.fieldOf("storageContents").forGetter(storage -> storage.mountedStorageContents)).apply(builder, MountedStorageData::new)));

	private static final MountedStorageData clientStorageCopy = new MountedStorageData();

	private final Map<UUID, CompoundTag> mountedStorageContents = new HashMap<>();
	private final Set<UUID> updatedStorageSettingsFlags = new HashSet<>();

	private MountedStorageData(Map<UUID, CompoundTag> mountedStorageContents) {
		this.mountedStorageContents.putAll(mountedStorageContents);
	}

	private MountedStorageData() {
	}

	public static MountedStorageData get() {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server != null) {
				ServerLevel overworld = server.getLevel(Level.OVERWORLD);
				// noinspection ConstantConditions - by this time overworld is loaded
				DimensionDataStorage storage = overworld.getDataStorage();
				return storage.computeIfAbsent(TYPE);
			}
		}
		return clientStorageCopy;
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
		for (String key : contents.keySet()) {
			// noinspection ConstantConditions - the key is one of the tag keys so there's no reason it wouldn't exist here
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
