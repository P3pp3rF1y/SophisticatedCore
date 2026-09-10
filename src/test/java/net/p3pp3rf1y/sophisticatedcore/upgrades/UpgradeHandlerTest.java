package net.p3pp3rf1y.sophisticatedcore.upgrades;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class UpgradeHandlerTest {
	@Test
	void accessorSupportsOverlappingCacheMisses() throws Exception {
		UpgradeHandler handler = mock(UpgradeHandler.class);
		Constructor<?> constructor = Class.forName(UpgradeHandler.class.getName() + "$Accessor").getDeclaredConstructor(UpgradeHandler.class);
		constructor.setAccessible(true);
		IUpgradeWrapperAccessor accessor = (IUpgradeWrapperAccessor) constructor.newInstance(handler);

		CountDownLatch lookupStarted = new CountDownLatch(1);
		CountDownLatch otherLookupFinished = new CountDownLatch(1);
		when(handler.getListOfWrappersThatImplement(Runnable.class)).thenAnswer(i -> {
			lookupStarted.countDown();
			assertTrue(otherLookupFinished.await(5, TimeUnit.SECONDS));
			return List.of();
		});
		when(handler.getListOfWrappersThatImplement(AutoCloseable.class)).thenReturn(List.of());

		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			Future<List<Runnable>> first = executor.submit(() -> accessor.getWrappersThatImplement(Runnable.class));
			assertTrue(lookupStarted.await(5, TimeUnit.SECONDS));
			assertTrue(accessor.getWrappersThatImplement(AutoCloseable.class).isEmpty());
			otherLookupFinished.countDown();
			assertTrue(first.get(5, TimeUnit.SECONDS).isEmpty());
			assertSame(accessor.getWrappersThatImplement(Runnable.class), accessor.getWrappersThatImplementFromMainStorage(Runnable.class));
			verify(handler).getListOfWrappersThatImplement(Runnable.class);

			accessor.clearCache();
			accessor.getWrappersThatImplement(Runnable.class);
			verify(handler, times(2)).getListOfWrappersThatImplement(Runnable.class);
		} finally {
			otherLookupFinished.countDown();
			executor.shutdownNow();
		}
	}
}
