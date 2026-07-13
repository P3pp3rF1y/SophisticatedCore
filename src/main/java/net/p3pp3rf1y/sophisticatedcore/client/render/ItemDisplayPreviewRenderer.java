package net.p3pp3rf1y.sophisticatedcore.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class ItemDisplayPreviewRenderer extends PictureInPictureRenderer<ItemDisplayPreviewRenderState> {
	@Override
	public Class<ItemDisplayPreviewRenderState> getRenderStateClass() {
		return ItemDisplayPreviewRenderState.class;
	}

	@Override
	protected void renderToTexture(ItemDisplayPreviewRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
		poseStack.mulPose(Axis.XN.rotationDegrees(-renderState.xAxisRotation()));
		poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.yAxisRotation()));
		float previewScale = renderState.scaleMultiplier();
		poseStack.scale(previewScale, -previewScale, -previewScale);
		TrackingItemStackRenderState itemStackRenderState = renderState.itemStackRenderState();
		Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
		itemStackRenderState.submit(poseStack, submitNodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
	}

	@Override
	protected boolean textureIsReadyToBlit(ItemDisplayPreviewRenderState renderState) {
		return false;
	}

	@Override
	protected float getTranslateY(int height, int guiScale) {
		return height / 2.0F;
	}

	@Override
	protected String getTextureLabel() {
		return "item_display_preview";
	}
}
