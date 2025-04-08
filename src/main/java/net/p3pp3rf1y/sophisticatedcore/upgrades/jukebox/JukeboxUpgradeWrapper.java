package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;

public class JukeboxUpgradeWrapper extends UpgradeWrapperBase<JukeboxUpgradeWrapper, JukeboxUpgradeItem> implements ITickableUpgrade {
	private static final int KEEP_ALIVE_SEND_INTERVAL = 5;
	private final ItemStackHandler discInventory;
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
		discInventory = new ItemStackHandler(upgradeItem.getNumberOfSlots()) {
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				upgrade.addTagElement("discInventory", serializeNBT());
				save();
				if (getStackInSlot(slot).isEmpty()) {
					discsRemoved.add(slot);
					discsAdded.remove(slot);
				} else {
					if (!playlist.contains(slot)) {
						discsAdded.add(slot);
						discsRemoved.remove(slot);
					}
				}
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return stack.getItem() instanceof RecordItem;
			}
		};
		NBTHelper.getCompound(upgrade, "discInventory").ifPresent(discInventory::deserializeNBT);
		isPlaying = NBTHelper.getBoolean(upgrade, "isPlaying").orElse(false);
	}

	public boolean isShuffleEnabled() {
		return NBTHelper.getBoolean(upgrade, "shuffle").orElse(false);
	}

	public void setShuffleEnabled(boolean shuffleEnabled) {
		NBTHelper.setBoolean(upgrade, "shuffle", shuffleEnabled);
		save();

		initPlaylist(true);
	}

	public RepeatMode getRepeatMode() {
		return NBTHelper.getEnumConstant(upgrade, "repeatMode", RepeatMode::fromName).orElse(RepeatMode.NO);
	}

	public void setRepeatMode(RepeatMode repeatMode) {
		NBTHelper.setEnumConstant(upgrade, "repeatMode", repeatMode);
		save();
	}

	public ItemStack getDisc() {
		return getDiscSlotActive() > -1 ? discInventory.getStackInSlot(getDiscSlotActive()) : ItemStack.EMPTY;
	}

	public int getDiscSlotActive() {
		return NBTHelper.getInt(upgrade, "discSlotActive").orElse(-1);
	}

	private void setDiscSlotActive(int discSlotActive) {
		NBTHelper.setInteger(upgrade, "discSlotActive", discSlotActive);
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
		if (getDisc().isEmpty()) {
			return;
		}

		storageWrapper.getContentsUuid().ifPresent(storageUuid -> {
			if (entityPlaying != null) {
				ServerStorageSoundHandler.startPlayingDisc(serverLevel, entityPlaying.position(), storageUuid, entityPlaying.getId(), Item.getId(getDisc().getItem()), onFinishedCallback);
			} else {
				ServerStorageSoundHandler.startPlayingDisc(serverLevel, posPlaying, storageUuid, Item.getId(getDisc().getItem()), onFinishedCallback);
			}
			if (getDisc().getItem() instanceof RecordItem recordItem) {
				int lengthInTicks = recordItem.getLengthInTicks();
				NBTHelper.setLong(upgrade, "discFinishTime", level.getGameTime() + lengthInTicks);
				NBTHelper.setLong(upgrade, "discLength", lengthInTicks);
			}
		});
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
		NBTHelper.setBoolean(upgrade, "isPlaying", playing);
		if (isPlaying) {
			storageWrapper.getRenderInfo().setUpgradeRenderData(JukeboxUpgradeRenderData.TYPE, new JukeboxUpgradeRenderData(true));
		} else {
			removeRenderData();
			setDiscSlotActive(-1);
		}
		save();
	}

	private void removeRenderData() {
		storageWrapper.getRenderInfo().removeUpgradeRenderData(JukeboxUpgradeRenderData.TYPE);
	}

	public void stop(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		storageWrapper.getContentsUuid().ifPresent(storageUuid ->
				ServerStorageSoundHandler.stopPlayingDisc(serverLevel, entity.position(), storageUuid)
		);
		setIsPlaying(false);
		NBTHelper.removeTag(upgrade, "discFinishTime");
		NBTHelper.removeTag(upgrade, "discLength");
		setDiscSlotActive(-1);
		playlist.clear();
		history.clear();
	}

	public IItemHandler getDiscInventory() {
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
		removeRenderData();
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
			if (history.size() > discInventory.getSlots()) {
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
		for (int i = 0; i < discInventory.getSlots(); i++) {
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
		return NBTHelper.getLong(upgrade, "discFinishTime").orElse(0L);
	}

	public int getDiscLength() {
		return NBTHelper.getInt(upgrade, "discLength").orElse(0);
	}
}
