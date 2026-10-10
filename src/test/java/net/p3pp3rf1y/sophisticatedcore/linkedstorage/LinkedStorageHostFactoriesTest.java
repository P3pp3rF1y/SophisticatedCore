package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class LinkedStorageHostFactoriesTest {
	@Test
	void areCompatibleAcceptsTheSameFactoryWithoutRegistration() {
		ResourceLocation factoryId = newFactoryId();

		boolean compatible = LinkedStorageHostFactories.areCompatible(factoryId, factoryId);

		assertTrue(compatible);
	}

	@Test
	void registerCompatibilityAcceptsBothDirectionsWithoutCreatingHosts() {
		ResourceLocation first = newFactoryId();
		ResourceLocation second = newFactoryId();
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
		ResourceLocation first = newFactoryId();
		ResourceLocation second = newFactoryId();
		ResourceLocation third = newFactoryId();
		ResourceLocation unknown = newFactoryId();
		LinkedStorageHostFactories.registerCompatibility(first, second);
		LinkedStorageHostFactories.registerCompatibility(second, third);

		boolean transitive = LinkedStorageHostFactories.areCompatible(first, third);
		boolean reverseTransitive = LinkedStorageHostFactories.areCompatible(third, first);
		boolean unregistered = LinkedStorageHostFactories.areCompatible(first, unknown);

		assertFalse(transitive);
		assertFalse(reverseTransitive);
		assertFalse(unregistered);
	}

	private static ResourceLocation newFactoryId() {
		return ResourceLocation.fromNamespaceAndPath("test", UUID.randomUUID().toString());
	}
}
