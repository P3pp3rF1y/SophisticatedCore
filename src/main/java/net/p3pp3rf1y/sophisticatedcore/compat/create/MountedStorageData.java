package net.p3pp3rf1y.sophisticatedcore.compat.create;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;

import java.util.*;
//TODO after 1.22 remove support for legacy UUID deserialization via strings
public class MountedStorageData extends SavedData implements IStorageSavedData {
	private static final SavedDataType<MountedStorageData> TYPE = new SavedDataType<>(SophisticatedCore.MOD_ID + "_mounted_storage", MountedStorageData::new,
			RecordCodecBuilder.create(
					builder -> builder.group(
							Codec.unboundedMap(CodecHelper.STRING_ENCODED_UUID, ContainerContents.CODEC)
									.fieldOf("storageContents").forGetter(storage -> storage.mountedStorageContents)
					).apply(builder, MountedStorageData::new)
			));

	private static final MountedStorageData clientStorageCopy = new MountedStorageData();

	private final Map<UUID, ContainerContents> mountedStorageContents = new HashMap<>();
	private final Set<UUID> updatedStorageSettingsFlags = new HashSet<>();

	private MountedStorageData(Map<UUID, ContainerContents> mountedStorageContents) {
		this.mountedStorageContents.putAll(mountedStorageContents);
	}

	private MountedStorageData() {
	}

	public static MountedStorageData get() {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server != null) {
				ServerLevel overworld = server.getLevel(Level.OVERWORLD);
				//noinspection ConstantConditions - by this time overworld is loaded
				DimensionDataStorage storage = overworld.getDataStorage();
				return storage.computeIfAbsent(TYPE);
			}
		}
		return clientStorageCopy;
	}

	@Override
	public ContainerContents getContents(UUID storageId) {
		return mountedStorageContents.computeIfAbsent(storageId, k -> new ContainerContents());
	}

	public void removeStorageContents(UUID storageId) {
		mountedStorageContents.remove(storageId);
		setDirty();
	}

	public void setContentsClient(UUID storageId, ContainerContents contents) {
		if (!mountedStorageContents.containsKey(storageId)) {
			mountedStorageContents.put(storageId, contents);
			updatedStorageSettingsFlags.add(storageId);
		} else {
			ContainerContents currentContents = mountedStorageContents.get(storageId);
			ContainerContents.SettingsData previousSettings = currentContents.settings().copy();
			currentContents.reloadFrom(contents);
			if (!currentContents.settings().equals(previousSettings)) {
				updatedStorageSettingsFlags.add(storageId);
			}
		}
		setDirty();
	}

	public void setContents(UUID storageId, ContainerContents contents) {
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
