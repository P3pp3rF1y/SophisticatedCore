package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class LinkedStorageHostFactoriesTest {
	@Test
	void areCompatibleAcceptsTheSameFactoryWithoutRegistration() {
		Identifier factoryId = newFactoryId();

		boolean compatible = LinkedStorageHostFactories.areCompatible(factoryId, factoryId);

		assertTrue(compatible);
	}

	@Test
	void registerCompatibilityAcceptsBothDirectionsWithoutCreatingHosts() {
		Identifier first = newFactoryId();
		Identifier second = newFactoryId();
		ILinkedStorageHostFactory factory = mock(ILinkedStorageHostFactory.class);
		LinkedStorageHostFactories.register(first, factory);
		boolean beforeRegistration = LinkedStorageHostFactories.areCompatible(first, second);

		LinkedStorageHostFactories.registerCompatibility(first, second);
		LinkedStorageHostFactories.registerCompatibility(second, first);
		boolean forward = LinkedStorageHostFactories.areCompatible(first, second);
		boolean reverse = LinkedStorageHostFactories.areCompatible(second, first);

		assertFalse(beforeRegistration);
		assertTrue(forward);
		assertTrue(reverse);
		verifyNoInteractions(factory);
	}

	@Test
	void areCompatibleRejectsUnknownAndTransitiveRelationships() {
		Identifier first = newFactoryId();
		Identifier second = newFactoryId();
		Identifier third = newFactoryId();
		Identifier unknown = newFactoryId();
		LinkedStorageHostFactories.registerCompatibility(first, second);
		LinkedStorageHostFactories.registerCompatibility(second, third);

		boolean transitive = LinkedStorageHostFactories.areCompatible(first, third);
		boolean reverseTransitive = LinkedStorageHostFactories.areCompatible(third, first);
		boolean unregistered = LinkedStorageHostFactories.areCompatible(first, unknown);

		assertFalse(transitive);
		assertFalse(reverseTransitive);
		assertFalse(unregistered);
	}

	private static Identifier newFactoryId() {
		return Identifier.fromNamespaceAndPath("test", UUID.randomUUID().toString());
	}
}
