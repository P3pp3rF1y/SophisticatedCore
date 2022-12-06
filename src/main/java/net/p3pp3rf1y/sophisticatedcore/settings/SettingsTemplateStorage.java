package net.p3pp3rf1y.sophisticatedcore.settings;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SettingsTemplateStorage extends SavedData {
	private static final String SAVED_DATA_NAME = SophisticatedCore.MOD_ID + "_settings_templates";
	private Map<UUID, Map<Integer, CompoundTag>> playerTemplates = new HashMap<>();
	private static final SettingsTemplateStorage clientStorageCopy = new SettingsTemplateStorage();

	private SettingsTemplateStorage() {}

	private SettingsTemplateStorage(Map<UUID, Map<Integer, CompoundTag>> playerTemplates) {
		this.playerTemplates = playerTemplates;
	}

	public void putPlayerTemplate(Player player, int slot, CompoundTag settingsTag) {
		playerTemplates.computeIfAbsent(player.getUUID(), u -> new HashMap<>()).put(slot, settingsTag);
		setDirty();
	}

	public Map<Integer, CompoundTag> getPlayerTemplates(Player player) {
		return playerTemplates.getOrDefault(player.getUUID(), new HashMap<>());
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		NBTHelper.putMap(tag, "playerTemplates", playerTemplates, UUID::toString, slotTemplates -> NBTHelper.putMap(new CompoundTag(), "slotTemplates", slotTemplates, String::valueOf, settingsTag -> settingsTag));
		return tag;
	}

	public static SettingsTemplateStorage get() {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server != null) {
				ServerLevel overworld = server.getLevel(Level.OVERWORLD);
				//noinspection ConstantConditions - by this time overworld is loaded
				DimensionDataStorage storage = overworld.getDataStorage();
				return storage.computeIfAbsent(SettingsTemplateStorage::load, SettingsTemplateStorage::new, SAVED_DATA_NAME);
			}
		}
		return clientStorageCopy;
	}

	private static SettingsTemplateStorage load(CompoundTag tag) {
		return new SettingsTemplateStorage(NBTHelper.getMap(tag, "playerTemplates", UUID::fromString,
				(key, playerTemplatesTag) -> NBTHelper.getMap((CompoundTag) playerTemplatesTag, "slotTemplates", Integer::valueOf, (k, settingsTag) -> Optional.of((CompoundTag) settingsTag))
		).orElse(new HashMap<>()));
	}
}
