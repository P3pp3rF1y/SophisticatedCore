package net.p3pp3rf1y.sophisticatedcore.client.gui.controls;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class ColorPicker extends CompositeWidgetBase<WidgetBase> {
	public static final int COLOR_GRADIENT_WIDTH = 50;
	public static final int RAINBOW_SLIDER_WIDTH = 10;
	public static final int COLOR_ENTRY_WIDTH = COLOR_GRADIENT_WIDTH + 2 + RAINBOW_SLIDER_WIDTH;
	public static final int COLOR_GRADIENT_HEIGHT = 50;
	public static final Dimension DIMENSIONS = new Dimension(114, 84);

	private final TextBox textColorEntry;
	private final ColorPreview colorPreview;
	private final ColorGradientArea colorGradientArea;
	private final RainbowSlider rainbowSlider;
	private final Button confirmButton;
	private final Button cancelButton;
	private final Button transparentColorButton;
	private final List<ColorButton> defaultColorButtons = new ArrayList<>();

	public ColorPicker(Screen screen, Position position, int color, IntConsumer colorSetter) {
		super(position, DIMENSIONS);

		colorPreview = new ColorPreview(new Position(0, 0), new Dimension(50, 12), color);
		addChild(colorPreview);
		textColorEntry = new TextBox(new Position(0, 0), new Dimension(COLOR_ENTRY_WIDTH, 12)) {
			@Override
			public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
				if (isEditable()) {
					setFocused(true);
					screen.setFocused(textColorEntry);
				}
				return super.mouseClicked(event, doubleClicked);
			}
		};
		addChild(textColorEntry);
		colorGradientArea = new ColorGradientArea(new Position(0, 0), new Dimension(COLOR_GRADIENT_WIDTH, COLOR_GRADIENT_HEIGHT), gradientColor -> {
			textColorEntry.setValueWithoutNotification(ColorHelper.getHexColor(gradientColor));
			colorPreview.setColor(gradientColor);
		});
		colorGradientArea.setColor(color);
		rainbowSlider = new RainbowSlider(new Position(0, 0), new Dimension(RAINBOW_SLIDER_WIDTH, 50), colorGradientArea::setHue);
		rainbowSlider.setColor(color);
		addChild(rainbowSlider);
		addChild(colorGradientArea);

		textColorEntry.setValue(ColorHelper.getHexColor(color));
		textColorEntry.setResponder(s -> {
			if (s.length() != 7) {
				return;
			}

			try {
				int c = ARGB.opaque(Integer.parseInt(s.substring(1), 16));
				colorPreview.setColor(c);
				colorGradientArea.setColor(c);
				rainbowSlider.setColor(c);
			} catch (NumberFormatException e) {
				// noop
			}
		});
		addChild(textColorEntry);

		confirmButton = new Button(new Position(0, 0), ButtonDefinitions.CONFIRM, button -> {
			colorSetter.accept(colorPreview.getColor());
		});
		addChild(confirmButton);

		transparentColorButton = new Button(new Position(0, 0), ButtonDefinitions.TRANSPARENT, button -> {
			colorSetter.accept(-1);
		});
		addChild(transparentColorButton);

		cancelButton = new Button(new Position(0, 0), ButtonDefinitions.CANCEL, button -> {
			colorSetter.accept(color); // just send the old color back
		});
		addChild(cancelButton);

		addDefaultColorButtons();

		setPosition(position); // calling set here so that all the positions can be kept in its code
	}

	private void addDefaultColorButtons() {
		for (DyeColor value : DyeColor.values()) {
			ColorButton colorButton = new ColorButton(new Position(0, 0), new Dimension(11, 11), value::getTextureDiffuseColor, button -> {
				int color = value.getTextureDiffuseColor();
				colorGradientArea.setColor(color);
				rainbowSlider.setColor(color);
				colorPreview.setColor(color);
				textColorEntry.setValueWithoutNotification(ColorHelper.getHexColor(color));
			}, null);
			defaultColorButtons.add(colorButton);
			addChild(colorButton);
		}
	}

	@Override
	public void extractBg(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		GuiHelper.renderControlBackground(guiGraphics, x - 5, y - 5, getWidth() + 5 + 5, getHeight() + 5 + 5, 128, 0, 128, 256);
	}

	@Override
	public void setPosition(Position position) {
		super.setPosition(position);
		textColorEntry.setPosition(new Position(x, y + colorGradientArea.getHeight() + 2));
		colorPreview.setPosition(new Position(x + textColorEntry.getWidth() + 2, y + colorGradientArea.getHeight() + 2));
		colorGradientArea.setPosition(new Position(x, y));
		rainbowSlider.setPosition(new Position(x + colorGradientArea.getWidth() + 2, y));
		confirmButton.setPosition(new Position(x + getWidth() / 2 - 32, y + colorGradientArea.getHeight() + 2 + textColorEntry.getHeight() + 2));
		transparentColorButton.setPosition(new Position(x + getWidth() / 2 - 8, confirmButton.getY()));
		cancelButton.setPosition(new Position(x + getWidth() / 2 + 16, confirmButton.getY()));

		int row = 0;
		int column = 0;
		for (ColorButton defaultColorButton : defaultColorButtons) {
			defaultColorButton.setPosition(new Position(rainbowSlider.getX() + rainbowSlider.getWidth() + 2 + column * 13, y + row * 13));
			column++;
			if (column > 3) {
				column = 0;
				row++;
			}
		}
	}

	private static class ColorGradientArea extends WidgetBase {
		private int color;
		private float hue;
		private final IntConsumer colorSetter;

		protected ColorGradientArea(Position position, Dimension dimension, IntConsumer colorSetter) {
			super(position, dimension);
			this.colorSetter = colorSetter;
		}

		public void setColor(int color) {
			this.color = color;
			this.hue = Color.RGBtoHSB(ARGB.red(color), ARGB.green(color), ARGB.blue(color), null)[0];
		}

		public void setHue(float hue) {
			this.hue = hue;
			float[] hsv = Color.RGBtoHSB(ARGB.red(color), ARGB.green(color), ARGB.blue(color), null);
			float saturation = hsv[1];
			float value = hsv[2];
			color = ARGB.opaque(Mth.hsvToRgb(hue, saturation, value));
			colorSetter.accept(color);
		}

		@Override
		protected void extractBg(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
			int topRightCornerColor = Mth.hsvToRgb(hue, 1, 1);
			int red = ARGB.red(topRightCornerColor);
			int green = ARGB.green(topRightCornerColor);
			int blue = ARGB.blue(topRightCornerColor);
			for (int i = 0; i < getWidth(); i++) {
				for (int j = 0; j < getHeight(); j++) {

					float horizontalFactor = (float) i / getWidth();
					float verticalFactor = (float) j / getHeight();
					int color = ARGB.opaque(ARGB.color((int) ((1 - verticalFactor) * ((1 - horizontalFactor) * 255 + red * horizontalFactor)),
							(int) ((1 - verticalFactor) * ((1 - horizontalFactor) * 255 + green * horizontalFactor)),
							(int) ((1 - verticalFactor) * ((1 - horizontalFactor) * 255 + blue * horizontalFactor))));

					guiGraphics.fill(x + i, y + j, x + i + 1, y + j + 1, color);
				}
			}
		}

		@Override
		protected void extractWidget(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
			float[] hsv = Color.RGBtoHSB(ARGB.red(color), ARGB.green(color), ARGB.blue(color), null);
			int x = Math.min((int) (hsv[1] * getWidth()), getWidth() - 1);
			int y = Math.min((int) ((1 - hsv[2]) * getHeight()), getHeight() - 1);

			Matrix3x2fStack pose = guiGraphics.pose();
			pose.pushMatrix();
			pose.translate(0, 0.8f);
			GuiHelper.fill(guiGraphics, this.x, this.y + Math.max(y - 1, 0), this.x + getWidth(), this.y + Math.min(y, getHeight()), 0xFF_FFFFFF);
			pose.popMatrix();
			pose.pushMatrix();
			pose.translate(0, -0.8f);
			GuiHelper.fill(guiGraphics, this.x, this.y + Math.max(y + 1, 0), this.x + getWidth(), this.y + Math.min(y + 2, getHeight()), 0xFF_FFFFFF);
			pose.popMatrix();
			pose.pushMatrix();
			pose.translate(0.8f, 0);
			GuiHelper.fill(guiGraphics, this.x + Math.max(x - 1, 0), this.y, this.x + Math.min(x, getWidth()), this.y + getHeight(), 0xFF_FFFFFF);
			pose.popMatrix();
			pose.pushMatrix();
			pose.translate(-0.8f, 0);
			GuiHelper.fill(guiGraphics, this.x + Math.max(x + 1, 0), this.y, this.x + Math.min(x + 2, getWidth()), this.y + getHeight(), 0xFF_FFFFFF);
			pose.popMatrix();
			GuiHelper.fill(guiGraphics, this.x, this.y + y, this.x + getWidth(), this.y + y + 1, 0xFF_000000);
			GuiHelper.fill(guiGraphics, this.x + x, this.y, this.x + x + 1, this.y + getHeight(), 0xFF_000000);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
			setColorBasedOnMouseCoords(event.x(), event.y());
			return true;
		}

		private void setColorBasedOnMouseCoords(double mouseX, double mouseY) {
			double xClicked = mouseX - x;
			double yClicked = mouseY - y;
			float saturation = (float) xClicked / getWidth();
			float value = 1 - (float) yClicked / getHeight();
			color = ARGB.opaque(Mth.hsvToRgb(hue, saturation, value));
			colorSetter.accept(color);
		}

		@Override
		public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
			setColorBasedOnMouseCoords(event.x(), event.y());
			return true;
		}
	}

	private static class RainbowSlider extends WidgetBase {
		private final FloatConsumer hueSetter;
		private float hue;

		protected RainbowSlider(Position position, Dimension dimension, FloatConsumer hueSetter) {
			super(position, dimension);
			this.hueSetter = hueSetter;
		}

		public void setColor(int color) {
			float[] hsl = Color.RGBtoHSB(ARGB.red(color), ARGB.green(color), ARGB.blue(color), null);
			this.hue = hsl[0];
		}

		@Override
		protected void extractBg(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
			for (int i = 0; i < getHeight(); i++) {
				float renderedHue = (float) i / getHeight();
				int color = ARGB.opaque(Mth.hsvToRgb(renderedHue, 1, 1));
				guiGraphics.fill(x, y + getHeight() - i, x + getWidth(), y + getHeight() - i - 1, color);
			}
		}

		@Override
		protected void extractWidget(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
			int hueMarker = (int) (hue * getHeight());
			Matrix3x2fStack pose = guiGraphics.pose();
			pose.pushMatrix();
			pose.translate(0, 0.8f);
			GuiHelper.fill(guiGraphics, x, y + getHeight() - hueMarker - 1, x + getWidth(), y + getHeight() - hueMarker - 2, 0xFF_FFFFFF);
			pose.popMatrix();
			pose.pushMatrix();
			pose.translate(0, -0.8f);
			GuiHelper.fill(guiGraphics, x, y + getHeight() - hueMarker, x + getWidth(), y + getHeight() - hueMarker + 1, 0xFF_FFFFFF);
			pose.popMatrix();
			GuiHelper.fill(guiGraphics, x, y + getHeight() - hueMarker, x + getWidth(), y + getHeight() - hueMarker - 1, 0xFF_000000);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
			setHueBasedOnMouseY(event.y());
			return true;
		}

		private void setHueBasedOnMouseY(double mouseY) {
			double yClicked = mouseY - y;
			this.hue = 1 - (float) yClicked / getHeight();
			hueSetter.accept(hue);
		}

		@Override
		public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
			setHueBasedOnMouseY(event.y());
			return true;
		}
	}

	private static class ColorPreview extends WidgetBase {
		private int color;

		protected ColorPreview(Position position, Dimension dimension, int color) {
			super(position, dimension);
			this.color = color;
		}

		public void setColor(int color) {
			this.color = color;
		}

		@Override
		protected void extractBg(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
			// noop
		}

		@Override
		protected void extractWidget(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
			guiGraphics.fill(x, y, x + getWidth(), y + getHeight(), color);
		}

		public int getColor() {
			return color;
		}
	}
}
