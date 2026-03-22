package net.p3pp3rf1y.sophisticatedcore.client.gui.controls;

import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiSoundHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

import java.util.function.IntConsumer;

public abstract class ButtonBase extends WidgetBase {
	protected IntConsumer onClick;

	protected ButtonBase(Position position, Dimension dimension, IntConsumer onClick) {
		super(position, dimension);
		this.onClick = onClick;
	}

	protected void setOnClick(IntConsumer onClick) {
		this.onClick = onClick;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isMouseOver(mouseX, mouseY)) {
			return false;
		}
		onClick.accept(button);
		GuiSoundHelper.playButtonClickSound();
		return true;
	}
}
