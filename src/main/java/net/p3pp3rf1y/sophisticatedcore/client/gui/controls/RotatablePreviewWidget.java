package net.p3pp3rf1y.sophisticatedcore.client.gui.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.util.Easing;

public class RotatablePreviewWidget extends CompositeWidgetBase<WidgetBase> {
	private static final float DEFAULT_X_AXIS_ROTATION = 30;
	private static final float DEFAULT_Y_AXIS_ROTATION = 45;
	private float xAxisRotation = DEFAULT_X_AXIS_ROTATION;
	private float yAxisRotation = DEFAULT_Y_AXIS_ROTATION;
	private float fromXAxisRotation = xAxisRotation;
	private float fromYAxisRotation = yAxisRotation;
	private float targetXAxisRotation = xAxisRotation;
	private float targetYAxisRotation = yAxisRotation;
	private long lastTargetSetTime = 0;

	protected RotatablePreviewWidget(Position position, Dimension dimension) {
		super(position, dimension);
	}

	public void resetToDefaultRotation() {
		setTargetRotations(DEFAULT_X_AXIS_ROTATION, DEFAULT_Y_AXIS_ROTATION);
	}

	public void setTargetRotations(float xAxisRotation, float yAxisRotation) {
		if (this.targetXAxisRotation == xAxisRotation && this.targetYAxisRotation == yAxisRotation) {
			return;
		}

		fromXAxisRotation = this.xAxisRotation;
		fromYAxisRotation = this.yAxisRotation;
		targetXAxisRotation = xAxisRotation;
		targetYAxisRotation = yAxisRotation;
		lastTargetSetTime = System.currentTimeMillis();
	}

	@Override
	protected void extractBg(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		guiGraphics.fill(x, y, x + getWidth(), y + getHeight(), 0xFF_000000);
	}

	@Override
	protected void extractWidget(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.extractWidget(guiGraphics, mouseX, mouseY, partialTicks);
		updateRotations();
		renderPreview(guiGraphics, x, y, getWidth(), getHeight(), xAxisRotation, yAxisRotation, partialTicks);
	}

	protected void renderPreview(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, float xAxisRotation, float yAxisRotation,
			float partialTicks) {
		// noop by default
	}

	private void updateRotations() {
		float secondsDuration = 1;
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastTargetSetTime <= secondsDuration * 1000) {
			float ratio = (currentTime - lastTargetSetTime) / (secondsDuration * 1000);
			ratio = Easing.EASE_IN_OUT_CUBIC.ease(ratio);
			xAxisRotation = fromXAxisRotation + (targetXAxisRotation - fromXAxisRotation) * ratio;
			yAxisRotation = fromYAxisRotation + (targetYAxisRotation - fromYAxisRotation) * ratio;
		} else {
			xAxisRotation = targetXAxisRotation;
			yAxisRotation = targetYAxisRotation;
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
		if (super.mouseClicked(event, doubleClicked)) {
			return true;
		}

		if (isMouseOver(event.x(), event.y())) {
			setDragging(event.button() == 0);
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (super.mouseDragged(event, dragX, dragY)) {
			return true;
		}

		yAxisRotation += (float) (2 * dragX);
		yAxisRotation = yAxisRotation % 360;
		xAxisRotation += (float) (2 * dragY);
		xAxisRotation = xAxisRotation % 360;
		targetXAxisRotation = xAxisRotation;
		targetYAxisRotation = yAxisRotation;
		return true;
	}
}
