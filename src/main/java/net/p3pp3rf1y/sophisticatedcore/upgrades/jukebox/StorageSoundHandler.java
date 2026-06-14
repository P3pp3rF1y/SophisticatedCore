package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

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
		}
	}

	public static boolean isStorageSoundPlayingNear(Vec3 position, double radius) {
		double radiusSqr = radius * radius;
		return storageSounds.values().stream().anyMatch(sound -> {
			Vec3 soundPosition = sound instanceof StorageSoundPosition storageSoundPosition ? storageSoundPosition.getStorageSoundPosition() : new Vec3(sound.getX(), sound.getY(), sound.getZ());
			double xDiff = soundPosition.x - position.x;
			double yDiff = soundPosition.y - position.y;
			double zDiff = soundPosition.z - position.z;
			return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff <= radiusSqr;
		});
	}

	public static void tick(LevelTickEvent.Post event) {
		if (!storageSounds.isEmpty() && lastPlaybackChecked < event.getLevel().getGameTime() - SOUND_STOP_CHECK_INTERVAL) {
			lastPlaybackChecked = event.getLevel().getGameTime();
			storageSounds.entrySet().removeIf(entry ->
					!Minecraft.getInstance().getSoundManager().isActive(entry.getValue())
			);
		}
	}

	public static void playStorageSound(SoundEvent soundEvent, UUID storageUuid, BlockPos pos) {
		playStorageSound(storageUuid, SimpleSoundInstance.forJukeboxSong(soundEvent, Vec3.atCenterOf(pos)));
	}

	public static void playStorageSound(SoundEvent soundEvent, UUID storageUuid, int entityId) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		float volume = 2;
		float pitch = 1;

		Entity entity = level.getEntity(entityId);
		if (entity == null) {
			stopStorageSound(storageUuid);
			return;
		}
		Player player = Minecraft.getInstance().player;
		if (player != null && player.getId() == entityId) {
			playStorageSound(storageUuid, new PlayerDirectStorageSoundInstance(soundEvent, player, level.random, volume, pitch));
			return;
		}
		playStorageSound(storageUuid, new EntityBoundSoundInstance(soundEvent, SoundSource.RECORDS, volume, pitch, entity, level.random.nextLong()){
			@Override
			public void tick() {
				super.tick();
				if (entity instanceof Player player) {
					Vec3 lookAngle = player.getLookAngle();
					this.x = player.getX() + lookAngle.x;
					this.y = player.getEyeY() + lookAngle.y;
					this.z = player.getZ() + lookAngle.z;
				}
			}
		});
	}

	private interface StorageSoundPosition {
		Vec3 getStorageSoundPosition();
	}

	private static class PlayerDirectStorageSoundInstance extends AbstractTickableSoundInstance implements StorageSoundPosition {
		private final Player player;

		private PlayerDirectStorageSoundInstance(SoundEvent soundEvent, Player player, RandomSource random, float volume, float pitch) {
			super(soundEvent, SoundSource.RECORDS, random);
			this.player = player;
			this.volume = volume;
			this.pitch = pitch;
			attenuation = SoundInstance.Attenuation.NONE;
			relative = true;
		}

		@Override
		public void tick() {
			if (!player.isAlive()) {
				stop();
			}
		}

		@Override
		public Vec3 getStorageSoundPosition() {
			return player.position();
		}
	}

	@SuppressWarnings({"unused", "java:S1172"}) // needs to be here for addListener to recognize which event this method should be subscribed to
	public static void onWorldUnload(LevelEvent.Unload evt) {
		storageSounds.clear();
		lastPlaybackChecked = 0;
	}
}
