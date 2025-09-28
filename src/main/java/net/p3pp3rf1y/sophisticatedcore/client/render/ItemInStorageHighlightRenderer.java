package net.p3pp3rf1y.sophisticatedcore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllerBoundable;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.network.RequestItemHighlightsMessage;
import net.p3pp3rf1y.sophisticatedcore.util.Easing;
import net.p3pp3rf1y.sophisticatedcore.util.IDoubleBlock;
import net.p3pp3rf1y.sophisticatedcore.util.VoxelOutliner;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import javax.annotation.Nullable;
import java.util.*;

public class ItemInStorageHighlightRenderer {
	public static final int HIGHLIGHT_DURATION = 40;
	public static final int HIGHLIGHT_RANGE = 32;
	public static final int MATCHING_STACK_HIGHLIGHT_COLOR = 0x4CAF50;
	public static final int MATCHING_ITEM_HIGHLIGHT_COLOR = 0x42A5F5;

	private static List<BlockPos> highlightedStackPositions = Collections.emptyList();
	private static List<BlockPos> highlightedItemPositions = Collections.emptyList();
	private static List<BlockPos> highlightedEmptyTargetPositions = Collections.emptyList();
	private static long highlightExpireTime = 0;
	@Nullable
	private static List<HighlightedBlock> cachedMatchingStackHighlights = null;
	@Nullable
	private static List<HighlightedBlock> cachedMatchingItemHighlights = null;
	@Nullable
	private static List<HighlightedBlock> cachedEmptyTargetHighlights = null;

	private static List<IClientHighlightHandler<?>> highlightHandlers = new ArrayList<>();

	public static void registerHighlightHandler(IClientHighlightHandler<?> highlightHandler) {
		highlightHandlers.add(highlightHandler);
	}

	public static void highlightItem(LocalPlayer player, ItemStack stack) {
		List<BlockPos> positions = WorldHelper.getBlockEntitiesInRange(player.level(), player.blockPosition(), 32, IControllableStorage.class).stream().map(IControllerBoundable::getStorageBlockPos).toList();
		Map<ResourceLocation, Object> extras = new LinkedHashMap<>();
		highlightHandlers.forEach(h -> {
			extras.put(h.getPayloadHandlerId(), h.buildClientRequestData(player, stack));
		});
		if (!positions.isEmpty() || !extras.isEmpty()) {
			PacketHandler.INSTANCE.sendToServer(new RequestItemHighlightsMessage(stack, positions, extras));
		}
	}

	private record HighlightedBlock(BlockPos pos, List<VoxelOutliner.Edge> edges, Vec3 pivot) {
	}

	public static void setHighlightedPositions(List<BlockPos> stackPositions, List<BlockPos> itemPositions, List<BlockPos> emptyTargetPositions) {
		highlightedStackPositions = stackPositions;
		highlightedItemPositions = itemPositions;
		highlightedEmptyTargetPositions = emptyTargetPositions;
		highlightExpireTime = Minecraft.getInstance().level.getGameTime() + HIGHLIGHT_DURATION;
		cachedMatchingStackHighlights = null;
		cachedMatchingItemHighlights = null;
		cachedEmptyTargetHighlights = null;
	}

	public static void render(PoseStack poseStack, float partialTick, Vec3 cameraPos) {
		Minecraft mc = Minecraft.getInstance();
		if (highlightExpireTime < mc.level.getGameTime()) {
			if (!highlightedStackPositions.isEmpty() || !highlightedItemPositions.isEmpty() || !highlightedEmptyTargetPositions.isEmpty()) {
				highlightedStackPositions = Collections.emptyList();
				highlightedItemPositions = Collections.emptyList();
				highlightedEmptyTargetPositions = Collections.emptyList();
				cachedMatchingStackHighlights = null;
				cachedMatchingItemHighlights = null;
				cachedEmptyTargetHighlights = null;
				highlightHandlers.forEach(IClientHighlightHandler::clearCache);
			}

			return;
		}
		MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

		if (cachedMatchingStackHighlights == null) {
			cachedMatchingStackHighlights = highlightedStackPositions.stream().map(pos -> getHighlightedBlock(mc, pos)).toList();
		}
		if (cachedMatchingItemHighlights == null) {
			cachedMatchingItemHighlights = highlightedItemPositions.stream().map(pos -> getHighlightedBlock(mc, pos)).toList();
		}
		if (cachedEmptyTargetHighlights == null) {
			cachedEmptyTargetHighlights = highlightedEmptyTargetPositions.stream().map(pos -> getHighlightedBlock(mc, pos)).toList();
		}

		cachedMatchingStackHighlights.forEach(bh -> renderHighlightedBlock(poseStack, partialTick, cameraPos, bh, mc, buffer, MATCHING_STACK_HIGHLIGHT_COLOR));
		cachedMatchingItemHighlights.forEach(bh -> renderHighlightedBlock(poseStack, partialTick, cameraPos, bh, mc, buffer, MATCHING_ITEM_HIGHLIGHT_COLOR));
		cachedEmptyTargetHighlights.forEach(bh -> renderHighlightedBlock(poseStack, partialTick, cameraPos, bh, mc, buffer, 0xFFEB3B));

		highlightHandlers.forEach(callback -> callback.render(poseStack, partialTick, cameraPos));
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
		BlockState state = level.getBlockState(pos);
		VoxelShape shape = state.getShape(level, pos);
		if (state.getBlock() instanceof IDoubleBlock doubleBlock) {
			VoxelShape finalShape = shape;
			shape = doubleBlock.getOtherPosition(state, pos).map(otherPos -> {
				BlockState otherState = level.getBlockState(otherPos);
				VoxelShape otherShape = otherState.getShape(level, otherPos);
				otherShape = otherShape.move(otherPos.getX() - pos.getX(), otherPos.getY() - pos.getY(), otherPos.getZ() - pos.getZ());
				return Shapes.join(finalShape, otherShape, BooleanOp.OR);
			}).orElse(shape);
		}
		return new HighlightedBlock(pos, VoxelOutliner.linesFromVoxelShapeSimplified(shape, pos), shape.bounds().getCenter());
	}

	public static double tri01(double ticks, double periodTicks, double phaseOffsetTicks) {
		if (periodTicks <= 0.0) return 0.0;
		double phase = ((ticks + phaseOffsetTicks) % periodTicks) / (periodTicks - 1);
		return 1.0 - Math.abs(2.0 * phase - 1.0);
	}
}
