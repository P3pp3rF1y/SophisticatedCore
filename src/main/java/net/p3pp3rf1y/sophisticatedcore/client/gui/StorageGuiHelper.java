package net.p3pp3rf1y.sophisticatedcore.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public class StorageGuiHelper {

	private StorageGuiHelper() {}

	public static void renderStorageBackground(Position position, PoseStack matrixStack, ResourceLocation textureName, int xSize, int slotsHeight) {
		int x = position.x();
		int y = position.y();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, textureName);
		int halfSlotHeight = slotsHeight / 2;
		GuiComponent.blit(matrixStack, x, y, 0, 0, xSize, StorageScreenBase.SLOTS_Y_OFFSET + halfSlotHeight, 256, 256);
		int playerInventoryHeight = 97;
		GuiComponent.blit(matrixStack, x, y + StorageScreenBase.SLOTS_Y_OFFSET + halfSlotHeight, 0, (float) 256 - (playerInventoryHeight + halfSlotHeight), xSize, playerInventoryHeight + halfSlotHeight, 256, 256);
	}
}
