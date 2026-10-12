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

		assertTrue(LinkedStorageHostFactories.areCompatible(factoryId, factoryId));
	}

	@Test
	void registerCompatibilityAcceptsBothDirectionsWithoutCreatingHosts() {
		ResourceLocation first = newFactoryId();
		ResourceLocation second = newFactoryId();
		ILinkedStorageHostFactory factory = mock(ILinkedStorageHostFactory.class);
		LinkedStorageHostFactories.register(first, factory);
		assertFalse(LinkedStorageHostFactories.areCompatible(first, second));

		LinkedStorageHostFactories.registerCompatibility(first, second);
		LinkedStorageHostFactories.registerCompatibility(second, first);

		assertTrue(LinkedStorageHostFactories.areCompatible(first, second));
		assertTrue(LinkedStorageHostFactories.areCompatible(second, first));
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

		assertFalse(LinkedStorageHostFactories.areCompatible(first, third));
		assertFalse(LinkedStorageHostFactories.areCompatible(third, first));
		assertFalse(LinkedStorageHostFactories.areCompatible(first, unknown));
	}

	private static ResourceLocation newFactoryId() {
		return new ResourceLocation("test", UUID.randomUUID().toString());
	}
}
