package net.p3pp3rf1y.sophisticatedcore.util;

public class MathHelper {
	private MathHelper() {}

	public static int intMaxCappedAddition(int a, int b) {
		return Integer.MAX_VALUE - a < b ? Integer.MAX_VALUE : a + b;
	}

	public static int intMaxCappedMultiply(int a, int b) {
		return Integer.MAX_VALUE / a < b ? Integer.MAX_VALUE : a * b;
	}
}
