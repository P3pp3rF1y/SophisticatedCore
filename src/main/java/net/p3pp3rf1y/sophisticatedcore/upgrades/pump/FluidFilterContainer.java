package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;

import java.util.function.Supplier;

public class FluidFilterContainer {
	private final Player player;
	private final IServerUpdater serverUpdater;
	private final Supplier<FluidFilterLogic> fluidFilterLogic;
	private static final String DATA_FLUID = "setFluid";

	public FluidFilterContainer(Player player, IServerUpdater serverUpdater, Supplier<FluidFilterLogic> fluidFilterLogic) {
		this.player = player;
		this.serverUpdater = serverUpdater;
		this.fluidFilterLogic = fluidFilterLogic;
	}

	public FluidStack getFluid(int index) {
		return fluidFilterLogic.get().getFluid(index);
	}

	private void setFluid(int index, FluidStack fluid) {
		fluidFilterLogic.get().setFluid(index, fluid);
		serverUpdater.sendDataToServer(() -> serializeSetFluidData(index, fluid));
	}

	private CompoundTag serializeSetFluidData(int index, FluidStack fluid) {
		CompoundTag ret = new CompoundTag();
		CompoundTag fluidNbt = new CompoundTag();
		fluidNbt.putInt("index", index);
		//noinspection ConstantConditions
		fluidNbt.put("fluid", fluid.writeToNBT(new CompoundTag()));
		ret.put(DATA_FLUID, fluidNbt);
		return ret;
	}

	public boolean handleMessage(CompoundTag data) {
		if (data.contains(DATA_FLUID)) {
			CompoundTag fluidData = data.getCompound(DATA_FLUID);
			FluidStack fluid = FluidStack.loadFluidStackFromNBT(data.getCompound("fluid"));
			if (!fluid.isEmpty()) {
				setFluid(fluidData.getInt("index"), fluid);
			}
			return true;
		}
		return false;
	}

	public int getNumberOfFluidFilters() {
		return fluidFilterLogic.get().getNumberOfFluidFilters();
	}

	public void slotClick(int index) {
		ItemStack carried = player.containerMenu.getCarried();
		if (carried.isEmpty()) {
			setFluid(index, FluidStack.EMPTY);
			return;
		}

		carried.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(itemFluidHandler -> {
			FluidStack containedFluid = itemFluidHandler.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
			if (!containedFluid.isEmpty()) {
				setFluid(index, containedFluid);
			}
		});
	}
}
