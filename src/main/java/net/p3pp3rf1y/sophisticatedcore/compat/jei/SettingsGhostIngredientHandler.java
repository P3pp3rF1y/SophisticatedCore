package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsTab;

import java.util.ArrayList;
import java.util.List;

public class SettingsGhostIngredientHandler<S extends SettingsScreen> implements IGhostIngredientHandler<S> {
	@Override
	public <I> List<Target<I>> getTargets(S screen, I i, boolean b) {
		List<Target<I>> targets = new ArrayList<>();
		if (!(i instanceof ItemStack ghostStack)) {
			return targets;
		}
		screen.getSettingsTabControl().getOpenTab().ifPresent(tab -> {
			if (tab instanceof MemorySettingsTab) {
				screen.getMenu().getStorageInventorySlots().forEach(s -> {
					if (s.getItem().isEmpty()) {
						targets.add(new Target<>() {
							@Override
							public Rect2i getArea() {
								return new Rect2i(screen.getGuiLeft() + s.x, screen.getGuiTop() + s.y, 17, 17);
							}

							@Override
							public void accept(I i) {
								SophisticatedCore.PACKET_HANDLER.sendToServer(new SetMemorySlotMessage(ghostStack, s.index));
							}
						});
					}
				});
			}
		});
		return targets;
	}

	@Override
	public void onComplete() {
		//noop
	}
}
