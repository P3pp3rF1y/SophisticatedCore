package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class JukeboxUpgradeItem extends UpgradeItemBase<JukeboxUpgradeItem.Wrapper> {
	public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);

	public JukeboxUpgradeItem(IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
	}

	@Override
	public UpgradeType<Wrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}

	public static class Wrapper extends UpgradeWrapperBase<Wrapper, JukeboxUpgradeItem> implements ITickableUpgrade {
		private static final int KEEP_ALIVE_SEND_INTERVAL = 5;
		private final ComponentItemHandler discInventory;
		private long lastKeepAliveSendTime = 0;
		private boolean isPlaying;

		protected Wrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler);
			discInventory = new ComponentItemHandler(upgrade, DataComponents.CONTAINER, 1) {
				@Override
				protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {
					super.onContentsChanged(slot, oldStack, newStack);
					save();
				}

				@Override
				public boolean isItemValid(int slot, ItemStack stack) {
					return stack.isEmpty() || stack.has(DataComponents.JUKEBOX_PLAYABLE);
				}

				@Override
				public int getSlotLimit(int slot) {
					return 64;
				}
			};
			isPlaying = upgrade.getOrDefault(ModCoreDataComponents.IS_PLAYING, false);
		}

		public void setDisc(ItemStack disc) {
			discInventory.setStackInSlot(0, disc);
		}

		public ItemStack getDisc() {
			return discInventory.getStackInSlot(0);
		}

		public void play(Level level, BlockPos pos) {
			play(level, (serverLevel, storageUuid) -> JukeboxSong.fromStack(level.registryAccess(), getDisc())
							.ifPresent(song -> ServerStorageSoundHandler.startPlayingDisc(serverLevel, pos, storageUuid, song, () -> setIsPlaying(false))));
		}

		public void play(LivingEntity entity) {
			play(entity.level(), (world, storageUuid) -> JukeboxSong.fromStack(entity.level().registryAccess(), getDisc())
							.ifPresent(song -> ServerStorageSoundHandler.startPlayingDisc(world, entity.position(), storageUuid, entity.getId(), song, () -> setIsPlaying(false))));
		}

		private void play(Level level, BiConsumer<ServerLevel, UUID> play) {
			if (!(level instanceof ServerLevel) || getDisc().isEmpty()) {
				return;
			}
			storageWrapper.getContentsUuid().ifPresent(storageUuid -> play.accept((ServerLevel) level, storageUuid));
			setIsPlaying(true);
		}

		private void setIsPlaying(boolean playing) {
			isPlaying = playing;
			upgrade.set(ModCoreDataComponents.IS_PLAYING, playing);
			if (isPlaying) {
				storageWrapper.getRenderInfo().setUpgradeRenderData(JukeboxUpgradeRenderData.TYPE, new JukeboxUpgradeRenderData(true));
			} else {
				removeRenderData();
			}
			save();
		}

		private void removeRenderData() {
			storageWrapper.getRenderInfo().removeUpgradeRenderData(JukeboxUpgradeRenderData.TYPE);
		}

		public void stop(LivingEntity entity) {
			if (!(entity.level() instanceof ServerLevel)) {
				return;
			}
			storageWrapper.getContentsUuid().ifPresent(storageUuid ->
					ServerStorageSoundHandler.stopPlayingDisc(entity.level(), entity.position(), storageUuid)
			);
			setIsPlaying(false);
		}

		public IItemHandler getDiscInventory() {
			return discInventory;
		}

		@Override
		public void tick(@Nullable LivingEntity entity, Level level, BlockPos pos) {
			if (isPlaying && lastKeepAliveSendTime < level.getGameTime() - KEEP_ALIVE_SEND_INTERVAL) {
				storageWrapper.getContentsUuid().ifPresent(storageUuid ->
						ServerStorageSoundHandler.updateKeepAlive(storageUuid, level, entity != null ? entity.position() : Vec3.atCenterOf(pos), () -> setIsPlaying(false))
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
	}
}
