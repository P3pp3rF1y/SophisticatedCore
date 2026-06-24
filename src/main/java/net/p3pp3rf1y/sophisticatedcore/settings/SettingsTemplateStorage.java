package net.p3pp3rf1y.sophisticatedcore.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class SettingsTemplateStorage extends SavedData {
	private static final SavedDataType<SettingsTemplateStorage> TYPE = new SavedDataType<>(SophisticatedCore.MOD_ID + "_settings_templates",
			SettingsTemplateStorage::new,
			RecordCodecBuilder.create(builder -> builder.group(
					Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), Codec.unboundedMap(ExtraCodecs.POSITIVE_INT, CompoundTag.CODEC))
							.fieldOf("playerTemplates").forGetter(storage -> storage.playerTemplates),
					Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), Codec.unboundedMap(ExtraCodecs.NON_EMPTY_STRING, CompoundTag.CODEC))
							.fieldOf("playerNamedTemplates").forGetter(storage -> storage.playerNamedTemplates))
					.apply(builder, SettingsTemplateStorage::new)));

	private Map<UUID, Map<Integer, CompoundTag>> playerTemplates = new HashMap<>();
	private Map<UUID, Map<String, CompoundTag>> playerNamedTemplates = new HashMap<>();
	private static final SettingsTemplateStorage clientStorageCopy = new SettingsTemplateStorage();

	private SettingsTemplateStorage() {
	}

	private SettingsTemplateStorage(Map<UUID, Map<Integer, CompoundTag>> playerTemplates, Map<UUID, Map<String, CompoundTag>> playerNamedTemplates) {
		this.playerTemplates = playerTemplates;
		this.playerNamedTemplates = playerNamedTemplates;
	}

	public void putPlayerTemplate(Player player, int slot, CompoundTag settingsTag) {
		playerTemplates.computeIfAbsent(player.getUUID(), u -> new HashMap<>()).put(slot, settingsTag);
		setDirty();
	}

	public void putPlayerNamedTemplate(Player player, String name, CompoundTag settingsTag) {
		playerNamedTemplates.computeIfAbsent(player.getUUID(), u -> new TreeMap<>()).put(name, settingsTag);
		setDirty();
	}

	public Map<Integer, CompoundTag> getPlayerTemplates(Player player) {
		return playerTemplates.getOrDefault(player.getUUID(), new HashMap<>());
	}

	public Map<String, CompoundTag> getPlayerNamedTemplates(Player player) {
		return playerNamedTemplates.getOrDefault(player.getUUID(), new TreeMap<>());
	}

	public static SettingsTemplateStorage get() {
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

	public void clearPlayerTemplates(Player player) {
		playerTemplates.remove(player.getUUID());
		playerNamedTemplates.remove(player.getUUID());
		setDirty();
	}
}
