package net.p3pp3rf1y.sophisticatedcore.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;

import java.util.UUID;

public abstract class MountedStorageSettingsContainerMenuBase extends SettingsContainerMenu<IStorageWrapper> {
	private final int contraptionEntityId;
	private final BlockPos localPos;
	private CompoundTag lastSettingsNbt = null;

	public MountedStorageSettingsContainerMenuBase(MenuType<?> menuType, int windowId, Player player, IStorageWrapper storageWrapper, int contraptionEntityId,
			BlockPos localPos) {
		super(menuType, windowId, player, storageWrapper);
		this.contraptionEntityId = contraptionEntityId;
		this.localPos = localPos;
	}

	@Override
	public void detectSettingsChangeAndReload() {
		if (player.level().isClientSide) {
			storageWrapper.getContentsUuid().ifPresent(uuid -> {
				updateFromContents(uuid);
			});
		}
	}

	protected abstract void updateFromContents(UUID uuid);

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
						PacketDistributor.sendToPlayer(serverPlayer, instantiateSettingsPayload(uuid, settingsContents));
					}
				}
			});
		}
	}

	protected abstract CustomPacketPayload instantiateSettingsPayload(UUID uuid, CompoundTag settingsContents);

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		sendStorageSettingsToClient();
	}

	public BlockPos getLocalPos() {
		return localPos;
	}

	@Override
	public BlockPos getBlockPosition() {
		return localPos;
	}
}
