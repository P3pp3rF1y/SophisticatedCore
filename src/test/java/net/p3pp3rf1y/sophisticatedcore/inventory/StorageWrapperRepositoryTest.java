package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class StorageWrapperRepositoryTest {
	@AfterEach
	void tearDown() {
		StorageWrapperRepository.clearCache();
	}

	@Test
	void invalidatingContentsInOneThreadGroupDoesNotInvalidateWrappersFromAnotherThreadGroup() throws Throwable {
		UUID storageUuid = UUID.randomUUID();
		IStorageWrapper serverWrapper = mock(IStorageWrapper.class);
		IStorageWrapper clientWrapper = mock(IStorageWrapper.class);

		runInThreadGroup("server", () -> StorageWrapperRepository.registerStorageWrapper(storageUuid, serverWrapper));
		runInThreadGroup("client", () -> {
			StorageWrapperRepository.registerStorageWrapper(storageUuid, clientWrapper);
			StorageWrapperRepository.invalidateStorageWrapperContents(storageUuid, clientWrapper);
		});

		verify(serverWrapper, never()).onContentsUpdated();
		verify(clientWrapper, never()).onContentsUpdated();
	}

	@Test
	void invalidatingContentsInThreadGroupStillInvalidatesOtherWrappersFromSameThreadGroup() throws Throwable {
		UUID storageUuid = UUID.randomUUID();
		IStorageWrapper sourceWrapper = mock(IStorageWrapper.class);
		IStorageWrapper otherWrapper = mock(IStorageWrapper.class);

		runInThreadGroup("server", () -> {
			StorageWrapperRepository.registerStorageWrapper(storageUuid, sourceWrapper);
			StorageWrapperRepository.registerStorageWrapper(storageUuid, otherWrapper);
			StorageWrapperRepository.invalidateStorageWrapperContents(storageUuid, sourceWrapper);
		});

		verify(sourceWrapper, never()).onContentsUpdated();
		verify(otherWrapper).onContentsUpdated();
	}

	@Test
	void stackWrapperCacheIsScopedByThreadGroup() throws Throwable {
		ItemStack stack = ItemStack.EMPTY;
		IStorageWrapper serverWrapper = mock(IStorageWrapper.class);
		IStorageWrapper clientWrapper = mock(IStorageWrapper.class);
		AtomicReference<IStorageWrapper> firstServerLookup = new AtomicReference<>();
		AtomicReference<IStorageWrapper> secondServerLookup = new AtomicReference<>();
		AtomicReference<IStorageWrapper> clientLookup = new AtomicReference<>();

		runInThreadGroup("server", () -> {
			firstServerLookup.set(StorageWrapperRepository.getStorageWrapper(stack, IStorageWrapper.class, ignored -> serverWrapper));
			secondServerLookup.set(StorageWrapperRepository.getStorageWrapper(stack, IStorageWrapper.class, ignored -> mock(IStorageWrapper.class)));
		});
		runInThreadGroup("client", () -> clientLookup.set(StorageWrapperRepository.getStorageWrapper(stack, IStorageWrapper.class, ignored -> clientWrapper)));

		assertSame(firstServerLookup.get(), secondServerLookup.get());
		assertNotSame(firstServerLookup.get(), clientLookup.get());
	}

	@Test
	void getStorageWrapperPromotesStackWrapperAndReusesItForOtherStacks() {
		UUID storageUuid = UUID.randomUUID();
		ItemStack originalStack = mock(ItemStack.class);
		ItemStack copiedStack = mock(ItemStack.class);
		IStorageWrapper wrapper = mock(IStorageWrapper.class);
		@SuppressWarnings("unchecked")
		Function<ItemStack, IStorageWrapper> factory = mock(Function.class);

		StorageWrapperRepository.setStorageWrapper(originalStack, wrapper);
		IStorageWrapper promotedWrapper = StorageWrapperRepository.getStorageWrapper(storageUuid, originalStack, IStorageWrapper.class, factory);
		IStorageWrapper copiedStackWrapper = StorageWrapperRepository.getStorageWrapper(storageUuid, copiedStack, IStorageWrapper.class, factory);
		IStorageWrapper existingOriginalStackWrapper = StorageWrapperRepository.getExistingStorageWrapper(originalStack, IStorageWrapper.class).orElseThrow();

		assertSame(wrapper, promotedWrapper);
		assertSame(wrapper, copiedStackWrapper);
		assertSame(wrapper, existingOriginalStackWrapper);
		verify(factory, never()).apply(originalStack);
		verify(factory, never()).apply(copiedStack);
	}

	@Test
	void removeStorageWrapperKeepsDifferentRegisteredWrapper() {
		UUID storageUuid = UUID.randomUUID();
		ItemStack stack = mock(ItemStack.class);
		IStorageWrapper wrapper = mock(IStorageWrapper.class);
		IStorageWrapper staleWrapper = mock(IStorageWrapper.class);

		StorageWrapperRepository.setStorageWrapper(storageUuid, wrapper);
		StorageWrapperRepository.removeStorageWrapper(storageUuid, staleWrapper);
		IStorageWrapper remainingWrapper = StorageWrapperRepository.getStorageWrapper(storageUuid, stack, IStorageWrapper.class,
				ignored -> mock(IStorageWrapper.class));

		assertSame(wrapper, remainingWrapper);
	}

	private void runInThreadGroup(String threadGroupName, ThrowingRunnable runnable) throws Throwable {
		ThreadGroup threadGroup = new ThreadGroup(threadGroupName);
		AtomicReference<Throwable> thrown = new AtomicReference<>();
		Thread thread = new Thread(threadGroup, () -> {
			try {
				runnable.run();
			} catch (Throwable e) {
				thrown.set(e);
			}
		});
		thread.start();
		thread.join();
		if (thrown.get() != null) {
			throw thrown.get();
		}
	}

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run() throws Throwable;
	}
}
