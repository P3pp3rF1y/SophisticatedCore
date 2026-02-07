package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ComponentItemStacksHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;

public class JukeboxUpgradeWrapper extends UpgradeWrapperBase<JukeboxUpgradeWrapper, JukeboxUpgradeItem> implements ITickableUpgrade {
	private static final int KEEP_ALIVE_SEND_INTERVAL = 5;
	private final ComponentItemStacksHandler discInventory;
	private long lastKeepAliveSendTime = 0;
	private boolean isPlaying;

	private final LinkedList<Integer> playlist = new LinkedList<>();
	private final LinkedList<Integer> history = new LinkedList<>();

	private final Set<Integer> discsRemoved = new HashSet<>();
	private final Set<Integer> discsAdded = new HashSet<>();

	@Nullable
	private Entity entityPlaying = null;
	@Nullable
	private Level levelPlaying = null;
	@Nullable
	private BlockPos posPlaying = null;

	private final Runnable onFinishedCallback = this::onDiscFinished;

	protected JukeboxUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		if (upgrade.has(DataComponents.CONTAINER)) {
			upgrade.set(ModCoreDataComponents.LENIENT_CONTAINER, upgrade.get(DataComponents.CONTAINER));
		}
		upgrade.remove(DataComponents.CONTAINER);
		discInventory = new ComponentItemStacksHandler(upgrade, ModCoreDataComponents.LENIENT_CONTAINER.get(), upgradeItem.getNumberOfSlots()) {
			@Override
			protected void onContentsChanged(int index, ItemStack previousContents) {
				super.onContentsChanged(index, previousContents);
				save();
				ItemStack currentContents = getStackInSlot(index);
				if (previousContents.isEmpty() && !currentContents.isEmpty()) {
					discsAdded.add(index);
					discsRemoved.remove(index);
				} else if (!previousContents.isEmpty() && currentContents.isEmpty()) {
					discsRemoved.add(index);
					discsAdded.remove(index);
				}
			}

			@Override
			public boolean isValid(int slot, ItemResource resource) {
				return resource.isEmpty() || DiscHandlerRegistry.isSupported(resource.toStack());
			}
		};
		isPlaying = upgrade.getOrDefault(ModCoreDataComponents.IS_PLAYING, false);
	}

	public boolean isShuffleEnabled() {
		return upgrade.getOrDefault(ModCoreDataComponents.SHUFFLE, false);
	}

	public void setShuffleEnabled(boolean shuffleEnabled) {
		upgrade.set(ModCoreDataComponents.SHUFFLE, shuffleEnabled);
		save();

		initPlaylist(true);
	}

	public RepeatMode getRepeatMode() {
		return upgrade.getOrDefault(ModCoreDataComponents.REPEAT_MODE, RepeatMode.NO);
	}

	public void setRepeatMode(RepeatMode repeatMode) {
		upgrade.set(ModCoreDataComponents.REPEAT_MODE, repeatMode);
		save();
	}

	public ItemStack getDisc() {
		return getDiscSlotActive() > -1 ? discInventory.getStackInSlot(getDiscSlotActive()) : ItemStack.EMPTY;
	}

	public int getDiscSlotActive() {
		return upgrade.getOrDefault(ModCoreDataComponents.DISC_SLOT_ACTIVE, -1);
	}

	private void setDiscSlotActive(int discSlotActive) {
		upgrade.set(ModCoreDataComponents.DISC_SLOT_ACTIVE, discSlotActive);
		save();
	}

	public void play(Level level, BlockPos pos) {
		if (isPlaying) {
			return;
		}

		levelPlaying = level;
		posPlaying = pos;
		playNext();
	}

	public void play(Entity entity) {
		if (isPlaying) {
			return;
		}
		entityPlaying = entity;
		playNext();
	}

	private void playDisc() {
		Level level = entityPlaying != null ? entityPlaying.level() : levelPlaying;
		if (!(level instanceof ServerLevel serverLevel) || (posPlaying == null && entityPlaying == null)) {
			return;
		}
		ItemStack disc = getDisc();
		if (disc.isEmpty()) {
			return;
		}

		storageWrapper.getContentsUuid().ifPresent(storageUuid ->
				DiscHandlerRegistry.findHandler(disc).ifPresent(handler -> {
					if (entityPlaying != null) {
						handler.playDisc(serverLevel, entityPlaying.position(), storageUuid, disc, entityPlaying.getId(), onFinishedCallback);
					} else {
						handler.playDisc(serverLevel, posPlaying, storageUuid, disc, onFinishedCallback);
					}
            		handler.getMusicLengthInTicks(disc, level).ifPresent(lengthInTicks -> upgrade.set(ModCoreDataComponents.DISC_FINISH_TIME, level.getGameTime() + lengthInTicks));
				})
		);
		setIsPlaying(true);
	}

	private void onDiscFinished() {
		if (getRepeatMode() == RepeatMode.ONE) {
			playDisc();
		} else if (getRepeatMode() == RepeatMode.ALL) {
			playNext();
		} else {
			playNext(false);
		}
	}

	private void setIsPlaying(boolean playing) {
		isPlaying = playing;
		upgrade.set(ModCoreDataComponents.IS_PLAYING, playing);
		if (isPlaying) {
			storageWrapper.getRenderDataHandler().setUpgradeClientData(JukeboxUpgradeClientData.TYPE, new JukeboxUpgradeClientData(true));
		} else {
			removeClientData();
			setDiscSlotActive(-1);
		}
		save();
	}

	private void removeClientData() {
		storageWrapper.getRenderDataHandler().removeUpgradeClientData(JukeboxUpgradeClientData.TYPE);
	}

	public void stop(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel)) {
			return;
		}
		storageWrapper.getContentsUuid().ifPresent(storageUuid ->
				ServerStorageSoundHandler.stopPlayingDisc(entity.level(), entity.position(), storageUuid)
		);
		setIsPlaying(false);
		upgrade.remove(ModCoreDataComponents.DISC_FINISH_TIME);
		setDiscSlotActive(-1);
		playlist.clear();
		history.clear();
	}

	public ComponentItemStacksHandler getDiscInventory() {
		return discInventory;
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (!level.isClientSide()) {
			if (!discsRemoved.isEmpty()) {
				discsRemoved.forEach(index -> {
					playlist.remove(index);
					history.remove(index);
				});
				discsRemoved.clear();
			}
			if (!discsAdded.isEmpty()) {
				playlist.addAll(discsAdded);
				discsAdded.clear();
			}
		}

		if (isPlaying && lastKeepAliveSendTime < level.getGameTime() - KEEP_ALIVE_SEND_INTERVAL) {
			storageWrapper.getContentsUuid().ifPresent(storageUuid ->
					ServerStorageSoundHandler.updateKeepAlive(storageUuid, level, Vec3.atCenterOf(pos), () -> setIsPlaying(false))
			);
			lastKeepAliveSendTime = level.getGameTime();
		}
	}

	public boolean isPlaying() {
		return isPlaying;
	}

	@Override
	public void onBeforeRemoved() {
		removeClientData();
	}

	public void next() {
		if (!isPlaying) {
			return;
		}
		playNext();
	}

	public void playNext() {
		playNext(true);
	}

	public void playNext(boolean startOverIfAtTheEnd) {
		if (playlist.isEmpty() && startOverIfAtTheEnd) {
			initPlaylist(false);
		}
		if (playlist.isEmpty()) {
			return;
		}
		if (getDiscSlotActive() != -1) {
			history.add(getDiscSlotActive());
			if (history.size() > discInventory.size()) {
				history.poll();
			}
		}
		Integer discIndex = playlist.poll();
		if (discIndex == null) {
			return;
		}
		setDiscSlotActive(discIndex);

		playDisc();
	}

	private void initPlaylist(boolean excludeActive) {
		playlist.clear();
		for (int i = 0; i < discInventory.size(); i++) {
			if (!discInventory.getStackInSlot(i).isEmpty() && (!excludeActive || !isPlaying || i != getDiscSlotActive())) {
				playlist.add(i);
			}
		}
		if (isShuffleEnabled()) {
			Collections.shuffle(playlist);
		}
	}

	public void previous() {
		if (!isPlaying) {
			return;
		}
		playPrevious();
	}

	public void playPrevious() {
		if (history.isEmpty()) {
			return;
		}
		playlist.addFirst(getDiscSlotActive());
		Integer discIndex = history.pollLast();
		if (discIndex == null) {
			return;
		}
		setDiscSlotActive(discIndex);
		playDisc();
	}

	public long getDiscFinishTime() {
		return upgrade.getOrDefault(ModCoreDataComponents.DISC_FINISH_TIME, 0L);
	}
}
