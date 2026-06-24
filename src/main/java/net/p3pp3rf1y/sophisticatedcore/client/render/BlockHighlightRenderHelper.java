package net.p3pp3rf1y.sophisticatedcore.client.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.util.VoxelOutliner;

import java.util.List;

public class BlockHighlightRenderHelper {

	public static final RenderPipeline THICK_HIGHLIGHT_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
			.withVertexShader("core/position_color").withFragmentShader("core/position_color")
			.withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS).withCull(false)
			.withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
			.withLocation(SophisticatedCore.getIdentifier("pipeline/outline_quads")).build();

	public static final RenderType THICK_HIGHLIGHT_QUADS = RenderType.create("storage_outline_quads", RenderSetup.builder(THICK_HIGHLIGHT_PIPELINE)
			.bufferSize(1536).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).setOutputTarget(OutputTarget.MAIN_TARGET).createRenderSetup());

	public static void submitThickEdges(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, int color, List<VoxelOutliner.Edge> edges,
			BlockPos originPos) {
		submitThickEdges(submitNodeCollector, poseStack, color, edges, originPos.getX(), originPos.getY(), originPos.getZ());
	}

	public static void submitThickEdges(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, int color, List<VoxelOutliner.Edge> edges, double originX,
			double originY, double originZ) {
		if (edges.isEmpty()) {
			return;
		}

		submitNodeCollector.submitCustomGeometry(poseStack, THICK_HIGHLIGHT_QUADS, (pose, vertexConsumer) -> {
			int red = color >> 16 & 255;
			int green = color >> 8 & 255;
			int blue = color & 255;
			edges.forEach(edge -> {
				emitThickLineOrtho(vertexConsumer, pose, edge.a(), edge.b(), 1 / 32f, red, green, blue, 255, originX, originY, originZ);
			});
		});
	}

	public static void emitThickLineOrtho(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, float thickness, int r, int g, int bl, int alpha,
			double originX, double originY, double originZ) {
		final float rh = thickness * 0.5f;

		Vec3 d = b.subtract(a);
		Vec3 u = new Vec3(Math.signum(d.x), Math.signum(d.y), Math.signum(d.z));

		Vec3 aEx = a.subtract(u.scale(rh));
		Vec3 bEx = b.add(u.scale(rh));

		Vec3 v, w;
		if (u.x != 0) {
			v = new Vec3(0, 1, 0);
			w = new Vec3(0, 0, 1);
		} else if (u.y != 0) {
			v = new Vec3(1, 0, 0);
			w = new Vec3(0, 0, 1);
		} else {
			v = new Vec3(1, 0, 0);
			w = new Vec3(0, 1, 0);
		}

		Vec3 vOff = v.scale(rh), wOff = w.scale(rh);

		// 8 prism corners around extended endpoints
		Vec3 aVpWm = aEx.add(vOff).subtract(wOff);
		Vec3 aVpWp = aEx.add(vOff).add(wOff);
		Vec3 aVmWp = aEx.subtract(vOff).add(wOff);
		Vec3 aVmWm = aEx.subtract(vOff).subtract(wOff);

		Vec3 bVpWm = bEx.add(vOff).subtract(wOff);
		Vec3 bVpWp = bEx.add(vOff).add(wOff);
		Vec3 bVmWp = bEx.subtract(vOff).add(wOff);
		Vec3 bVmWm = bEx.subtract(vOff).subtract(wOff);

		// 4 side faces (POSITION_COLOR, no normals needed)
		emitQuad(vc, pose, aVpWm, aVpWp, bVpWp, bVpWm, r, g, bl, alpha, originX, originY, originZ);
		emitQuad(vc, pose, aVmWp, aVmWm, bVmWm, bVmWp, r, g, bl, alpha, originX, originY, originZ);
		emitQuad(vc, pose, aVmWp, aVpWp, bVpWp, bVmWp, r, g, bl, alpha, originX, originY, originZ);
		emitQuad(vc, pose, aVpWm, aVmWm, bVmWm, bVpWm, r, g, bl, alpha, originX, originY, originZ);
	}

	private static void emitQuad(VertexConsumer vc, PoseStack.Pose pose, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int r, int g, int b, int a, double originX,
			double originY, double originZ) {
		add(vc, pose, p0, r, g, b, a, originX, originY, originZ);
		add(vc, pose, p1, r, g, b, a, originX, originY, originZ);
		add(vc, pose, p2, r, g, b, a, originX, originY, originZ);
		add(vc, pose, p3, r, g, b, a, originX, originY, originZ);
	}

	private static void add(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, int r, int g, int b, int a, double originX, double originY, double originZ) {
		vc.addVertex(pose, (float) (p.x - originX), (float) (p.y - originY), (float) (p.z - originZ)).setColor(r, g, b, a);
	}
}
