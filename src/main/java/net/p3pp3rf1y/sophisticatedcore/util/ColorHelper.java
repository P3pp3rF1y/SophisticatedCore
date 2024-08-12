package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.world.item.DyeColor;

import java.util.List;

public class ColorHelper {
	private ColorHelper() {}

	public static int calculateColor(int baseColor, int defaultColor, List<DyeColor> dyes) {
		if (dyes.isEmpty()) {
			return baseColor;
		}

		int[] rgb = new int[3];
		int sumMaxComponent = 0;
		int numberOfColors = 0;
		if (baseColor != defaultColor) {
			float baseRed = (baseColor >> 16 & 255);
			float baseGreen = (baseColor >> 8 & 255);
			float baseBlue = (baseColor & 255);
			sumMaxComponent = (int) (sumMaxComponent + Math.max(baseRed, Math.max(baseGreen, baseBlue)));
			rgb[0] = (int) (rgb[0] + baseRed);
			rgb[1] = (int) (rgb[1] + baseGreen);
			rgb[2] = (int) (rgb[2] + baseBlue);
			++numberOfColors;
		}

		for (DyeColor dye : dyes) {
			int dyeRgb = dye.getTextureDiffuseColor();
			int dyeRed = (dyeRgb >> 16) & 255;
			int dyeGreen = (dyeRgb >> 8) & 255;
			int dyeBlue = dyeRgb & 255;
			sumMaxComponent += Math.max(dyeRed, Math.max(dyeGreen, dyeBlue));
			rgb[0] += dyeRed;
			rgb[1] += dyeGreen;
			rgb[2] += dyeBlue;
			++numberOfColors;
		}

		int avgRed = rgb[0] / numberOfColors;
		int avgGreen = rgb[1] / numberOfColors;
		int avgBlue = rgb[2] / numberOfColors;
		float avgMaxComponent = (float) sumMaxComponent / (float) numberOfColors;
		float maxAvgComponent = Math.max(avgRed, Math.max(avgGreen, avgBlue));
		avgRed = (int) (avgRed * avgMaxComponent / maxAvgComponent);
		avgGreen = (int) (avgGreen * avgMaxComponent / maxAvgComponent);
		avgBlue = (int) (avgBlue * avgMaxComponent / maxAvgComponent);
		int finalColor = (avgRed << 8) + avgGreen;
		finalColor = (finalColor << 8) + avgBlue;

		return 0xFF_000000 | finalColor;
	}
}
