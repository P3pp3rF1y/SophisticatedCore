package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ImageButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;

public class StorageSettingsTab extends Tab {
	private static final TextureBlitData ICON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(16, 96), Dimension.SQUARE_16);
	private final StorageScreenBase<?> screen;

	public StorageSettingsTab(Position position, StorageScreenBase<?> screen, String tabTooltip) {
		super(position, Component.translatable(tabTooltip), onTabIconClicked -> new ImageButton(new Position(position.x() + 1, position.y() + 4), Dimension.SQUARE_16, ICON, onTabIconClicked));
		this.screen = screen;
	}

	@Override
	protected void onTabIconClicked(int button) {
		screen.getMenu().openSettings();
	}
}
