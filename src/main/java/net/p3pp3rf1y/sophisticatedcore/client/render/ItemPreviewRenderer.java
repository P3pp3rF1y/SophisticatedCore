package net.p3pp3rf1y.sophisticatedcore.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

public class ItemPreviewRenderer extends PictureInPictureRenderer<ItemPreviewRenderState> {
	public ItemPreviewRenderer(MultiBufferSource.BufferSource bufferSource) {
		super(bufferSource);
	}

	@Override
	public Class<ItemPreviewRenderState> getRenderStateClass() {
		return ItemPreviewRenderState.class;
	}

	@Override
	protected void renderToTexture(ItemPreviewRenderState renderState, PoseStack poseStack) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
		poseStack.mulPose(Axis.XN.rotationDegrees(-renderState.xAxisRotation()));
		poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.yAxisRotation()));
		poseStack.scale(renderState.itemScale(), -renderState.itemScale(), -renderState.itemScale());
		minecraft.getItemRenderer().renderStatic(renderState.stack(), ItemDisplayContext.NONE, 15728880, OverlayTexture.NO_OVERLAY, poseStack, bufferSource,
				minecraft.level, 0);
	}

	@Override
	protected boolean textureIsReadyToBlit(ItemPreviewRenderState renderState) {
		return false;
	}

	@Override
	protected float getTranslateY(int height, int guiScale) {
		return height / 2.0F;
	}

	@Override
	protected String getTextureLabel() {
		return "sophisticatedcore_item_preview";
	}
}
