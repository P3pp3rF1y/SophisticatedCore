package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IFilterSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SetGhostSlotMessage;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeTab;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class JeiStorageGhostIngredientHandler<S extends StorageScreenBase<?>> implements IGhostIngredientHandler<S> {
	@Override
	public <I> List<Target<I>> getTargetsTyped(S gui, ITypedIngredient<I> ingredient, boolean doStart) {
		List<Target<I>> targets = new ArrayList<>();
		if (ingredient.getType() == VanillaTypes.ITEM_STACK) {
			StorageContainerMenuBase<?> container = gui.getMenu();
			ingredient.getItemStack().ifPresent(ghostStack -> {
				FluidStack fluidStack = ghostStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).filter(fluidHandler -> fluidHandler.getTanks() > 0)
						.map(fluidHandler -> fluidHandler.getFluidInTank(0)).orElse(FluidStack.EMPTY);

				if (!fluidStack.isEmpty()) {
					gui.getUpgradeSettingsControl().getOpenTab().filter(tab -> tab instanceof PumpUpgradeTab.Advanced).map(PumpUpgradeTab.Advanced.class::cast)
							.ifPresent(pumpUpgradeTab -> addFluidTargets(pumpUpgradeTab, fluidStack, targets));
					return;
				}
				container.getOpenContainer().ifPresent(c -> c.getSlots().forEach(s -> {
					if (s instanceof IFilterSlot && s.mayPlace(ghostStack)) {
						targets.add(new Target<>() {
							@Override
							public Rect2i getArea() {
								return new Rect2i(gui.getGuiLeft() + s.x, gui.getGuiTop() + s.y, 17, 17);
							}

							@Override
							public void accept(I i) {
								PacketHandler.INSTANCE.sendToServer(new SetGhostSlotMessage(ghostStack, s.index));
							}
						});
					}
				}));
			});
		} else if (ingredient.getType() == ForgeTypes.FLUID_STACK) {
			gui.getUpgradeSettingsControl().getOpenTab().filter(tab -> tab instanceof PumpUpgradeTab.Advanced).map(PumpUpgradeTab.Advanced.class::cast)
					.ifPresent(pumpUpgradeTab -> ForgeTypes.FLUID_STACK.castIngredient(ingredient.getIngredient())
							.ifPresent(ghostFluid -> addFluidTargets(pumpUpgradeTab, ghostFluid, targets)));
		}
		return targets;
	}

	private <I> void addFluidTargets(PumpUpgradeTab.Advanced pumpUpgradeTab, FluidStack ghostFluid, List<Target<I>> targets) {
		List<Position> slotTopLeftPositions = pumpUpgradeTab.getFluidFilterControl().getSlotTopLeftPositions();
		AtomicInteger slot = new AtomicInteger();
		for (slot.set(0); slot.get() < slotTopLeftPositions.size(); slot.incrementAndGet()) {
			Position position = slotTopLeftPositions.get(slot.get());
			targets.add(new Target<>() {
				private final int slotIndex = slot.get();

				@Override
				public Rect2i getArea() {
					return new Rect2i(position.x(), position.y(), 17, 17);
				}

				@Override
				public void accept(I i) {
					pumpUpgradeTab.getFluidFilterControl().setFluid(slotIndex, ghostFluid);
				}
			});
		}
	}

	@Override
	public void onComplete() {
		// noop
	}
}
