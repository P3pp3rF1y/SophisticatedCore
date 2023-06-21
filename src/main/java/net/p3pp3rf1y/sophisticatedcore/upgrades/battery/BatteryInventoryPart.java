package net.p3pp3rf1y.sophisticatedcore.upgrades.battery;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeInventoryPartBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BatteryInventoryPart extends UpgradeInventoryPartBase<BatteryUpgradeContainer> {
	private static final TextureBlitData TANK_BACKGROUND_TOP = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 30), Dimension.SQUARE_18);
	private static final TextureBlitData TANK_BACKGROUND_MIDDLE = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 48), Dimension.SQUARE_18);
	private static final TextureBlitData TANK_BACKGROUND_BOTTOM = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(29, 66), Dimension.SQUARE_18);
	private static final TextureBlitData OVERLAY = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(47, 56), new Dimension(16, 18));
	private static final TextureBlitData CHARGE_SEGMENT = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(47, 74), new Dimension(16, 6));
	private static final TextureBlitData CONNECTION_TOP = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(47, 48), new Dimension(16, 4));
	private static final TextureBlitData CONNECTION_BOTTOM = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(47, 52), new Dimension(16, 4));
	private final Position pos;
	private final int height;
	private final StorageScreenBase<?> screen;
	private static final int TOP_BAR_COLOR = 0xff1a1a;
	private static final int BOTTOM_BAR_COLOR = 0xffff40;

	public BatteryInventoryPart(int upgradeSlot, BatteryUpgradeContainer container, Position pos, int height, StorageScreenBase<?> screen) {
		super(upgradeSlot, container);
		this.pos = pos;
		this.height = height;
		this.screen = screen;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		GuiHelper.blit(guiGraphics, getTankLeft(), pos.y(), TANK_BACKGROUND_TOP);
		int yOffset = 18;
		for (int i = 0; i < (height - 36) / 18; i++) {
			GuiHelper.blit(guiGraphics, getTankLeft(), pos.y() + yOffset, TANK_BACKGROUND_MIDDLE);
			yOffset += 18;
		}
		GuiHelper.blit(guiGraphics, getTankLeft(), pos.y() + yOffset, TANK_BACKGROUND_BOTTOM);

		yOffset = 0;
		for (int i = 0; i < height / 18; i++) {
			GuiHelper.blit(guiGraphics, getTankLeft() + 1, pos.y() + yOffset, OVERLAY);
			yOffset += 18;
		}

		renderCharge(guiGraphics);

		GuiHelper.blit(guiGraphics, getTankLeft() + 1, pos.y(), CONNECTION_TOP);
		GuiHelper.blit(guiGraphics, getTankLeft() + 1, pos.y() + height - 4, CONNECTION_BOTTOM);
	}

	private int getTankLeft() {
		return pos.x() + 9;
	}

	@Override
	public boolean handleMouseReleased(double mouseX, double mouseY, int button) {
		return false;
	}

	@Override
	public void renderErrorOverlay(GuiGraphics guiGraphics) {
		screen.renderOverlay(guiGraphics, StorageScreenBase.ERROR_SLOT_COLOR, getTankLeft() + 1, pos.y() + 1, 16, height - 2);
	}

	@Override
	public void renderTooltip(StorageScreenBase<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		int screenX = screen.getGuiLeft() + pos.x() + 10;
		int screenY = screen.getGuiTop() + pos.y() + 1;
		if (mouseX >= screenX && mouseX < screenX + 16 && mouseY >= screenY && mouseY < screenY + height - 2) {
			int energyStored = container.getEnergyStored();
			int maxEnergyStored = container.getMaxEnergyStored();
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(Component.translatable(TranslationHelper.INSTANCE.translUpgradeKey("battery.contents_tooltip"), String.format("%,d", energyStored), String.format("%,d", maxEnergyStored)));
			guiGraphics.renderTooltip(screen.font, tooltip, Optional.empty(), mouseX, mouseY);
		}
	}

	private void renderCharge(GuiGraphics guiGraphics) {
		int energyStored = container.getEnergyStored();

		int maxEnergyStored = container.getMaxEnergyStored();

		int segmentHeight = CHARGE_SEGMENT.getHeight();
		int numberOfSegments = height / segmentHeight;
		int displayLevel = (int) (numberOfSegments * ((float) energyStored / maxEnergyStored));

		int finalRed = TOP_BAR_COLOR >> 16 & 255;
		int finalGreen = TOP_BAR_COLOR >> 8 & 255;
		int finalBlue = TOP_BAR_COLOR & 255;

		int initialRed = BOTTOM_BAR_COLOR >> 16 & 255;
		int initialGreen = BOTTOM_BAR_COLOR >> 8 & 255;
		int initialBlue = BOTTOM_BAR_COLOR & 255;

		Matrix4f matrix = guiGraphics.pose().last().pose();

		for (int i = 0; i < displayLevel; i++) {
			float percentage = (float) i / (numberOfSegments - 1);
			int red = (int) (initialRed * (1 - percentage) + finalRed * percentage);
			int green = (int) (initialGreen * (1 - percentage) + finalGreen * percentage);
			int blue = (int) (initialBlue * (1 - percentage) + finalBlue * percentage);
			int color = red << 16 | green << 8 | blue | 255 << 24;

			GuiHelper.coloredBlit(matrix, getTankLeft() + 1, pos.y() + height - (i + 1) * segmentHeight, CHARGE_SEGMENT, color);
		}
	}
}
