package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.Optional;

public class JukeboxUpgradeContainer extends UpgradeContainerBase<JukeboxUpgradeWrapper, JukeboxUpgradeContainer> {

	private static final String ACTION_DATA = "action";

	public JukeboxUpgradeContainer(Player player, int upgradeContainerId, JukeboxUpgradeWrapper upgradeWrapper, UpgradeContainerType<JukeboxUpgradeWrapper, JukeboxUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
		for (int slot = 0; slot < upgradeWrapper.getDiscInventory().size(); slot++) {
			int finalSlot = slot;
			slots.add(new ResourceHandlerSlot(upgradeWrapper.getDiscInventory(),
					(index, resource, amount) -> {
						upgradeWrapper.getDiscInventory().set(index, resource, amount);
						if (upgradeWrapper.isPlaying() && finalSlot == upgradeWrapper.getDiscSlotActive()) {
							upgradeWrapper.stop(player);
						}
					},
					slot, -100, -100) {
			});
		}
	}

	@Override
	public void handlePacket(CompoundTag data) {
		data.getString(ACTION_DATA).ifPresent(actionName -> {
			switch (actionName) {
				case "play" -> {
					if (player.containerMenu instanceof StorageContainerMenuBase<?> storageContainerMenu) {
						storageContainerMenu.getBlockPosition().ifPresentOrElse(pos -> upgradeWrapper.play(player.level(), pos), () -> upgradeWrapper.play(storageContainerMenu.getEntity().orElse(player)));
					}
				}
				case "stop" -> upgradeWrapper.stop(player);
				case "next" -> upgradeWrapper.next();
				case "previous" -> upgradeWrapper.previous();
			}
		});
		data.getBoolean("shuffle").ifPresent(upgradeWrapper::setShuffleEnabled);
		NBTHelper.getEnumConstant(data, "repeat", RepeatMode::fromName).ifPresent(upgradeWrapper::setRepeatMode);

	}

	public void play() {
		sendDataToServer(() -> NBTHelper.putString(new CompoundTag(), ACTION_DATA, "play"));
	}

	public void stop() {
		sendDataToServer(() -> NBTHelper.putString(new CompoundTag(), ACTION_DATA, "stop"));
	}

	public void next() {
		sendDataToServer(() -> NBTHelper.putString(new CompoundTag(), ACTION_DATA, "next"));
	}

	public void previous() {
		sendDataToServer(() -> NBTHelper.putString(new CompoundTag(), ACTION_DATA, "previous"));
	}

	public boolean isShuffleEnabled() {
		return upgradeWrapper.isShuffleEnabled();
	}

	public void toggleShuffle() {
		boolean newValue = !upgradeWrapper.isShuffleEnabled();
		upgradeWrapper.setShuffleEnabled(newValue);
		sendBooleanToServer("shuffle", newValue);
	}

	public RepeatMode getRepeatMode() {
		return upgradeWrapper.getRepeatMode();
	}

	public void toggleRepeat() {
		RepeatMode newValue = upgradeWrapper.getRepeatMode().next();
		upgradeWrapper.setRepeatMode(newValue);
		sendDataToServer(() -> NBTHelper.putEnumConstant(new CompoundTag(), "repeat", newValue));
	}

	public Optional<Slot> getDiscSlotActive() {
		int discSlotActive = upgradeWrapper.getDiscSlotActive();
		return discSlotActive > -1 ? Optional.of(slots.get(discSlotActive)) : Optional.empty();
	}

	public long getDiscFinishTime() {
		return upgradeWrapper.getDiscFinishTime();
	}
}
