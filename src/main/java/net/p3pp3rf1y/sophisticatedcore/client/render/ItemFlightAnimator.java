package net.p3pp3rf1y.sophisticatedcore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.util.Easing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ItemFlightAnimator {
	private static final List<Flight> flights = new ArrayList<>();

	private static final float START_PROGRESS = 0.12f;
	private static final double LAUNCH_PUSH = 0.25;

	public static void startFlightFromPayload(FlightPayload payload, RandomSource rng) {
		flights.add(new Flight(payload, rng));
	}

	public static void startFlight(ItemStack stack, Vec3 from, Vec3 to, long startGameTime, int durationTicks, RandomSource rng) {
		startFlightFromPayload(new FlightPayload(stack.copy(), from, to, startGameTime, durationTicks), rng);
	}

	public static void render(PoseStack poseStack, float partialTick, Vec3 cameraPos) {
		Minecraft mc = Minecraft.getInstance();
		if (flights.isEmpty() || mc.level == null) return;

		long gameTime = mc.level.getGameTime();

		Iterator<Flight> it = flights.iterator();
		while (it.hasNext()) {
			Flight flight = it.next();
			double timeRaw = (gameTime + partialTick - flight.startTime) / (double) flight.durationTicks;
			float time = (float) Mth.clamp(timeRaw, 0d, 1d);

			float progress = START_PROGRESS + (1f - START_PROGRESS) * Easing.EASE_IN_OUT_CUBIC.ease(time);

			Vec3 pos = lerp(flight.from, flight.to, progress);

			double spinP = Easing.EASE_OUT_CUBIC.ease(progress);
			float spinDeg = (float) (flight.spinTurns * 360.0 * spinP);
			float scale = (float) (0.9 + 0.1 * progress);

			poseStack.pushPose();
			poseStack.translate(pos.x - cameraPos.x(), pos.y - cameraPos.y(), pos.z - cameraPos.z());
			poseStack.mulPose(Axis.YP.rotationDegrees(flight.yawDeg));
			poseStack.mulPose(Axis.XP.rotationDegrees(spinDeg));
			poseStack.translate(0, -0.15, 0);
			poseStack.scale(scale, scale, scale);

			MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
			mc.getItemRenderer().renderStatic(
					flight.stack,
					ItemDisplayContext.GROUND,
					LevelRenderer.getLightColor(mc.level, new BlockPos((int) pos.x, (int) pos.y, (int) pos.z)),
					OverlayTexture.NO_OVERLAY,
					poseStack,
					buffer,
					mc.level,
					0
			);

			poseStack.popPose();

			if (time >= 1.0)  {
				it.remove();
			}
		}
	}

	public record FlightPayload(ItemStack stack, Vec3 from, Vec3 to, long startGameTime, int durationTicks) {}

	private static final class Flight {
		final ItemStack stack;
		final Vec3 from;
		final Vec3 to;
		final long startTime;
		final int durationTicks;
		final double spinTurns;
		final float yawDeg;

		Flight(FlightPayload p, RandomSource rng) {
			this.stack = p.stack();

			Vec3 rawFrom = p.from();
			Vec3 rawTo = p.to();
			Vec3 dir = rawTo.subtract(rawFrom);
			double dist = Math.max(1e-4, dir.length());
			dir = dir.scale(1.0 / dist);

			this.from = rawFrom.add(dir.scale(LAUNCH_PUSH));
			this.to = rawTo;

			this.startTime = p.startGameTime();
			this.durationTicks = Math.max(1, p.durationTicks());

			this.spinTurns = 2.25 + rng.nextDouble() * 0.75;

			Vec3 fwd = rawTo.subtract(rawFrom);
			double yawRad = Math.atan2(fwd.x, fwd.z);
			this.yawDeg = (float) (yawRad * 180.0 / Math.PI);
		}
	}

	private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
		return a.add(b.subtract(a).scale(t));
	}
}

