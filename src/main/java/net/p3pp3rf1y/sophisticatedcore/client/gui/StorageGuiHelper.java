package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public class StorageGuiHelper {

	private StorageGuiHelper() {}

	public static void renderStorageBackground(Position position, GuiGraphics guiGraphics, ResourceLocation textureName, int xSize, int slotsHeight) {
		int x = position.x();
		int y = position.y();
		int slotsTopBottomHeight = Math.min(slotsHeight / 2, 150);
		int yOffset = 0;

		guiGraphics.blit(textureName, x, y, 0, 0, xSize, StorageScreenBase.SLOTS_Y_OFFSET + slotsTopBottomHeight, 256, 256);

		if (slotsHeight / 2 > 150) {
			int middleHeight = (slotsHeight / 2 - 150) * 2;
			guiGraphics.blit(textureName, x, y + StorageScreenBase.SLOTS_Y_OFFSET + slotsTopBottomHeight, 0, StorageScreenBase.SLOTS_Y_OFFSET, xSize, middleHeight, 256, 256);
			yOffset = middleHeight;
		}

		int playerInventoryHeight = 97;
		guiGraphics.blit(textureName, x, y + yOffset + StorageScreenBase.SLOTS_Y_OFFSET + slotsTopBottomHeight, 0, (float) 256 - (playerInventoryHeight + slotsTopBottomHeight), xSize, playerInventoryHeight + slotsTopBottomHeight, 256, 256);
	}
}
