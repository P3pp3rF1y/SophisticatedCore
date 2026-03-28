package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

public abstract class UpgradeInventoryPartBase<C extends UpgradeContainerBase<?, ?>> {
	protected final C container;
	protected final int upgradeSlot;

	protected UpgradeInventoryPartBase(int upgradeSlot, C container) {
		this.container = container;
		this.upgradeSlot = upgradeSlot;
	}

	public abstract void extract(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY);

	public abstract boolean handleMouseReleased(MouseButtonEvent event);

	public abstract void extractErrorOverlay(GuiGraphicsExtractor guiGraphics);

	public abstract void extractTooltip(StorageScreenBase<?> screen, GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY);
}
