package net.p3pp3rf1y.sophisticatedcore.settings.main;

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
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PlayerMainSettingsSavedData extends SavedData {
	public static final String FILE_ID = SophisticatedCore.MOD_ID + "-player-main-settings";
	private static final SavedDataType<PlayerMainSettingsSavedData> TYPE = new SavedDataType<>(FILE_ID, PlayerMainSettingsSavedData::new,
			RecordCodecBuilder.create(instance ->
					instance.group(
							Codec.unboundedMap(
									CodecHelper.STRING_ENCODED_UUID,
									Codec.unboundedMap(Codec.STRING, MainSettingsCategoryData.CODEC).xmap(CodecHelper::toMutable, Function.identity())
							).xmap(CodecHelper::toMutable, Function.identity()).fieldOf("data").forGetter(savedData -> savedData.data)
					).apply(instance, PlayerMainSettingsSavedData::new))
	);
	private static final PlayerMainSettingsSavedData clientDataCopy = new PlayerMainSettingsSavedData();

	private final Map<UUID, Map<String, MainSettingsCategoryData>> data;

	public PlayerMainSettingsSavedData(Map<UUID, Map<String, MainSettingsCategoryData>> data) {
		this.data = data;
	}

	private PlayerMainSettingsSavedData() {
		this.data = new HashMap<>();
	}

	public static PlayerMainSettingsSavedData get() {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server != null) {
				ServerLevel overworld = server.getLevel(Level.OVERWORLD);
				//noinspection ConstantConditions - by this time overworld is loaded
				DimensionDataStorage storage = overworld.getDataStorage();
				return storage.computeIfAbsent(TYPE);
			}
		}
		return clientDataCopy;
	}

	public MainSettingsCategoryData get(UUID uuid, String settingsName) {
		return data.computeIfAbsent(uuid, id -> new HashMap<>()).computeIfAbsent(settingsName, n -> new MainSettingsCategoryData());
	}

	public void put(UUID uuid, String settingsName, MainSettingsCategoryData value) {
		data.computeIfAbsent(uuid, k -> new HashMap<>()).put(settingsName, value);
		setDirty();
	}

	public void setvalue(UUID uuid, String settingsName, Consumer<MainSettingsCategoryData> setter) {
		setter.accept(get(uuid, settingsName));
		setDirty();
	}
}
