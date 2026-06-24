package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.util.Mth;

import java.util.function.Function;

public class Easing {
	public static final Easing EASE_IN_CUBIC = new Easing(number -> number * number * number);
	public static final Easing EASE_IN_OUT_CUBIC = new Easing(
			number -> (float) (number < 0.5 ? 4 * number * number * number : (1 - Math.pow(-2 * number + 2, 3) / 2)));
	public static final Easing EASE_OUT_CUBIC = new Easing(number -> (float) (1f - Math.pow(1 - number, 3)));
	public static final Easing EASE_IN_OUT_QUINT = new Easing(
			number -> (float) (number < 0.5 ? 16 * Math.pow(number, 5) : (1 - Math.pow(-2 * number + 2, 5) / 2)));
	public static final Easing EASE_IN_CUBIC_OUT_QUINT = new Easing(number -> {
		number = Mth.clamp(number, 0f, 1f);

		if (number < 0.5f) {
			return (float) (0.5f * Math.pow(number * 2f, 3));
		} else {
			return (float) (1f - 0.5f * (1f - (1 - Math.pow(1 - ((number - 0.5f) * 2f), 5))));
		}
	});

	private final Function<Float, Float> easingFunction;

	private Easing(Function<Float, Float> easingFunction) {
		this.easingFunction = easingFunction;
	}

	public float ease(float number) {
		return easingFunction.apply(number);
	}
}
