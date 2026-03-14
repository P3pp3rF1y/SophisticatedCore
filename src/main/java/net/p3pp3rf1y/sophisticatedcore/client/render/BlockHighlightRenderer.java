package net.p3pp3rf1y.sophisticatedcore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.util.Easing;
import net.p3pp3rf1y.sophisticatedcore.util.IDoubleBlock;
import net.p3pp3rf1y.sophisticatedcore.util.VoxelOutliner;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BlockHighlightRenderer {
	public static final int HIGHLIGHT_DURATION = 40;

	private static Map<Integer, List<BlockPos>> highlightedPositions = new HashMap<>();
	private static long highlightExpireTime = 0;
	@Nullable
	private static Map<Integer, List<HighlightedBlock>> cachedHighlightedBlocks = new HashMap<>();

	private record HighlightedBlock(BlockPos pos, List<VoxelOutliner.Edge> edges, Vec3 pivot) {
	}

	public static void addHighlightedPositions(Map<Integer, List<BlockPos>> highlightPositions) {
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
			highlightedPositions.forEach((color, positions) -> {
				cachedHighlightedBlocks.put(color, positions.stream().map(pos -> getHighlightedBlock(mc, pos)).filter(Objects::nonNull).toList());
			});
		}

		cachedHighlightedBlocks.forEach((color, highlightedBlocks) -> {
			highlightedBlocks.forEach(bh -> renderHighlightedBlock(poseStack, partialTick, cameraPos, bh, mc, buffer, color));
		});
	}

	private static void renderHighlightedBlock(PoseStack poseStack, float partialTick, Vec3 cameraPos, HighlightedBlock bh, Minecraft mc, MultiBufferSource.BufferSource buffer, int color) {
		poseStack.pushPose();
		poseStack.translate(bh.pos.getX() - cameraPos.x(), bh.pos.getY() - cameraPos.y(), bh.pos.getZ() - cameraPos.z());
		poseStack.translate(bh.pivot.x, bh.pivot.y, bh.pivot.z);
		float scale = 1 + Easing.EASE_IN_OUT_CUBIC.ease((float) tri01(mc.level.getGameTime(), 15, partialTick)) * 0.05f;
		poseStack.scale(scale, scale, scale);
		poseStack.translate(-bh.pivot.x, -bh.pivot.y, -bh.pivot.z);
		BlockHighlightRenderHelper.renderThickEdges(poseStack, buffer, color, bh.edges(), bh.pos().getX(), bh.pos().getY(), bh.pos().getZ());
		poseStack.popPose();
	}

	private static HighlightedBlock getHighlightedBlock(Minecraft mc, BlockPos pos) {
		ClientLevel level = mc.level;
		if (!level.isLoaded(pos) || level.isEmptyBlock(pos)) {
			return null;
		}
		BlockState state = level.getBlockState(pos);

		VoxelShape shape = state.getShape(level, pos);
		if (state.getBlock() instanceof IDoubleBlock doubleBlock) {
			VoxelShape finalShape = shape;
			shape = doubleBlock.getOtherPosition(state, pos).map(otherPos -> {
				if (!level.isLoaded(otherPos) || level.isEmptyBlock(otherPos)) {
					return finalShape;
				}
				BlockState otherState = level.getBlockState(otherPos);
				VoxelShape otherShape = otherState.getShape(level, otherPos);
				otherShape = otherShape.move(otherPos.getX() - pos.getX(), otherPos.getY() - pos.getY(), otherPos.getZ() - pos.getZ());
				return Shapes.join(finalShape, otherShape, BooleanOp.OR);
			}).orElse(shape);
		} else if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			Direction connectedDir = ChestBlock.getConnectedDirection(state);
			BlockPos otherPos = pos.relative(connectedDir);
			if (level.isLoaded(otherPos) && !level.isEmptyBlock(otherPos)) {
				BlockState otherState = level.getBlockState(otherPos);
				VoxelShape otherShape = otherState.getShape(level, otherPos);
				otherShape = otherShape.move(otherPos.getX() - pos.getX(), otherPos.getY() - pos.getY(), otherPos.getZ() - pos.getZ());
				shape = Shapes.join(shape, otherShape, BooleanOp.OR);
			}
		}
		return new HighlightedBlock(pos, VoxelOutliner.linesFromVoxelShapeSimplified(shape, pos), shape.bounds().getCenter());
	}

	public static double tri01(double ticks, double periodTicks, double phaseOffsetTicks) {
		if (periodTicks <= 0.0) return 0.0;
		double phase = ((ticks + phaseOffsetTicks) % periodTicks) / (periodTicks - 1);
		return 1.0 - Math.abs(2.0 * phase - 1.0);
	}
}
