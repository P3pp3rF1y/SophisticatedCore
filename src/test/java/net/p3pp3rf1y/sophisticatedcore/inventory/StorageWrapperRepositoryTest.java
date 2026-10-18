package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class StorageWrapperRepositoryTest {
	@AfterEach
	void tearDown() {
		StorageWrapperRepository.clearCache();
	}

	@Test
	void uuidWrappersAreScopedToTheirThreadGroupAndClearCacheClearsBothGroups() throws Throwable {
		UUID uuid = UUID.randomUUID();
		ItemStack stack = mock(ItemStack.class);
		ThreadGroup serverGroup = new ThreadGroup("server");
		ThreadGroup clientGroup = new ThreadGroup("client");
		AtomicReference<IStorageWrapper> server = new AtomicReference<>();
		AtomicReference<IStorageWrapper> client = new AtomicReference<>();

		runInThreadGroup(serverGroup, () -> {
			server.set(StorageWrapperRepository.getStorageWrapper(uuid, stack, IStorageWrapper.class, ignored -> mock(IStorageWrapper.class)));
		});
		runInThreadGroup(clientGroup, () -> {
			client.set(StorageWrapperRepository.getStorageWrapper(uuid, stack, IStorageWrapper.class, ignored -> mock(IStorageWrapper.class)));
		});

		assertNotSame(server.get(), client.get());
		StorageWrapperRepository.setStorageWrapper(stack, server.get());
		runInThreadGroup(clientGroup,
				() -> assertSame(server.get(), StorageWrapperRepository.getExistingStorageWrapper(stack, IStorageWrapper.class).orElseThrow()));
		StorageWrapperRepository.clearCache();
		assertTrue(StorageWrapperRepository.getExistingStorageWrapper(stack, IStorageWrapper.class).isEmpty());
		runInThreadGroup(serverGroup, () -> assertNotSame(server.get(),
				StorageWrapperRepository.getStorageWrapper(uuid, stack, IStorageWrapper.class, ignored -> mock(IStorageWrapper.class))));
		runInThreadGroup(clientGroup, () -> assertNotSame(client.get(),
				StorageWrapperRepository.getStorageWrapper(uuid, stack, IStorageWrapper.class, ignored -> mock(IStorageWrapper.class))));
	}

	@Test
	void uuidLookupPromotesExistingStackWrapperAndReusesItForCopiedStacks() {
		UUID uuid = UUID.randomUUID();
		ItemStack original = mock(ItemStack.class);
		ItemStack copy = mock(ItemStack.class);
		IStorageWrapper wrapper = mock(IStorageWrapper.class);
		@SuppressWarnings("unchecked")
		Function<ItemStack, IStorageWrapper> factory = mock(Function.class);
		StorageWrapperRepository.setStorageWrapper(original, wrapper);
		long changeCount = StorageWrapperRepository.getWrapperCacheChangeCounter();

		IStorageWrapper promoted = StorageWrapperRepository.getStorageWrapper(uuid, original, IStorageWrapper.class, factory);
		IStorageWrapper reused = StorageWrapperRepository.getStorageWrapper(uuid, copy, IStorageWrapper.class, factory);

		assertSame(wrapper, promoted);
		assertSame(wrapper, reused);
		assertSame(wrapper, StorageWrapperRepository.getExistingStorageWrapper(original, IStorageWrapper.class).orElseThrow());
		verify(factory, never()).apply(original);
		verify(factory, never()).apply(copy);
		assertEquals(changeCount + 1, StorageWrapperRepository.getWrapperCacheChangeCounter());
	}

	@Test
	void staleOwnerCannotEvictNewUuidOwner() {
		UUID uuid = UUID.randomUUID();
		ItemStack stack = mock(ItemStack.class);
		IStorageWrapper replacement = StorageWrapperRepository.getStorageWrapper(uuid, stack, IStorageWrapper.class, ignored -> mock(IStorageWrapper.class));
		IStorageWrapper stale = mock(IStorageWrapper.class);
		long changeCount = StorageWrapperRepository.getWrapperCacheChangeCounter();

		StorageWrapperRepository.removeStorageWrapper(uuid, stale);

		assertSame(replacement, StorageWrapperRepository.getStorageWrapper(uuid, stack, IStorageWrapper.class, ignored -> stale));
		assertEquals(changeCount, StorageWrapperRepository.getWrapperCacheChangeCounter());
	}

	@Test
	void wrongClassUuidOwnerUsesFactoryInsteadOfPromotingStackWrapper() {
		UUID uuid = UUID.randomUUID();
		ItemStack stack = mock(ItemStack.class);
		IStorageWrapper wrongClass = StorageWrapperRepository.getStorageWrapper(uuid, stack, IStorageWrapper.class, ignored -> mock(IStorageWrapper.class));
		NoopStorageWrapper stackWrapper = mock(NoopStorageWrapper.class);
		NoopStorageWrapper replacement = mock(NoopStorageWrapper.class);
		StorageWrapperRepository.setStorageWrapper(stack, stackWrapper);

		assertSame(replacement, StorageWrapperRepository.getStorageWrapper(uuid, stack, NoopStorageWrapper.class, ignored -> replacement));
		assertSame(stackWrapper, StorageWrapperRepository.getExistingStorageWrapper(stack, NoopStorageWrapper.class).orElseThrow());
		StorageWrapperRepository.removeStorageWrapper(uuid, wrongClass);
		assertSame(replacement, StorageWrapperRepository.getStorageWrapper(uuid, stack, NoopStorageWrapper.class, ignored -> stackWrapper));
	}

	private static void runInThreadGroup(ThreadGroup group, Runnable action) throws Throwable {
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread thread = new Thread(group, () -> {
			try {
				action.run();
			} catch (Throwable throwable) {
				failure.set(throwable);
			}
		});
		thread.start();
		thread.join();
		if (failure.get() != null) {
			throw failure.get();
		}
	}
}
