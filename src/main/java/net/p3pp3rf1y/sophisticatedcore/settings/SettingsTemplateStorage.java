package net.p3pp3rf1y.sophisticatedcore.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
//TODO after 1.22 remove support for legacy UUID deserialization via strings
public class SettingsTemplateStorage extends SavedData {
	private static final SavedDataType<SettingsTemplateStorage> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "settings_templates"), SettingsTemplateStorage::new,
			RecordCodecBuilder.create(builder -> builder.group(
					Codec.unboundedMap(CodecHelper.STRING_ENCODED_UUID, Codec.unboundedMap(ExtraCodecs.POSITIVE_INT, ContainerContents.SettingsData.CODEC))
							.fieldOf("playerTemplates").forGetter(storage -> storage.playerTemplates),
					Codec.unboundedMap(CodecHelper.STRING_ENCODED_UUID, Codec.unboundedMap(ExtraCodecs.NON_EMPTY_STRING, ContainerContents.SettingsData.CODEC))
							.fieldOf("playerNamedTemplates").forGetter(storage -> storage.playerNamedTemplates))
					.apply(builder, SettingsTemplateStorage::new)));

	private Map<UUID, Map<Integer, ContainerContents.SettingsData>> playerTemplates = new HashMap<>();
	private Map<UUID, Map<String, ContainerContents.SettingsData>> playerNamedTemplates = new HashMap<>();
	private static final SettingsTemplateStorage clientStorageCopy = new SettingsTemplateStorage();

	private SettingsTemplateStorage() {
	}

	private SettingsTemplateStorage(Map<UUID, Map<Integer, ContainerContents.SettingsData>> playerTemplates,
			Map<UUID, Map<String, ContainerContents.SettingsData>> playerNamedTemplates) {
		this.playerTemplates = playerTemplates;
		this.playerNamedTemplates = playerNamedTemplates;
	}

	public void putPlayerTemplate(Player player, int slot, ContainerContents.SettingsData data) {
		playerTemplates.computeIfAbsent(player.getUUID(), u -> new HashMap<>()).put(slot, data);
		setDirty();
	}

	public void putPlayerNamedTemplate(Player player, String name, ContainerContents.SettingsData data) {
		playerNamedTemplates.computeIfAbsent(player.getUUID(), u -> new TreeMap<>()).put(name, data);
		setDirty();
	}

	public Map<Integer, ContainerContents.SettingsData> getPlayerTemplates(Player player) {
		return playerTemplates.getOrDefault(player.getUUID(), new HashMap<>());
	}

	public Map<String, ContainerContents.SettingsData> getPlayerNamedTemplates(Player player) {
		return playerNamedTemplates.getOrDefault(player.getUUID(), new TreeMap<>());
	}

	public static SettingsTemplateStorage get() {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server != null) {
				ServerLevel overworld = server.getLevel(Level.OVERWORLD);
				// noinspection ConstantConditions - by this time overworld is loaded
				SavedDataStorage storage = overworld.getDataStorage();
				return storage.computeIfAbsent(TYPE);
			}
		}
		return clientStorageCopy;
	}

	public void clearPlayerTemplates(Player player) {
		playerTemplates.remove(player.getUUID());
		playerNamedTemplates.remove(player.getUUID());
		setDirty();
	}
}
