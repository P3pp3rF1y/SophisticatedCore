package net.p3pp3rf1y.sophisticatedcore.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.util.VoxelOutliner;

import java.util.List;

public class BlockHighlightRenderHelper {

	public static final RenderType OUTLINE_QUADS = RenderType.create(
			"storage_outline_quads",
			DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.QUADS,
			256,
			false,  // no affect crumbling
			false,  // no sorting needed for opaque
			RenderType.CompositeState.builder()
					.setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
					.setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
					.setCullState(RenderStateShard.NO_CULL)
					.setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
					.setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
					.setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
					.setOutputState(RenderStateShard.MAIN_TARGET)
					.createCompositeState(true));

	public static void renderThickEdges(PoseStack poseStack, MultiBufferSource bufferSource, int color, List<VoxelOutliner.Edge> edges, BlockPos originPos) {
		VertexConsumer vertexConsumer = bufferSource.getBuffer(OUTLINE_QUADS);
		int red = color >> 16 & 255;
		int green = color >> 8 & 255;
		int blue = color & 255;
		PoseStack.Pose pose = poseStack.last();

		edges.forEach(edge -> {
			emitThickLineOrtho(vertexConsumer, pose, originPos, edge.a(), edge.b(), 1 / 32f, red, green, blue, 255);
		});
	}

	public static void emitThickLineOrtho(
			VertexConsumer vc, PoseStack.Pose pose, BlockPos origin,
			Vec3 a, Vec3 b, float thickness,
			int r, int g, int bl, int alpha
	) {
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
		emitQuad(vc, pose, origin, aVpWm, aVpWp, bVpWp, bVpWm, r, g, bl, alpha);
		emitQuad(vc, pose, origin, aVmWp, aVmWm, bVmWm, bVmWp, r, g, bl, alpha);
		emitQuad(vc, pose, origin, aVmWp, aVpWp, bVpWp, bVmWp, r, g, bl, alpha);
		emitQuad(vc, pose, origin, aVpWm, aVmWm, bVmWm, bVpWm, r, g, bl, alpha);
	}

	private static void emitQuad(
			VertexConsumer vc, PoseStack.Pose pose, BlockPos origin,
			Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
			int r, int g, int b, int a
	) {
		add(vc, pose, origin, p0, r, g, b, a);
		add(vc, pose, origin, p1, r, g, b, a);
		add(vc, pose, origin, p2, r, g, b, a);
		add(vc, pose, origin, p3, r, g, b, a);
	}

	private static void add(VertexConsumer vc, PoseStack.Pose pose, BlockPos origin,
							Vec3 p, int r, int g, int b, int a) {
		vc.addVertex(pose,
						(float) (p.x - origin.getX()),
						(float) (p.y - origin.getY()),
						(float) (p.z - origin.getZ()))
				.setColor(r, g, b, a);
	}
}
