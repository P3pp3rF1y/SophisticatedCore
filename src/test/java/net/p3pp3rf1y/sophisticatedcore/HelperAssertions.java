package net.p3pp3rf1y.sophisticatedcore;

import net.minecraft.world.item.ItemStack;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;

public class HelperAssertions {
	public static void assertStackEquals(ItemStack expected, ItemStack actual, Object message) {
		if (!ItemStack.matches(expected, actual)) {
			assertionFailure().message(message).expected(expected).actual(actual).buildAndThrow();
		}
	}
}
