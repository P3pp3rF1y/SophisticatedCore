package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

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

	public void setFluid(int index, FluidStack fluid) {
		fluidFilterLogic.get().setFluid(index, fluid);
		serverUpdater.sendDataToServer(() -> serializeSetFluidData(index, fluid));
	}

	private CompoundTag serializeSetFluidData(int index, FluidStack fluid) {
		CompoundTag ret = new CompoundTag();
		CompoundTag fluidNbt = new CompoundTag();
		fluidNbt.putInt("index", index);
		fluidNbt.put("fluid", NBTHelper.serializeFluidToTag(fluid).orElse(new CompoundTag()));
		ret.put(DATA_FLUID, fluidNbt);
		return ret;
	}

	public boolean handlePacket(CompoundTag data) {
		if (data.contains(DATA_FLUID)) {
			data.getCompound(DATA_FLUID).ifPresent(fluidData -> {
				FluidStack fluid = NBTHelper.deserializeFluidFromTag(fluidData.getCompoundOrEmpty("fluid")).orElse(FluidStack.EMPTY);
				setFluid(fluidData.getIntOr("index", 0), fluid);
			});
			return true;
		}
		return false;
	}

	public int getNumberOfFluidFilters() {
		return fluidFilterLogic.get().getNumberOfFluidFilters();
	}

	public boolean slotClick(int index) {
		ItemStack carried = player.containerMenu.getCarried();
		if (carried.isEmpty()) {
			setFluid(index, FluidStack.EMPTY);
			return true;
		}

		FluidStack containedFluid = getContainedFluid(carried);
		if (containedFluid.isEmpty()) {
			return false;
		}

		setFluid(index, containedFluid);
		return true;
	}

	public static FluidStack getContainedFluid(ItemStack stack) {
		if (stack.isEmpty()) {
			return FluidStack.EMPTY;
		}

		return CapabilityHelper.getFromCapability(ItemAccess.forStack(stack), Capabilities.Fluid.ITEM,
				fluidHandler -> fluidHandler.size() > 0 ? fluidHandler.getResource(0).toStack(FluidType.BUCKET_VOLUME) : FluidStack.EMPTY, FluidStack.EMPTY);
	}
}
