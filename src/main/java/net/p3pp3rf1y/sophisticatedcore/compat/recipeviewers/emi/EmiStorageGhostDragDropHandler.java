package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import com.google.common.collect.Maps;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IFilterSlot;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SetGhostSlotMessage;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeTab;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class EmiStorageGhostDragDropHandler<T extends StorageScreenBase<?>> implements EmiDragDropHandler<T> {
    private final BiFunction<T, EmiIngredient, Map<Bounds, Consumer<EmiIngredient>>> bounds;

    public EmiStorageGhostDragDropHandler() {
        this.bounds = (screen, ingredient) -> {
            Map<Bounds, Consumer<EmiIngredient>> map = Maps.newHashMap();
            if (ingredient.getEmiStacks().isEmpty()) {
                return map;
            }

            EmiStack emiGhostStack = ingredient.getEmiStacks().get(0);
            if (!emiGhostStack.isEmpty()) {
                ItemStack ghostStack = emiGhostStack.getItemStack();
                FluidStack fluidStack = ghostStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                        .filter(fluidHandler -> fluidHandler.getTanks() > 0)
                        .map(fluidHandler -> fluidHandler.getFluidInTank(0)).orElse(FluidStack.EMPTY);

                if (!fluidStack.isEmpty()) {
                    screen.getUpgradeSettingsControl()
                            .getOpenTab()
                            .filter(tab -> tab instanceof PumpUpgradeTab.Advanced)
                            .map(PumpUpgradeTab.Advanced.class::cast)
                            .ifPresent(pumpUpgradeTab -> addFluidTargets(pumpUpgradeTab, fluidStack, map));
                }
                screen.getMenu().getOpenContainer().ifPresent(c -> c.getSlots().forEach(s -> {
                    if (s instanceof IFilterSlot && s.mayPlace(ghostStack)) {
                        map.put(
                                new Bounds(screen.getLeftX() + s.x, screen.getTopY() + s.y, 18, 18),
                                (i) -> PacketHandler.INSTANCE.sendToServer(new SetGhostSlotMessage(ghostStack, s.index)));
                    }
                }));
            } else if (emiGhostStack.getKey() instanceof Fluid fluid) {
                screen.getUpgradeSettingsControl()
                        .getOpenTab()
                        .filter(tab -> tab instanceof PumpUpgradeTab.Advanced)
                        .map(PumpUpgradeTab.Advanced.class::cast)
                        .ifPresent(pumpUpgradeTab -> addFluidTargets(pumpUpgradeTab, new FluidStack(fluid, 1), map));
            }
            return map;
        };
    }

    @Override
    public boolean dropStack(T screen, EmiIngredient stack, int x, int y) {
        Map<Bounds, Consumer<EmiIngredient>> bounds = this.bounds.apply(screen, stack);
        for (Bounds b : bounds.keySet()) {
            if (b.contains(x, y)) {
                bounds.get(b).accept(stack);
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(T screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta) {
        for (Bounds b : this.bounds.apply(screen, dragged).keySet()) {
            draw.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), 0x8822BB33);
        }
    }

    private void addFluidTargets(PumpUpgradeTab.Advanced pumpUpgradeTab, FluidStack ghostFluid, Map<Bounds, Consumer<EmiIngredient>> map) {
        List<Position> slotTopLeftPositions = pumpUpgradeTab.getFluidFilterControl().getSlotTopLeftPositions();
        for (int slot = 0; slot < slotTopLeftPositions.size(); slot++) {
            Position position = slotTopLeftPositions.get(slot);
            int finalSlot = slot;
            map.put(
                    new Bounds(position.x(), position.y(), 17, 17),
                    ingredient -> pumpUpgradeTab.getFluidFilterControl().setFluid(finalSlot, ghostFluid)
            );
        }
    }
}