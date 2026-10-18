package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.MapMaker;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class StorageWrapperRepository {
	private static final Cache<ItemStack, IStorageWrapper> stackStorageWrappers = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
	private static final Map<ThreadGroup, Cache<UUID, IStorageWrapper>> uuidStorageWrappers = new MapMaker().weakKeys().makeMap();
	private static final AtomicLong wrapperCacheChangeCounter = new AtomicLong();

	private static Cache<UUID, IStorageWrapper> getUuidStorageWrappers() {
		return uuidStorageWrappers.computeIfAbsent(Thread.currentThread().getThreadGroup(), group -> CacheBuilder.newBuilder().weakValues().build());
	}

	public static <T extends IStorageWrapper> Optional<T> getExistingStorageWrapper(ItemStack stack, Class<T> wrapperClass) {
		IStorageWrapper wrapper = stackStorageWrappers.getIfPresent(stack);
		return wrapperClass.isInstance(wrapper) ? Optional.of(wrapperClass.cast(wrapper)) : Optional.empty();
	}

	public static <T extends IStorageWrapper> T getStorageWrapper(ItemStack stack, Class<T> wrapperClass, Function<ItemStack, T> factory) {
		Cache<ItemStack, IStorageWrapper> wrappers = stackStorageWrappers;
		IStorageWrapper wrapper = wrappers.getIfPresent(stack);
		if (!wrapperClass.isInstance(wrapper)) {
			if (wrapper != null) {
				SophisticatedCore.LOGGER.error("StorageWrapperRepository: Wrapper with ItemStack {} is not an instance of {}. Replacing with new instance...",
						stack, wrapperClass);
			}
			wrapper = factory.apply(stack);
			wrappers.put(stack, wrapper);
			wrapperCacheChangeCounter.incrementAndGet();
		}
		return wrapperClass.cast(wrapper);
	}

	public static <T extends IStorageWrapper> T getStorageWrapper(UUID uuid, ItemStack stack, Class<T> wrapperClass, Function<ItemStack, T> factory) {
		Cache<UUID, IStorageWrapper> wrappers = getUuidStorageWrappers();
		IStorageWrapper wrapper = wrappers.getIfPresent(uuid);
		if (wrapper == null) {
			wrapper = getExistingStorageWrapper(stack, wrapperClass).orElseGet(() -> factory.apply(stack));
			wrappers.put(uuid, wrapper);
			wrapperCacheChangeCounter.incrementAndGet();
		} else if (!wrapperClass.isInstance(wrapper)) {
			SophisticatedCore.LOGGER.error("StorageWrapperRepository: Wrapper with UUID {} is not an instance of {}. Replacing with new instance...", uuid,
					wrapperClass);
			wrapper = factory.apply(stack);
			wrappers.put(uuid, wrapper);
			wrapperCacheChangeCounter.incrementAndGet();
		}
		return wrapperClass.cast(wrapper);
	}

	public static void setStorageWrapper(ItemStack stack, IStorageWrapper wrapper) {
		stackStorageWrappers.put(stack, wrapper);
		wrapperCacheChangeCounter.incrementAndGet();
	}

	public static void removeStorageWrapper(UUID uuid, IStorageWrapper wrapper) {
		Cache<UUID, IStorageWrapper> wrappers = getUuidStorageWrappers();
		if (wrappers.getIfPresent(uuid) == wrapper) {
			wrappers.invalidate(uuid);
			wrapperCacheChangeCounter.incrementAndGet();
		}
	}

	public static void removeStorageWrapper(UUID uuid) {
		Cache<UUID, IStorageWrapper> wrappers = getUuidStorageWrappers();
		if (wrappers.getIfPresent(uuid) != null) {
			wrappers.invalidate(uuid);
			wrapperCacheChangeCounter.incrementAndGet();
		}
	}

	public static void refreshStorageWrapperContents(UUID uuid, IStorageWrapper sourceWrapper) {
		IStorageWrapper storageWrapper = getUuidStorageWrappers().getIfPresent(uuid);
		if (storageWrapper != null && storageWrapper != sourceWrapper) {
			storageWrapper.onContentsNbtUpdated();
		}
	}

	public static long getWrapperCacheChangeCounter() {
		return wrapperCacheChangeCounter.get();
	}

	public static void migrateToUuid(IStorageWrapper wrapper, ItemStack stack, UUID uuid) {
		stackStorageWrappers.invalidate(stack);
		getUuidStorageWrappers().put(uuid, wrapper);
		wrapperCacheChangeCounter.incrementAndGet();
	}

	public static void clearCache() {
		stackStorageWrappers.invalidateAll();
		uuidStorageWrappers.values().forEach(Cache::invalidateAll);
		uuidStorageWrappers.clear();
		wrapperCacheChangeCounter.incrementAndGet();
	}
}
