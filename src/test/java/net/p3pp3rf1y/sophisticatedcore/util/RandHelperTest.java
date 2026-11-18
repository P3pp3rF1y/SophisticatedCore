package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RandHelperTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void getRandomWeightedElementReturnsEmptyForEmptyListWithoutRandomInteraction() {
		RandomSource random = mock(RandomSource.class);

		var selected = RandHelper.getRandomWeightedElement(random, List.of());

		assertTrue(selected.isEmpty());
		verifyNoInteractions(random);
	}

	@Test
	void getRandomWeightedElementReturnsEmptyForZeroTotalWeightWithoutRandomInteraction() {
		RandomSource random = mock(RandomSource.class);

		var selected = RandHelper.getRandomWeightedElement(random, List.of(new WeightedElement<>(0, "leather"), new WeightedElement<>(0, "copper")));

		assertTrue(selected.isEmpty());
		verifyNoInteractions(random);
	}

	@Test
	void getRandomWeightedElementPreservesPositiveTotalSelectionBehavior() {
		RandomSource random = mock(RandomSource.class);
		when(random.nextInt(4)).thenReturn(2);

		var selected = RandHelper.getRandomWeightedElement(random, List.of(new WeightedElement<>(1, "first"), new WeightedElement<>(2, "second")));

		assertEquals("second", selected.orElseThrow());
		verify(random).nextInt(4);
	}

	@Test
	void getRandomWeightedElementThrowsForNegativeWeightWithoutRandomInteraction() {
		RandomSource random = mock(RandomSource.class);

		assertThrows(IllegalArgumentException.class, () -> RandHelper.getRandomWeightedElement(random, List.of(new WeightedElement<>(-1, "invalid"))));

		verifyNoInteractions(random);
	}
}
