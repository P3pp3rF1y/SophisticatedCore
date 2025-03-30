package net.p3pp3rf1y.sophisticatedcore.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;

public abstract class MountedStorageSettingsContainerMenuBase extends SettingsContainerMenu<IStorageWrapper> {
	private final int contraptionEntityId;
	private final BlockPos localPos;
	private CompoundTag lastSettingsNbt = null;

	public MountedStorageSettingsContainerMenuBase(MenuType<?> menuType, int windowId, Player player, int contraptionEntityId, BlockPos localPos) {
		super(menuType, windowId, player, getWrapper(player.level(), contraptionEntityId, localPos));
		this.contraptionEntityId = contraptionEntityId;
		this.localPos = localPos;
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


	@Override
	public void detectSettingsChangeAndReload() {
		if (player.level().isClientSide) {
			storageWrapper.getContentsUuid().ifPresent(uuid -> {
				MountedStorageData storage = MountedStorageData.get(uuid);
				if (storage.removeUpdatedStorageSettingsFlag(uuid)) {
					CompoundTag contents = storage.getContents();
					storageWrapper.getSettingsHandler().reloadFrom(getSettingsTag(contents));
				}
			});
		}
	}

	protected abstract CompoundTag getSettingsTag(CompoundTag contents);

	public int getContraptionEntityId() {
		return contraptionEntityId;
	}

	private void sendStorageSettingsToClient() {
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
						PacketHandler.INSTANCE.sendToClient(serverPlayer, new MountedStorageContentsMessage(uuid, settingsContents));
					}
				}
			});
		}
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		sendStorageSettingsToClient();
	}

	public BlockPos getLocalPos() {
		return localPos;
	}
}
