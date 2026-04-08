package net.p3pp3rf1y.sophisticatedcore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.util.Easing;
import net.p3pp3rf1y.sophisticatedcore.util.VoxelOutliner;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BlockHighlightRenderer {
	public static final int HIGHLIGHT_DURATION = 40;

	private static Map<Integer, List<List<BlockPos>>> highlightedPositions = new HashMap<>();
	private static long highlightExpireTime = 0;
	@Nullable
	private static Map<Integer, List<HighlightedGroup>> cachedHighlightedBlocks = new HashMap<>();

	private record HighlightedGroup(List<VoxelOutliner.Edge> edges, Vec3 pivot) {
	}

	public static void addHighlightedPositions(Map<Integer, List<List<BlockPos>>> highlightPositions) {
		highlightPositions.forEach((color, positions) ->
				highlightedPositions.computeIfAbsent(color, k -> new ArrayList<>()).addAll(positions)
		);
		highlightExpireTime = Minecraft.getInstance().level.getGameTime() + HIGHLIGHT_DURATION;
		cachedHighlightedBlocks = null;
	}

	public static void render(PoseStack poseStack, float partialTick, Vec3 cameraPos) {
		Minecraft mc = Minecraft.getInstance();
		if (highlightExpireTime < mc.level.getGameTime()) {
			if (!highlightedPositions.isEmpty()) {
				highlightedPositions.clear();
				cachedHighlightedBlocks = null;
			}
			return;
		}
		MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

		if (cachedHighlightedBlocks == null) {
			cachedHighlightedBlocks = new HashMap<>();
			highlightedPositions.forEach((color, positionGroups) -> {
				cachedHighlightedBlocks.put(color, positionGroups.stream().map(positions -> getHighlightedGroup(mc, positions)).filter(Objects::nonNull).toList());
			});
		}

		SubmitNodeStorage submitNodeStorage = mc.gameRenderer.getSubmitNodeStorage();
		cachedHighlightedBlocks.forEach((color, highlightedBlocks) -> {
			highlightedBlocks.forEach(bh -> submitHighlightedBlock(submitNodeStorage, poseStack, partialTick, cameraPos, bh, mc, buffer, color));
		});
	}

	private static void submitHighlightedBlock(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, float partialTick, Vec3 cameraPos, HighlightedGroup bh, Minecraft mc, MultiBufferSource.BufferSource buffer, int color) {
		poseStack.pushPose();
		poseStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());
		poseStack.translate(bh.pivot.x, bh.pivot.y, bh.pivot.z);
		float scale = 1 + Easing.EASE_IN_OUT_CUBIC.ease((float) tri01(mc.level.getGameTime(), 15, partialTick)) * 0.05f;
		poseStack.scale(scale, scale, scale);
		poseStack.translate(-bh.pivot.x, -bh.pivot.y, -bh.pivot.z);
		BlockHighlightRenderHelper.submitThickEdges(submitNodeCollector, poseStack, color, bh.edges(), 0, 0, 0);
		poseStack.popPose();
	}

	private static HighlightedGroup getHighlightedGroup(Minecraft mc, List<BlockPos> positions) {
		ClientLevel level = mc.level;
		VoxelShape shape = Shapes.empty();
		for (BlockPos pos : positions) {
			if (!level.isLoaded(pos) || level.isEmptyBlock(pos)) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			shape = Shapes.join(shape, state.getShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ()), BooleanOp.OR);
		}
		if (shape.isEmpty()) {
			return null;
		}
		return new HighlightedGroup(VoxelOutliner.linesFromVoxelShapeSimplified(shape, BlockPos.ZERO), shape.bounds().getCenter());
	}

	public static double tri01(double ticks, double periodTicks, double phaseOffsetTicks) {
		if (periodTicks <= 0.0) return 0.0;
		double phase = ((ticks + phaseOffsetTicks) % periodTicks) / (periodTicks - 1);
		return 1.0 - Math.abs(2.0 * phase - 1.0);
	}
}
