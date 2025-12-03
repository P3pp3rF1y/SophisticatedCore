package net.p3pp3rf1y.sophisticatedcore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;

public interface IClientHighlightHandler<T> extends IItemActionPayloadBuilder<T> {
	void clearCache();
	void submit(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, float partialTick, Vec3 cameraPos);
}
