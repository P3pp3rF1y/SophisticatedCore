package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.ref.WeakReference;
import java.util.*;

public class ServerStorageSoundHandler {
	private ServerStorageSoundHandler() {
	}

	private static final int KEEP_ALIVE_CHECK_INTERVAL = 10;
	private static final Map<ResourceKey<Level>, Long> lastWorldCheck = new HashMap<>();
	private static final Map<ResourceKey<Level>, Map<UUID, SoundInfo>> worldStorageSoundInfos = new HashMap<>();

	public static void tick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}

		ResourceKey<Level> dim = serverLevel.dimension();
		if (!worldStorageSoundInfos.containsKey(dim)) {
			return;
		}

		removeFinished(serverLevel, dim);

		if (lastWorldCheck.computeIfAbsent(dim, key -> serverLevel.getGameTime()) > serverLevel.getGameTime() - KEEP_ALIVE_CHECK_INTERVAL) {
			return;
		}
		lastWorldCheck.put(dim, serverLevel.getGameTime());

		worldStorageSoundInfos.get(dim).entrySet().removeIf(entry -> {
			if (entry.getValue().getLastKeepAliveTime() < serverLevel.getGameTime() - KEEP_ALIVE_CHECK_INTERVAL) {
				sendStopMessage(serverLevel, entry.getValue().getLastPosition(), entry.getKey());
				return true;
			}
			return false;
		});
	}

	private static void removeFinished(ServerLevel serverLevel, ResourceKey<Level> dim) {
		List<UUID> storageIdsToRemove = new ArrayList<>();
		worldStorageSoundInfos.get(dim).forEach((storageId, soundInfo) -> {
			if (soundInfo.getFinishTime() <= serverLevel.getGameTime()) {
				storageIdsToRemove.add(storageId);
			}
		});
		storageIdsToRemove.forEach(storageId -> removeSoundInfo(serverLevel, storageId, true));
	}

	public static void updateKeepAlive(UUID storageUuid, Level level, Vec3 position, Runnable onNoLongerRunning) {
		ResourceKey<Level> dim = level.dimension();
		if (!worldStorageSoundInfos.containsKey(dim) || !worldStorageSoundInfos.get(dim).containsKey(storageUuid)) {
			onNoLongerRunning.run();
			return;
		}
		if (worldStorageSoundInfos.get(dim).containsKey(storageUuid)) {
			worldStorageSoundInfos.get(dim).get(storageUuid).update(level.getGameTime(), position);
		}
	}

	public static void onSoundFinished(Level level, UUID storageUuid) {
		removeSoundInfo(level, storageUuid, true);
	}

	private static class SoundInfo {
		private final WeakReference<Runnable> onFinishedHandler;
		private long lastKeepAliveTime;
		private Vec3 lastPosition;
		private long finishTime;

		private SoundInfo(Runnable onFinishedHandler, long lastKeepAliveTime, Vec3 lastPosition, long finishTime) {
			this.onFinishedHandler = new WeakReference<>(onFinishedHandler);
			this.lastKeepAliveTime = lastKeepAliveTime;
			this.lastPosition = lastPosition;
			this.finishTime = finishTime;
		}

		public long getLastKeepAliveTime() {
			return lastKeepAliveTime;
		}

		public Vec3 getLastPosition() {
			return lastPosition;
		}

		public long getFinishTime() {
			return finishTime;
		}

		public void update(long gameTime, Vec3 position) {
			lastKeepAliveTime = gameTime;
			lastPosition = position;
		}

		public void runOnFinished() {
			Runnable handler = onFinishedHandler.get();
			if (handler != null) {
				handler.run();
			}
		}
	}

	public static void startPlayingDisc(ServerLevel serverLevel, BlockPos position, UUID storageUuid, Holder<JukeboxSong> song, Runnable onFinishedHandler) {
		Vec3 pos = Vec3.atCenterOf(position);
		PacketDistributor.sendToPlayersNear(serverLevel, null, pos.x, pos.y, pos.z, 128, new PlayDiscPayload(storageUuid, song, position));
		putSoundInfo(serverLevel, storageUuid, onFinishedHandler, pos, serverLevel.getGameTime() + song.value().lengthInTicks());
	}

	public static void startPlayingDisc(ServerLevel serverLevel, Vec3 position, UUID storageUuid, int entityId, Holder<JukeboxSong> song, Runnable onStopHandler) {
		PacketDistributor.sendToPlayersNear(serverLevel, null, position.x(), position.y(), position.z(), 128, new PlayDiscPayload(storageUuid, song, entityId));
		putSoundInfo(serverLevel, storageUuid, onStopHandler, position, serverLevel.getGameTime() + song.value().lengthInTicks());
	}

	public static void putSoundInfo(ServerLevel serverLevel, UUID storageUuid, Runnable onFinishedHandler, Vec3 pos, long finishTime) {
		worldStorageSoundInfos.computeIfAbsent(serverLevel.dimension(), dim -> new HashMap<>()).put(storageUuid, new SoundInfo(onFinishedHandler, serverLevel.getGameTime(), pos, finishTime));
	}

	public static void stopPlayingDisc(Level level, Vec3 position, UUID storageUuid) {
		removeSoundInfo(level, storageUuid, false);
		sendStopMessage(level, position, storageUuid);
	}

	private static void removeSoundInfo(Level level, UUID storageUuid, boolean finished) {
		ResourceKey<Level> dim = level.dimension();
		if (worldStorageSoundInfos.containsKey(dim) && worldStorageSoundInfos.get(dim).containsKey(storageUuid)) {
			SoundInfo soundInfo = worldStorageSoundInfos.get(dim).remove(storageUuid);
			if (finished) {
				soundInfo.runOnFinished();
			}
		}
	}

	private static void sendStopMessage(Level level, Vec3 position, UUID storageUuid) {
		if (level instanceof ServerLevel serverLevel) {
			PacketDistributor.sendToPlayersNear(serverLevel, null, position.x(), position.y(), position.z(), 128, new StopDiscPlaybackPayload(storageUuid));
		}
	}

	@SuppressWarnings({"unused", "java:S1172"}) // needs to be here for addListener to recognize which event this method should be subscribed to
	public static void onWorldUnload(LevelEvent.Unload evt) {
		if (!(evt.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}

		worldStorageSoundInfos.remove(serverLevel.dimension());
		lastWorldCheck.remove(serverLevel.dimension());
	}
}
