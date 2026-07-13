package net.p3pp3rf1y.sophisticatedcore.client.gui.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		guiGraphics.fill(x, y, x + getWidth(), y + getHeight(), 0xFF_000000);
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
		updateRotations();
		renderPreview(guiGraphics, x, y, getWidth(), getHeight(), xAxisRotation, yAxisRotation, partialTicks);
	}

	protected void renderPreview(GuiGraphics guiGraphics, int x, int y, int width, int height, float xAxisRotation, float yAxisRotation, float partialTicks) {
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
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		if (isMouseOver(mouseX, mouseY)) {
			setDragging(button == 0);
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		yAxisRotation += 2 * dragX;
		yAxisRotation = yAxisRotation % 360;
		xAxisRotation += 2 * dragY;
		xAxisRotation = xAxisRotation % 360;
		targetXAxisRotation = xAxisRotation;
		targetYAxisRotation = yAxisRotation;
		return true;
	}

}
