package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.*;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IFilterSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SetGhostSlotPayload;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ReiStorageGhostIngredientHandler<S extends StorageScreenBase<?>> implements DraggableStackVisitor<S> {
	private final Class<S> handingScreenClass;

	public ReiStorageGhostIngredientHandler(Class<S> handingScreenClass) {
		this.handingScreenClass = handingScreenClass;
	}

	@Override
	public DraggedAcceptorResult acceptDraggedStack(DraggingContext<S> context, DraggableStack stack) {
        Point cursor = context.getCurrentPosition();
		if (cursor != null) {
            Optional<ReiGhostTarget> target = getDraggableAcceptingBounds(context, stack)
					.map(ReiGhostTarget.class::cast)
					.filter(b -> b.contains(cursor.getX(), cursor.getY()))
					.findFirst();
			if (target.isPresent()) {
				target.get().accept();
				return DraggedAcceptorResult.CONSUMED;
			}
		}
		return DraggableStackVisitor.super.acceptDraggedStack(context, stack);
	}

	@Override
	public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<S> context, DraggableStack stack) {
		List<BoundsProvider> targets = new ArrayList<>();
		StorageScreenBase<?> screen = context.getScreen();

		if (stack.getStack().getType() == VanillaEntryTypes.ITEM) {
			StorageContainerMenuBase<?> menu = context.getScreen().getMenu();
			if (stack.getStack().getValue() instanceof ItemStack ghostStack) {
				FluidStack fluidStack = CapabilityHelper.getFromCapability(ghostStack, Capabilities.FluidHandler.ITEM,
						null, fluidHandler -> fluidHandler.getTanks() > 0 ? fluidHandler.getFluidInTank(0) : FluidStack.EMPTY, FluidStack.EMPTY);
				if (!fluidStack.isEmpty()) {
					screen.getUpgradeSettingsControl()
							.getOpenTab()
							.filter(tab -> tab instanceof PumpUpgradeTab.Advanced)
							.map(PumpUpgradeTab.Advanced.class::cast)
							.ifPresent(pumpUpgradeTab -> addFluidTargets(pumpUpgradeTab, fluidStack, targets));
					return targets.stream();
				}
				menu.getOpenContainer().ifPresent(c -> c.getSlots().forEach(s -> {
					if (s instanceof IFilterSlot && s.mayPlace(ghostStack)) {
						targets.add(new ReiGhostTarget() {
							@Override
							public VoxelShape bounds() {
								return DraggableBoundsProvider.fromRectangle(new Rectangle(screen.getGuiLeft() + s.x, screen.getGuiTop() + s.y, 17, 17));
							}

							@Override
							public void accept() {
								PacketDistributor.sendToServer(new SetGhostSlotPayload(ghostStack, s.index));
							}
						});
					}
				}));
			}
		} else if (stack.getStack().getType() == VanillaEntryTypes.FLUID) {
			screen.getUpgradeSettingsControl()
					.getOpenTab()
					.filter(tab -> tab instanceof PumpUpgradeTab.Advanced)
					.map(PumpUpgradeTab.Advanced.class::cast)
					.ifPresent(pumpUpgradeTab -> {
						dev.architectury.fluid.FluidStack ghostFluidStack = stack.getStack().castValue();
						addFluidTargets(pumpUpgradeTab, new FluidStack(ghostFluidStack.getFluid(), (int)ghostFluidStack.getAmount()), targets);
					});
		}

		return targets.stream();
	}

	private void addFluidTargets(PumpUpgradeTab.Advanced pumpUpgradeTab, FluidStack ghostFluid, List<BoundsProvider> targets) {
		List<Position> slotTopLeftPositions = pumpUpgradeTab.getFluidFilterControl().getSlotTopLeftPositions();
		AtomicInteger slot = new AtomicInteger();
		for (slot.set(0); slot.get() < slotTopLeftPositions.size(); slot.incrementAndGet()) {
			Position position = slotTopLeftPositions.get(slot.get());
			targets.add(new ReiGhostTarget() {
				private final int slotIndex = slot.get();

				@Override
				public VoxelShape bounds() {
					return DraggableBoundsProvider.fromRectangle(new Rectangle(position.x(), position.y(), 17, 17));
				}

				@Override
				public void accept() {
					pumpUpgradeTab.getFluidFilterControl().setFluid(slotIndex, ghostFluid);
				}
			});
		}
	}

	@Override
	public <R extends Screen> boolean isHandingScreen(R screen) {
		return this.handingScreenClass.isInstance(screen);
	}
}