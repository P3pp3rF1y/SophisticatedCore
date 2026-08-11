package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.FluidFilterContainer;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

public class VoidUpgradeContainer extends UpgradeContainerBase<VoidUpgradeWrapper, VoidUpgradeContainer> {
	private static final String DATA_SHOULD_WORKD_IN_GUI = "shouldWorkdInGUI";
	private static final String DATA_VOID_TYPE = "voidType";
	private final FilterLogicContainer<FilterLogic> filterLogicContainer;
	private final FluidFilterContainer fluidFilterContainer;
	private boolean syncingFilters = false;

	public VoidUpgradeContainer(Player player, int containerId, VoidUpgradeWrapper wrapper,
			UpgradeContainerType<VoidUpgradeWrapper, VoidUpgradeContainer> type) {
		super(player, containerId, wrapper, type);
		fluidFilterContainer = new FluidFilterContainer(player, this, supplyFromWrapper(VoidUpgradeWrapper::getFluidFilterLogic)) {
			@Override
			public void setFluid(int index, FluidStack fluid) {
				super.setFluid(index, fluid);
				if (!syncingFilters) {
					syncingFilters = true;
					upgradeWrapper.getFilterLogic().getFilterHandler().setStackInSlot(index, ItemStack.EMPTY);
					syncingFilters = false;
				}
			}
		};
		filterLogicContainer = new FilterLogicContainer<>(supplyFromWrapper(VoidUpgradeWrapper::getFilterLogic), this, slots::add,
				(slot, button) -> clearFluidFilter(slot));
	}

	@Override
	public void handlePacket(CompoundTag data) {
		data.getBoolean(DATA_SHOULD_WORKD_IN_GUI).ifPresent(this::setShouldWorkdInGUI);
		data.getString(DATA_VOID_TYPE).ifPresent(voidTypeName -> setVoidType(VoidType.fromName(voidTypeName)));
		filterLogicContainer.handlePacket(data);
		fluidFilterContainer.handlePacket(data);
	}

	public FilterLogicContainer<FilterLogic> getFilterLogicContainer() {
		return filterLogicContainer;
	}

	public FluidFilterContainer getFluidFilterContainer() {
		return fluidFilterContainer;
	}

	private void clearFluidFilter(int slot) {
		syncingFilters = true;
		fluidFilterContainer.setFluid(slot, FluidStack.EMPTY);
		syncingFilters = false;
	}

	public void setShouldWorkdInGUI(boolean shouldWorkdInGUI) {
		upgradeWrapper.setShouldWorkdInGUI(shouldWorkdInGUI);
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), DATA_SHOULD_WORKD_IN_GUI, shouldWorkdInGUI));
	}

	public void setVoidType(VoidType voidType) {
		upgradeWrapper.setVoidType(voidType);
		sendDataToServer(() -> NBTHelper.putEnumConstant(new CompoundTag(), DATA_VOID_TYPE, voidType));
	}

	public boolean shouldWorkInGUI() {
		return upgradeWrapper.shouldWorkInGUI();
	}

	public VoidType getVoidType() {
		return upgradeWrapper.getVoidType();
	}
}
