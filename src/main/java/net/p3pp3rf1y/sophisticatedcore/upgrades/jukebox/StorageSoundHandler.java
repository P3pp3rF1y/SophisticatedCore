package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StorageSoundHandler {
	private static final int SOUND_STOP_CHECK_INTERVAL = 10;

	private StorageSoundHandler() {}

	private static final Map<UUID, SoundInstance> storageSounds = new ConcurrentHashMap<>();
	private static long lastPlaybackChecked = 0;

	public static void playStorageSound(UUID storageUuid, SoundInstance sound) {
		stopStorageSound(storageUuid);
		storageSounds.put(storageUuid, sound);
		Minecraft.getInstance().getSoundManager().play(sound);
	}

	public static void stopStorageSound(UUID storageUuid) {
		if (storageSounds.containsKey(storageUuid)) {
			Minecraft.getInstance().getSoundManager().stop(storageSounds.remove(storageUuid));
			PacketHandler.INSTANCE.sendToServer(new SoundStopNotificationMessage(storageUuid));
		}
	}

	public static void tick(TickEvent.LevelTickEvent event) {
		if (!storageSounds.isEmpty() && lastPlaybackChecked < event.level.getGameTime() - SOUND_STOP_CHECK_INTERVAL) {
			lastPlaybackChecked = event.level.getGameTime();
			storageSounds.entrySet().removeIf(entry -> {
				if (!Minecraft.getInstance().getSoundManager().isActive(entry.getValue())) {
					PacketHandler.INSTANCE.sendToServer(new SoundStopNotificationMessage(entry.getKey()));
					return true;
				}
				return false;
			});
		}
	}

	public static void playStorageSound(SoundEvent soundEvent, UUID storageUuid, BlockPos pos) {
		playStorageSound(storageUuid, SimpleSoundInstance.forRecord(soundEvent, pos.getX(), pos.getY(), pos.getZ()));
	}

	public static void playStorageSound(SoundEvent soundEvent, UUID storageUuid, int entityId) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}

		Entity entity = level.getEntity(entityId);
		if (!(entity instanceof LivingEntity)) {
			return;
		}
		playStorageSound(storageUuid, new EntityBoundSoundInstance(soundEvent, SoundSource.RECORDS, 2, 1, entity, level.random.nextLong()));
	}

	@SuppressWarnings({"unused", "java:S1172"}) // needs to be here for addListener to recognize which event this method should be subscribed to
	public static void onWorldUnload(LevelEvent.Unload evt) {
		storageSounds.clear();
		lastPlaybackChecked = 0;
	}
}
