package net.p3pp3rf1y.sophisticatedcore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public interface IClientHighlightHandler<T> {
	ResourceLocation getPayloadHandlerId();

	T buildClientRequestData(LocalPlayer player, ItemStack stack);

	void clearCache();

	void render(PoseStack poseStack, float partialTick, Vec3 cameraPos);
}
