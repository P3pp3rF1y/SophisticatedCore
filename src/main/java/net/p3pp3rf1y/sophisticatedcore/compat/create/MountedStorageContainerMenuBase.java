package net.p3pp3rf1y.sophisticatedcore.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SophisticatedMenuProvider;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;

import javax.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.UUID;

public abstract class MountedStorageContainerMenuBase extends StorageContainerMenuBase<IStorageWrapper> {
	protected final WeakReference<AbstractContraptionEntity> contraptionEntity;
	protected final BlockPos localPos;

	@Nullable
	private CompoundTag lastSettingsNbt = null;
	protected final MountedStorageBase mountedStorage;

	public MountedStorageContainerMenuBase(MenuType<?> menuType, int containerId, Player player, IStorageWrapper parentStorageWrapper, int storageItemSlotIndex,
			boolean shouldLockStorageItemSlot, int contraptionEntityId, BlockPos localPos) {
		this(menuType, containerId, player, getWrapper(player.level(), contraptionEntityId, localPos), parentStorageWrapper, storageItemSlotIndex,
				shouldLockStorageItemSlot, contraptionEntityId, localPos);
	}

	public MountedStorageContainerMenuBase(MenuType<?> menuType, int containerId, Player player, IStorageWrapper wrapper, IStorageWrapper parentStorageWrapper,
			int storageItemSlotIndex, boolean shouldLockStorageItemSlot, int contraptionEntityId, BlockPos localPos) {
		super(menuType, containerId, player, wrapper, parentStorageWrapper, storageItemSlotIndex, shouldLockStorageItemSlot);
		if (!(player.level().getEntity(contraptionEntityId) instanceof AbstractContraptionEntity cEntity)) {
			throw new IllegalArgumentException("Incorrect entity with id " + contraptionEntityId + " expected to find AbstractContraptionEntity");
		}
		this.contraptionEntity = new WeakReference<>(cEntity);
		this.localPos = localPos;
		MountedStorageBase itemStorage = ContraptionHelper.getMountedStorage(cEntity, localPos);
		if (itemStorage == null) {
			throw new IllegalArgumentException("Incorrect storage type at " + localPos + " expected to find MountedStorageBase");
		}
		mountedStorage = itemStorage;
	}

	protected Optional<MountedStorageBase> getMountedStorage() {
		AbstractContraptionEntity c = contraptionEntity.get();
		if (c == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(ContraptionHelper.getMountedStorage(c, localPos));
	}

	private static IStorageWrapper getWrapper(Level level, int contraptionEntityId, BlockPos localPos) {
		if (!(level.getEntity(contraptionEntityId) instanceof AbstractContraptionEntity contraptionEntity)) {
			return NoopStorageWrapper.INSTANCE;
		}
		MountedStorageBase itemStorage = ContraptionHelper.getMountedStorage(contraptionEntity, localPos);
		if (itemStorage == null) {
			return NoopStorageWrapper.INSTANCE;
		}

		return itemStorage.getStorageWrapper();
	}

	public Optional<AbstractContraptionEntity> getContraptionEntity() {
		return Optional.ofNullable(contraptionEntity.get());
	}

	public BlockPos getLocalPos() {
		return localPos;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		getContraptionEntity().ifPresent(c -> {
			Vec3 localPosVec = Vec3.atCenterOf(localPos);
			Vec3 newPos = c.toGlobalVector(localPosVec, 0);
			MountedStorageBase mountedStorage = ContraptionHelper.getMountedStorage(c, localPos);
			if (mountedStorage != null) {
				mountedStorage.onClose(player, newPos);
			}
		});
	}

	@Override
	public Optional<BlockPos> getBlockPosition() {
		return Optional.empty();
	}

	@Override
	public Optional<Entity> getEntity() {
		return getContraptionEntity().map(c -> c);
	}

	@Override
	public void openSettings() {
		if (isClientSide()) {
			sendToServer(data -> data.putString(ACTION_TAG, "openSettings"));
			return;
		}
		getContraptionEntity()
				.ifPresent(c -> player.openMenu(new SophisticatedMenuProvider((w, p, pl) -> instantiateSettingsContainerMenu(w, pl, c.getId(), localPos),
						Component.translatable(getSettingsTitleKey()), false), this::writeSettingsContainerMenuExtraData));
	}

	protected abstract void writeSettingsContainerMenuExtraData(FriendlyByteBuf buffer);

	protected abstract String getSettingsTitleKey();

	protected abstract MountedStorageSettingsContainerMenuBase instantiateSettingsContainerMenu(int windowId, Player player, int contraptionEntityId,
			BlockPos localPos);

	@Override
	protected boolean storageItemHasChanged() {
		return false; // the stack is only used for internal tracking in moving entities so it can't be swapped away by a player
	}

	@Override
	public boolean detectSettingsChangeAndReload() {
		if (player.level().isClientSide) {
			return storageWrapper.getContentsUuid().map(uuid -> {
				MountedStorageData storage = MountedStorageData.get();
				if (storage.removeUpdatedStorageSettingsFlag(uuid)) {
					CompoundTag contents = storage.getContents(uuid);
					storageWrapper.getSettingsHandler().reloadFrom(getSettingsTag(contents));
					return true;
				}
				return false;
			}).orElse(false);
		}
		return false;
	}

	protected abstract CompoundTag getSettingsTag(CompoundTag contents);

	@Override
	public boolean stillValid(Player player) {
		return getContraptionEntity().map(c -> c.isAlive() && player.canInteractWithEntity(c, 4.0F)).orElse(false);
	}

	@Override
	protected void sendStorageSettingsToClient() {
		if (player.level().isClientSide) {
			return;
		}

		if (lastSettingsNbt == null || !lastSettingsNbt.equals(storageWrapper.getSettingsHandler().getNbt())) {
			lastSettingsNbt = storageWrapper.getSettingsHandler().getNbt().copy();

			storageWrapper.getContentsUuid().ifPresent(uuid -> {
				CompoundTag settingsContents = new CompoundTag();
				CompoundTag settingsNbt = storageWrapper.getSettingsHandler().getNbt();
				if (!settingsNbt.isEmpty()) {
					settingsContents.put(IStorageWrapper.SETTINGS_TAG, settingsNbt);
					if (player instanceof ServerPlayer serverPlayer) {
						PacketDistributor.sendToPlayer(serverPlayer, instantiateSettingsPayload(uuid, settingsContents));
					}
				}
			});
		}
	}

	protected abstract CustomPacketPayload instantiateSettingsPayload(UUID uuid, CompoundTag settingsContents);
}
