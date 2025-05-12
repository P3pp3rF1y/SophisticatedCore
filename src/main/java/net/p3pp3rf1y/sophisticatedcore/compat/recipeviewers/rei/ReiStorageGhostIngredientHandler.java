package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IFilterSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SetGhostSlotPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
		StorageContainerMenuBase<?> menu = context.getScreen().getMenu();

		if (stack.getStack().getValue() instanceof ItemStack ghostStack) {
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

		return targets.stream();
	}

	@Override
	public <R extends Screen> boolean isHandingScreen(R screen) {
		return this.handingScreenClass.isInstance(screen);
	}
}