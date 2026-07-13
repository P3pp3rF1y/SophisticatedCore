package net.p3pp3rf1y.sophisticatedcore.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class ItemDisplayPreviewRenderer extends PictureInPictureRenderer<ItemDisplayPreviewRenderState> {
	public ItemDisplayPreviewRenderer(MultiBufferSource.BufferSource bufferSource) {
		super(bufferSource);
	}

	@Override
	public Class<ItemDisplayPreviewRenderState> getRenderStateClass() {
		return ItemDisplayPreviewRenderState.class;
	}

	@Override
	protected void renderToTexture(ItemDisplayPreviewRenderState renderState, PoseStack poseStack) {
		poseStack.mulPose(Axis.XN.rotationDegrees(-renderState.xAxisRotation()));
		poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.yAxisRotation()));
		float previewScale = renderState.scaleMultiplier();
		poseStack.scale(previewScale, -previewScale, -previewScale);
		TrackingItemStackRenderState itemStackRenderState = renderState.itemStackRenderState();
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
		FeatureRenderDispatcher featureRenderDispatcher = minecraft.gameRenderer.getFeatureRenderDispatcher();
		SubmitNodeStorage submitNodeStorage = featureRenderDispatcher.getSubmitNodeStorage();
		itemStackRenderState.submit(poseStack, submitNodeStorage, 15728880, OverlayTexture.NO_OVERLAY, 0);
		featureRenderDispatcher.renderAllFeatures();
		minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
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
