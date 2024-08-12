package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerStorageSoundHandler {
	private ServerStorageSoundHandler() {
	}

	private static final int KEEP_ALIVE_CHECK_INTERVAL = 10;
	private static final Map<ResourceKey<Level>, Long> lastWorldCheck = new HashMap<>();
	private static final Map<ResourceKey<Level>, Map<UUID, KeepAliveInfo>> worldStorageSoundKeepAlive = new HashMap<>();

	public static void tick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}
		ResourceKey<Level> dim = serverLevel.dimension();
		if (lastWorldCheck.computeIfAbsent(dim, key -> serverLevel.getGameTime()) > serverLevel.getGameTime() - KEEP_ALIVE_CHECK_INTERVAL || !worldStorageSoundKeepAlive.containsKey(dim)) {
			return;
		}
		lastWorldCheck.put(dim, serverLevel.getGameTime());

		worldStorageSoundKeepAlive.get(dim).entrySet().removeIf(entry -> {
			if (entry.getValue().getLastKeepAliveTime() < serverLevel.getGameTime() - KEEP_ALIVE_CHECK_INTERVAL) {
				sendStopMessage(serverLevel, entry.getValue().getLastPosition(), entry.getKey());
				return true;
			}
			return false;
		});
	}

	public static void updateKeepAlive(UUID storageUuid, Level level, Vec3 position, Runnable onNoLongerRunning) {
		ResourceKey<Level> dim = level.dimension();
		if (!worldStorageSoundKeepAlive.containsKey(dim) || !worldStorageSoundKeepAlive.get(dim).containsKey(storageUuid)) {
			onNoLongerRunning.run();
			return;
		}
		if (worldStorageSoundKeepAlive.get(dim).containsKey(storageUuid)) {
			worldStorageSoundKeepAlive.get(dim).get(storageUuid).update(level.getGameTime(), position);
		}
	}

	public static void onSoundStopped(Level level, UUID storageUuid) {
		removeKeepAliveInfo(level, storageUuid);
	}

	private static class KeepAliveInfo {
		private final WeakReference<Runnable> onStopHandler;
		private long lastKeepAliveTime;
		private Vec3 lastPosition;

		private KeepAliveInfo(Runnable onStopHandler, long lastKeepAliveTime, Vec3 lastPosition) {
			this.onStopHandler = new WeakReference<>(onStopHandler);
			this.lastKeepAliveTime = lastKeepAliveTime;
			this.lastPosition = lastPosition;
		}

		public long getLastKeepAliveTime() {
			return lastKeepAliveTime;
		}

		public Vec3 getLastPosition() {
			return lastPosition;
		}

		public void update(long gameTime, Vec3 position) {
			lastKeepAliveTime = gameTime;
			lastPosition = position;
		}

		public void runOnStop() {
			Runnable handler = onStopHandler.get();
			if (handler != null) {
				handler.run();
			}
		}
	}

	public static void startPlayingDisc(ServerLevel serverLevel, BlockPos position, UUID storageUuid, Holder<JukeboxSong> song, Runnable onStopHandler) {
		Vec3 pos = Vec3.atCenterOf(position);
		PacketDistributor.sendToPlayersNear(serverLevel, null, pos.x, pos.y, pos.z, 128, new PlayDiscPayload(storageUuid, song, position));
		putKeepAliveInfo(serverLevel, storageUuid, onStopHandler, pos);
	}

	public static void startPlayingDisc(ServerLevel serverLevel, Vec3 position, UUID storageUuid, int entityId, Holder<JukeboxSong> song, Runnable onStopHandler) {
		PacketDistributor.sendToPlayersNear(serverLevel, null, position.x(), position.y(), position.z(), 128, new PlayDiscPayload(storageUuid, song, entityId));
		putKeepAliveInfo(serverLevel, storageUuid, onStopHandler, position);
	}

	private static void putKeepAliveInfo(ServerLevel serverLevel, UUID storageUuid, Runnable onStopHandler, Vec3 pos) {
		worldStorageSoundKeepAlive.computeIfAbsent(serverLevel.dimension(), dim -> new HashMap<>()).put(storageUuid, new KeepAliveInfo(onStopHandler, serverLevel.getGameTime(), pos));
	}

	public static void stopPlayingDisc(Level level, Vec3 position, UUID storageUuid) {
		removeKeepAliveInfo(level, storageUuid);
		sendStopMessage(level, position, storageUuid);
	}

	private static void removeKeepAliveInfo(Level level, UUID storageUuid) {
		ResourceKey<Level> dim = level.dimension();
		if (worldStorageSoundKeepAlive.containsKey(dim) && worldStorageSoundKeepAlive.get(dim).containsKey(storageUuid)) {
			worldStorageSoundKeepAlive.get(dim).remove(storageUuid).runOnStop();
		}
	}

	private static void sendStopMessage(Level level, Vec3 position, UUID storageUuid) {
		if (level instanceof ServerLevel serverLevel) {
			PacketDistributor.sendToPlayersNear(serverLevel, null, position.x(), position.y(), position.z(), 128, new StopDiscPlaybackPayload(storageUuid));
		}
	}
}
