package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerStorageSoundHandler {
	private ServerStorageSoundHandler() {}

	private static final int KEEP_ALIVE_CHECK_INTERVAL = 10;
	private static final Map<ResourceKey<Level>, Long> lastWorldCheck = new HashMap<>();
	private static final Map<ResourceKey<Level>, Map<UUID, KeepAliveInfo>> worldStorageSoundKeepAlive = new HashMap<>();

	public static void init() {
		MinecraftForge.EVENT_BUS.addListener(ServerStorageSoundHandler::tick);
	}

	public static void tick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
			return;
		}
		ServerLevel world = (ServerLevel) event.level;
		ResourceKey<Level> dim = world.dimension();
		if (lastWorldCheck.computeIfAbsent(dim, key -> world.getGameTime()) > world.getGameTime() - KEEP_ALIVE_CHECK_INTERVAL || !worldStorageSoundKeepAlive.containsKey(dim)) {
			return;
		}
		lastWorldCheck.put(dim, world.getGameTime());

		worldStorageSoundKeepAlive.get(dim).entrySet().removeIf(entry -> {
			if (entry.getValue().getLastKeepAliveTime() < world.getGameTime() - KEEP_ALIVE_CHECK_INTERVAL) {
				sendStopMessage(world, entry.getValue().getLastPosition(), entry.getKey());
				return true;
			}
			return false;
		});
	}

	public static void updateKeepAlive(UUID storageUuid, Level world, Vec3 position, Runnable onNoLongerRunning) {
		ResourceKey<Level> dim = world.dimension();
		if (!worldStorageSoundKeepAlive.containsKey(dim) || !worldStorageSoundKeepAlive.get(dim).containsKey(storageUuid)) {
			onNoLongerRunning.run();
			return;
		}
		if (worldStorageSoundKeepAlive.get(dim).containsKey(storageUuid)) {
			worldStorageSoundKeepAlive.get(dim).get(storageUuid).update(world.getGameTime(), position);
		}
	}

	public static void onSoundFinished(ServerLevel level, UUID storageUuid) {
		removeKeepAliveInfo(level, storageUuid, true);
	}

	private static class KeepAliveInfo {
		private final WeakReference<Runnable> onFinishedHandler;
		private long lastKeepAliveTime;
		private Vec3 lastPosition;

		private KeepAliveInfo(Runnable onFinishedHandler, long lastKeepAliveTime, Vec3 lastPosition) {
			this.onFinishedHandler = new WeakReference<>(onFinishedHandler);
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

		public void runOnFinished() {
			Runnable handler = onFinishedHandler.get();
			if (handler != null) {
				handler.run();
			}
		}
	}

	public static void startPlayingDisc(ServerLevel serverWorld, BlockPos position, UUID storageUuid, int discItemId, Runnable onFinishedHandler) {
		Vec3 pos = Vec3.atCenterOf(position);
		PacketHandler.INSTANCE.sendToAllNear(serverWorld.dimension(), pos, 128, new PlayDiscMessage(storageUuid, discItemId, position));
		putKeepAliveInfo(serverWorld, storageUuid, onFinishedHandler, pos);
	}

	public static void startPlayingDisc(ServerLevel serverWorld, Vec3 position, UUID storageUuid, int entityId, int discItemId, Runnable onFinishedHandler) {
		PacketHandler.INSTANCE.sendToAllNear(serverWorld.dimension(), position, 128, new PlayDiscMessage(storageUuid, discItemId, entityId));
		putKeepAliveInfo(serverWorld, storageUuid, onFinishedHandler, position);
	}

	private static void putKeepAliveInfo(ServerLevel serverWorld, UUID storageUuid, Runnable onFinishedHandler, Vec3 pos) {
		worldStorageSoundKeepAlive.computeIfAbsent(serverWorld.dimension(), dim -> new HashMap<>()).put(storageUuid, new KeepAliveInfo(onFinishedHandler, serverWorld.getGameTime(), pos));
	}

	public static void stopPlayingDisc(ServerLevel serverWorld, Vec3 position, UUID storageUuid) {
		removeKeepAliveInfo(serverWorld, storageUuid, false);
		sendStopMessage(serverWorld, position, storageUuid);
	}

	private static void removeKeepAliveInfo(ServerLevel serverWorld, UUID storageUuid, boolean finished) {
		ResourceKey<Level> dim = serverWorld.dimension();
		if (worldStorageSoundKeepAlive.containsKey(dim) && worldStorageSoundKeepAlive.get(dim).containsKey(storageUuid)) {
			KeepAliveInfo keepAliveInfo = worldStorageSoundKeepAlive.get(dim).remove(storageUuid);
			if (finished) {
				keepAliveInfo.runOnFinished();
			}
		}
	}

	private static void sendStopMessage(ServerLevel serverWorld, Vec3 position, UUID storageUuid) {
		PacketHandler.INSTANCE.sendToAllNear(serverWorld.dimension(), position, 128, new StopDiscPlaybackMessage(storageUuid));
	}
}
