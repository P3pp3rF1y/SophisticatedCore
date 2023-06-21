package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public class StorageGuiHelper {

	private StorageGuiHelper() {}

	public static void renderStorageBackground(Position position, GuiGraphics guiGraphics, ResourceLocation textureName, int xSize, int slotsHeight) {
		int x = position.x();
		int y = position.y();
		int halfSlotHeight = slotsHeight / 2;
		guiGraphics.blit(textureName, x, y, 0, 0, xSize, StorageScreenBase.SLOTS_Y_OFFSET + halfSlotHeight, 256, 256);
		int playerInventoryHeight = 97;
		guiGraphics.blit(textureName, x, y + StorageScreenBase.SLOTS_Y_OFFSET + halfSlotHeight, 0, (float) 256 - (playerInventoryHeight + halfSlotHeight), xSize, playerInventoryHeight + halfSlotHeight, 256, 256);
	}
}
