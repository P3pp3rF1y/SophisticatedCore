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

public class ItemDisplaySettingsPreviewRenderer extends PictureInPictureRenderer<ItemDisplaySettingsPreviewRenderState> {
	public ItemDisplaySettingsPreviewRenderer(MultiBufferSource.BufferSource bufferSource) {
		super(bufferSource);
	}

	@Override
	public Class<ItemDisplaySettingsPreviewRenderState> getRenderStateClass() {
		return ItemDisplaySettingsPreviewRenderState.class;
	}

	@Override
	protected void renderToTexture(ItemDisplaySettingsPreviewRenderState renderState, PoseStack poseStack) {
		poseStack.mulPose(Axis.XN.rotationDegrees(-renderState.xAxisRotation()));
		poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.yAxisRotation()));
		poseStack.scale(renderState.itemScale(), -renderState.itemScale(), -renderState.itemScale());
		TrackingItemStackRenderState itemStackRenderState = renderState.itemStackRenderState();
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
		FeatureRenderDispatcher featureRenderDispatcher = minecraft.gameRenderer.getFeatureRenderDispatcher();
		SubmitNodeStorage submitNodeStorage = featureRenderDispatcher.getSubmitNodeStorage();
		itemStackRenderState.submit(poseStack, submitNodeStorage, 15728880, OverlayTexture.NO_OVERLAY, 0);
		featureRenderDispatcher.renderAllFeatures();
	}

	@Override
	protected boolean textureIsReadyToBlit(ItemDisplaySettingsPreviewRenderState renderState) {
		return false;
	}

	@Override
	protected float getTranslateY(int height, int guiScale) {
		return height / 2.0F;
	}

	@Override
	protected String getTextureLabel() {
		return "item_display_settings_preview";
	}
}
