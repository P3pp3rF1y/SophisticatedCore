package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SetMemorySlotPayload;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsTab;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class SettingsGhostIngredientHandler<S extends SettingsScreen> implements DraggableStackVisitor<S> {
	private final Class<S> handingScreenClass;

	public SettingsGhostIngredientHandler(Class<S> handingScreenClass) {
		this.handingScreenClass = handingScreenClass;
	}

	@Override
	public DraggedAcceptorResult acceptDraggedStack(DraggingContext<S> context, DraggableStack stack) {
		Stream<BoundsProvider> bounds = getDraggableAcceptingBounds(context, stack);
		Point cursor = context.getCurrentPosition();
		if (cursor != null) {
			int x = cursor.getX();
			int y = cursor.getY();
			Optional<BoundsProvider> target = bounds.filter(b -> {
				AABB box = b.bounds().bounds();
				double minX = box.minX;
				double minY = box.minY;
				double maxX = box.maxX;
				double maxY = box.maxY;
				return x >= minX && x <= maxX && y >= minY && y <= maxY && b instanceof GhostTarget;
			}).findFirst();
			if (target.isPresent()) {
				//noinspection unchecked
				GhostTarget<ItemStack, ? extends SettingsScreen> ghost = (GhostTarget<ItemStack, ? extends SettingsScreen>) target.get();
				Object held = stack.getStack().getValue();
				if (held instanceof ItemStack item) {
					ghost.accept(item);
					return DraggedAcceptorResult.CONSUMED;
				}
			}
		}
		return DraggableStackVisitor.super.acceptDraggedStack(context, stack);
	}

	@Override
	public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<S> context, DraggableStack stack) {
		List<BoundsProvider> targets = new ArrayList<>();
		SettingsScreen screen = context.getScreen();

		if (stack.getStack().getValue() instanceof ItemStack ghostStack) {
			screen.getSettingsTabControl().getOpenTab().ifPresent(tab -> {
				if (tab instanceof MemorySettingsTab) {
					screen.getMenu().getStorageInventorySlots().forEach(s -> {
						if (s.getItem().isEmpty()) {
							targets.add(new GhostTarget<>(screen, ghostStack, s));
						}
					});
				}
			});
		}

		return targets.stream();
	}

	@Override
	public <R extends Screen> boolean isHandingScreen(R screen) {
		return this.handingScreenClass.isInstance(screen);
	}

	private static class GhostTarget<I, S extends SettingsScreen> implements BoundsProvider {
		private final Rectangle area;
		private final Slot slot;
		private final ItemStack stack;

		public GhostTarget(S screen, ItemStack stack, Slot slot) {
			this.slot = slot;
			this.stack = stack;
			this.area = new Rectangle(screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y, 16, 16);
		}

		public void accept(I ingredient) {
			PacketDistributor.sendToServer(new SetMemorySlotPayload(stack, slot.index));
		}

		@Override
		public VoxelShape bounds() {
			return BoundsProvider.fromRectangle(area);
		}
	}
}