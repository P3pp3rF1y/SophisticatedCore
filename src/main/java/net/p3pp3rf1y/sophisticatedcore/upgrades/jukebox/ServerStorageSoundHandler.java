package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

import java.lang.ref.WeakReference;
import java.util.*;

public class ServerStorageSoundHandler {
	private ServerStorageSoundHandler() {
	}

	private static final int KEEP_ALIVE_CHECK_INTERVAL = 10;
	private static final Map<ResourceKey<Level>, Long> lastWorldCheck = new HashMap<>();
	private static final Map<ResourceKey<Level>, Map<UUID, SoundInfo>> worldStorageSoundInfos = new HashMap<>();

	public static void init() {
		MinecraftForge.EVENT_BUS.addListener(ServerStorageSoundHandler::tick);
	}

	public static void tick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) {
			return;
		}
		ResourceKey<Level> dim = serverLevel.dimension();

		if (!worldStorageSoundInfos.containsKey(dim)) {
			return;
		}

		removeFinished(serverLevel, dim);

		if (lastWorldCheck.computeIfAbsent(dim, key -> serverLevel.getGameTime()) > serverLevel.getGameTime() - KEEP_ALIVE_CHECK_INTERVAL || !worldStorageSoundInfos.containsKey(dim)) {
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

	public static void updateKeepAlive(UUID storageUuid, Level world, Vec3 position, Runnable onNoLongerRunning) {
		ResourceKey<Level> dim = world.dimension();
		if (!worldStorageSoundInfos.containsKey(dim) || !worldStorageSoundInfos.get(dim).containsKey(storageUuid)) {
			onNoLongerRunning.run();
			return;
		}
		if (worldStorageSoundInfos.get(dim).containsKey(storageUuid)) {
			worldStorageSoundInfos.get(dim).get(storageUuid).update(world.getGameTime(), position);
		}
	}

	private static class SoundInfo {
		private final WeakReference<Runnable> onFinishedHandler;
		private long lastKeepAliveTime;
		private Vec3 lastPosition;
		private final long finishTime;

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

	public static void startPlayingDisc(ServerLevel serverLevel, BlockPos position, UUID storageUuid, Item item, Runnable onFinishedHandler) {
		Vec3 pos = Vec3.atCenterOf(position);
		PacketHandler.INSTANCE.sendToAllNear(serverLevel.dimension(), pos, 128, new PlayDiscMessage(storageUuid, Item.getId(item), position));
		putSoundInfo(serverLevel, storageUuid, onFinishedHandler, pos, serverLevel.getGameTime() + (item instanceof RecordItem recordItem ? recordItem.getLengthInTicks() : 0));
	}

	public static void startPlayingDisc(ServerLevel serverLevel, Vec3 position, UUID storageUuid, int entityId, Item item, Runnable onFinishedHandler) {
		PacketHandler.INSTANCE.sendToAllNear(serverLevel.dimension(), position, 128, new PlayDiscMessage(storageUuid, Item.getId(item), entityId));
		putSoundInfo(serverLevel, storageUuid, onFinishedHandler, position, serverLevel.getGameTime() + (item instanceof RecordItem recordItem ? recordItem.getLengthInTicks() : 0));
	}

	private static void putSoundInfo(ServerLevel serverWorld, UUID storageUuid, Runnable onFinishedHandler, Vec3 pos, long finishTime) {
		worldStorageSoundInfos.computeIfAbsent(serverWorld.dimension(), dim -> new HashMap<>()).put(storageUuid, new SoundInfo(onFinishedHandler, serverWorld.getGameTime(), pos, finishTime));
	}

	public static void stopPlayingDisc(ServerLevel serverWorld, Vec3 position, UUID storageUuid) {
		removeSoundInfo(serverWorld, storageUuid, false);
		sendStopMessage(serverWorld, position, storageUuid);
	}

	private static void removeSoundInfo(ServerLevel serverWorld, UUID storageUuid, boolean finished) {
		ResourceKey<Level> dim = serverWorld.dimension();
		if (worldStorageSoundInfos.containsKey(dim) && worldStorageSoundInfos.get(dim).containsKey(storageUuid)) {
			SoundInfo soundInfo = worldStorageSoundInfos.get(dim).remove(storageUuid);
			if (finished) {
				soundInfo.runOnFinished();
			}
		}
	}

	private static void sendStopMessage(ServerLevel serverWorld, Vec3 position, UUID storageUuid) {
		PacketHandler.INSTANCE.sendToAllNear(serverWorld.dimension(), position, 128, new StopDiscPlaybackMessage(storageUuid));
	}
}
