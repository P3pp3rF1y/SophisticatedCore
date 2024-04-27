package net.p3pp3rf1y.sophisticatedcore.compat.chipped;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

public class BlockTransformationUpgradeContainer extends UpgradeContainerBase<BlockTransformationUpgradeWrapper, BlockTransformationUpgradeContainer> {
	private static final String DATA_SHIFT_CLICK_INTO_STORAGE = "shiftClickIntoStorage";
	private final BlockTransformationRecipeContainer recipeContainer;

	public BlockTransformationUpgradeContainer(Player player, int upgradeContainerId, BlockTransformationUpgradeWrapper upgradeWrapper, UpgradeContainerType<BlockTransformationUpgradeWrapper, BlockTransformationUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
		ContainerLevelAccess worldPosCallable = player.level().isClientSide ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(player.level(), player.blockPosition());
		recipeContainer = new BlockTransformationRecipeContainer(this, upgradeWrapper.getRecipeType(), slots::add, this, worldPosCallable);
	}

	@Override
	public void handlePacket(CompoundTag data) {
		if (data.contains(DATA_SHIFT_CLICK_INTO_STORAGE)) {
			setShiftClickIntoStorage(data.getBoolean(DATA_SHIFT_CLICK_INTO_STORAGE));
		} else {
			recipeContainer.handlePacket(data);
		}
	}

	public boolean shouldShiftClickIntoStorage() {
		return upgradeWrapper.shouldShiftClickIntoStorage();
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		upgradeWrapper.setShiftClickIntoStorage(shiftClickIntoStorage);
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), DATA_SHIFT_CLICK_INTO_STORAGE, shiftClickIntoStorage));
	}

	public BlockTransformationRecipeContainer getRecipeContainer() {
		return recipeContainer;
	}

	@Override
	public boolean mergeIntoStorageFirst(Slot slot) {
		return recipeContainer.isNotResultSlot(slot) || shouldShiftClickIntoStorage();
	}

	@Override
	public boolean allowsPickupAll(Slot slot) {
		return recipeContainer.isNotResultSlot(slot);
	}
}
