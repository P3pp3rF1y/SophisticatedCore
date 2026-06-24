package net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

public abstract class BlockConverterUpgradeContainer<R extends SingleItemRecipe, W extends BlockConverterUpgradeWrapper<?, ?>, C extends BlockConverterUpgradeContainer<R, W, C, RC>, RC extends BlockConverterRecipeContainer<R, W, RC, C>>
		extends
			UpgradeContainerBase<W, C>
		implements
			IRecentCraftedResultsRefresh {
	private static final String DATA_SHIFT_CLICK_INTO_STORAGE = "shiftClickIntoStorage";
	private static final String DATA_REFILL_INPUT = "refill_input";
	private final RC recipeContainer;

	public BlockConverterUpgradeContainer(Player player, int upgradeContainerId, W upgradeWrapper, UpgradeContainerType<W, C> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
		ContainerLevelAccess worldPosCallable = player.level().isClientSide
				? ContainerLevelAccess.NULL
				: ContainerLevelAccess.create(player.level(), player.blockPosition());
		recipeContainer = createRecipeContainer(worldPosCallable);
	}

	public RC getRecipeContainer() {
		return recipeContainer;
	}

	protected abstract RC createRecipeContainer(ContainerLevelAccess worldPosCallable);

	@Override
	public void handlePacket(CompoundTag data) {
		data.getBoolean(DATA_SHIFT_CLICK_INTO_STORAGE).ifPresent(this::setShiftClickIntoStorage);
		data.getBoolean(DATA_REFILL_INPUT).ifPresent(this::setRefillInput);
		recipeContainer.handlePacket(data);
	}

	public boolean shouldShiftClickIntoStorage() {
		return upgradeWrapper.shouldShiftClickIntoStorage();
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		upgradeWrapper.setShiftClickIntoStorage(shiftClickIntoStorage);
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), DATA_SHIFT_CLICK_INTO_STORAGE, shiftClickIntoStorage));
	}

	public boolean shouldRefillInput() {
		return upgradeWrapper.shouldRefillInput();
	}

	public void setRefillInput(boolean refillInput) {
		upgradeWrapper.setRefillInput(refillInput);
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), DATA_REFILL_INPUT, refillInput));
	}

	@Override
	public void refreshRecentResultsFromClientCache() {
		recipeContainer.refreshRecentResultsFromClientCache();
	}

	@Override
	public boolean mergeIntoStorageFirst(Slot slot) {
		return recipeContainer.isNotResultSlot(slot) || shouldShiftClickIntoStorage();
	}

	@Override
	public boolean allowsPickupAll(Slot slot) {
		return recipeContainer.isNotResultSlot(slot);
	}

	@Override
	public int getRepeatedQuickMoveLimit(Slot slot, ItemStack transferredStack) {
		return !recipeContainer.isNotResultSlot(slot) && shouldRefillInput() ? transferredStack.getMaxStackSize() : 0;
	}
}
