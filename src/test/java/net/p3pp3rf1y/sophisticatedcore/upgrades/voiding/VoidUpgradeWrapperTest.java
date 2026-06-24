package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class VoidUpgradeWrapperTest {

	@Test
	void overflowMatchIncludesPartialStackWhenNbtIsIgnored() {
		Object partiallyFilledStack = new Object();

		Assertions.assertTrue(VoidUpgradeWrapper.hasOverflowMatch(Set.of(), Set.of(partiallyFilledStack), stackKey -> stackKey == partiallyFilledStack));
	}
}
