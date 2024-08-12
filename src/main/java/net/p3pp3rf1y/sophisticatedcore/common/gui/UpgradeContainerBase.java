package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.network.SyncContainerClientDataPayload;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class UpgradeContainerBase<W extends IUpgradeWrapper, C extends UpgradeContainerBase<W, C>> implements IServerUpdater {
	protected final ArrayList<Slot> slots = new ArrayList<>();

	private final int upgradeContainerId;

	protected W upgradeWrapper;
	protected final Player player;
	private final UpgradeContainerType<W, C> type;
	private boolean isOpen = false;

	protected UpgradeContainerBase(Player player, int upgradeContainerId, W upgradeWrapper, UpgradeContainerType<W, C> type) {
		this.upgradeContainerId = upgradeContainerId;
		this.upgradeWrapper = upgradeWrapper;
		this.player = player;
		this.type = type;
	}

	public List<Slot> getSlots() {
		return slots;
	}

	public UpgradeContainerType<W, C> getType() {
		return type;
	}

	public void setIsOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}

	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public void sendBooleanToServer(String key, boolean value) {
		if (!player.level().isClientSide) {
			return;
		}
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), key, value));
	}

	@Override
	public void sendDataToServer(Supplier<CompoundTag> supplyData) {
		if (!player.level().isClientSide) {
			return;
		}
		CompoundTag data = supplyData.get();
		data.putInt("containerId", upgradeContainerId);
		PacketDistributor.sendToServer(new SyncContainerClientDataPayload(data));
	}

	public void onInit() {
		//noop by default
	}

	public abstract void handlePacket(CompoundTag data);

	public ItemStack getUpgradeStack() {
		return upgradeWrapper.getUpgradeStack();
	}

	public W getUpgradeWrapper() {
		return upgradeWrapper;
	}

	public void setUpgradeWrapper(IUpgradeWrapper updatedUpgradeWrapper) {
		//noinspection unchecked - only used in logic that makes sure the item is the same and the same item will have a wrapper with the same (W) class
		upgradeWrapper = (W) updatedUpgradeWrapper;
	}

	public boolean containsSlot(Slot slot) {
		for (Slot containerSlot : slots) {
			if (containerSlot == slot) {
				return true;
			}
		}
		return false;
	}

	public ItemStack getSlotStackToTransfer(Slot slot) {
		return slot.getItem();
	}

	public void onTakeFromSlot(Slot slot, Player player, ItemStack slotStack) {
		slot.onTake(player, slotStack);
	}

	@SuppressWarnings({"unused", "java:S1172"}) //parameter is used in overrides
	public boolean mergeIntoStorageFirst(Slot slot) {
		return true;
	}

	@SuppressWarnings({"unused", "java:S1172"}) //parameter is used in overrides
	public boolean allowsPickupAll(Slot slot) {
		return true;
	}

	public int getUpgradeContainerId() {
		return upgradeContainerId;
	}
}
