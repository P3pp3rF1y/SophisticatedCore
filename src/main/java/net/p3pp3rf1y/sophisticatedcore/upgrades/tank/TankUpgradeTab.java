package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;

import java.util.List;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.GUI_CONTROLS;

public class TankUpgradeTab extends UpgradeSettingsTab<TankUpgradeContainer> {
	private static final TextureBlitData ARROW = new TextureBlitData(GUI_CONTROLS, new UV(97, 216), new Dimension(15, 8));

	public TankUpgradeTab(TankUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("tank"), TranslationHelper.INSTANCE.translUpgradeTooltip("tank"));
		openTabDimension = new Dimension(48, 80);
	}

	@Override
	protected void extractBg(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		super.extractBg(guiGraphics, minecraft, mouseX, mouseY);
		if (getContainer().isOpen()) {
			GuiHelper.renderSlotsBackground(guiGraphics, x + 3, y + 24, 1, 1);
			GuiHelper.renderSlotsBackground(guiGraphics, x + 24, y + 24, 1, 1);
			GuiHelper.renderSlotsBackground(guiGraphics, x + 3, y + 24 + 32, 1, 1);
			GuiHelper.renderSlotsBackground(guiGraphics, x + 24, y + 24 + 32, 1, 1);

			GuiHelper.blit(guiGraphics, x + 3 + 1, y + 24 + 18 + 3, ARROW);
			GuiHelper.blit(guiGraphics, x + 24 + 1, y + 24 + 18 + 3, ARROW);
		}
	}

	@Override
	protected void moveSlotsToTab() {
		List<Slot> slots = getContainer().getSlots();
		positionSlot(slots.get(TankUpgradeWrapper.INPUT_SLOT), screen.getGuiLeft(), screen.getGuiTop(), 4, 0);
		positionSlot(slots.get(TankUpgradeWrapper.OUTPUT_SLOT), screen.getGuiLeft(), screen.getGuiTop(), 25, 0);
		positionSlot(slots.get(TankUpgradeWrapper.INPUT_RESULT_SLOT), screen.getGuiLeft(), screen.getGuiTop(), 4, 32);
		positionSlot(slots.get(TankUpgradeWrapper.OUTPUT_RESULT_SLOT), screen.getGuiLeft(), screen.getGuiTop(), 25, 32);
	}

	private void positionSlot(Slot slot, int screenGuiLeft, int screenGuiTop, int xOffset, int yOffset) {
		slot.x = x - screenGuiLeft + xOffset;
		slot.y = y - screenGuiTop + 25 + yOffset;
	}
}
