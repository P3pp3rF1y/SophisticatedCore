package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.MapMaker;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class StorageWrapperRepository {

	private static final Map<ThreadGroup, Cache<ItemStack, IStorageWrapper>> stackStorageWrappers = new MapMaker().weakKeys().makeMap();
	private static final Map<ThreadGroup, Cache<UUID, IStorageWrapper>> uuidStorageWrappers = new MapMaker().weakKeys().makeMap();
	private static final Map<ThreadGroup, Cache<UUID, Set<IStorageWrapper>>> storageWrappersByUuid = new MapMaker().weakKeys().makeMap();
	private static final AtomicLong wrapperCacheChangeCounter = new AtomicLong();

	public static <T extends IStorageWrapper> Optional<T> getExistingStorageWrapper(ItemStack stack, Class<T> wrapperClass) {
		IStorageWrapper storageWrapper = getStackStorageWrappers().getIfPresent(stack);
		if (wrapperClass.isInstance(storageWrapper)) {
			return Optional.of(wrapperClass.cast(storageWrapper));
		}
		return Optional.empty();
	}

	public static <T extends IStorageWrapper> T getStorageWrapper(ItemStack stack, Class<T> wrapperClass, Function<ItemStack, T> factory) {
		Cache<ItemStack, IStorageWrapper> wrappers = getStackStorageWrappers();
		IStorageWrapper storageWrapper = wrappers.getIfPresent(stack);
		if (storageWrapper == null) {
			storageWrapper = instantiateWrapper(stack, factory);
			wrappers.put(stack, storageWrapper);
			wrapperCacheChangeCounter.incrementAndGet();
		} else if (!wrapperClass.isInstance(storageWrapper)) {
			SophisticatedCore.LOGGER.error("StorageWrapperRepository: Wrapper with ItemStack {} is not an instance of {}. Replacing with new instance...",
					stack, wrapperClass);
			wrappers.invalidate(stack);
			storageWrapper = instantiateWrapper(stack, factory);
			wrappers.put(stack, storageWrapper);
			wrapperCacheChangeCounter.incrementAndGet();
		}
		return wrapperClass.cast(storageWrapper);
	}

	public static void setStorageWrapper(ItemStack stack, IStorageWrapper storageWrapper) {
		getStackStorageWrappers().put(stack, storageWrapper);
		wrapperCacheChangeCounter.incrementAndGet();
	}

	public static long getWrapperCacheChangeCounter() {
		return wrapperCacheChangeCounter.get();
	}

	public static void registerStorageWrapper(UUID storageUuid, IStorageWrapper storageWrapper) {
		getStorageWrappersByUuid().asMap().computeIfAbsent(storageUuid, uuid -> Collections.newSetFromMap(new MapMaker().weakKeys().makeMap()))
				.add(storageWrapper);
	}

	public static void invalidateStorageWrapperContents(UUID storageUuid, IStorageWrapper sourceWrapper) {
		Set<IStorageWrapper> storageWrappers = getStorageWrappersByUuid().getIfPresent(storageUuid);
		if (storageWrappers == null) {
			return;
		}

		storageWrappers.forEach(storageWrapper -> {
			if (storageWrapper != sourceWrapper) {
				storageWrapper.onContentsUpdated();
			}
		});
	}

	/*
	 * public static <T extends IStorageWrapper> T getStorageWrapper(UUID uuid, Class<T> wrapperClass, BiFunction<ItemStack, RegistryAccess, T> factory) {
	 * //TODO future UUID based caching and retrieval IStorageWrapper storageWrapper = uuidStorageWrappers.getIfPresent(uuid); if (storageWrapper == null) {
	 * storageWrapper = instantiateWrapper(factory); uuidStorageWrappers.put(uuid, storageWrapper); } else if (!wrapperClass.isInstance(storageWrapper)) {
	 * SophisticatedCore.LOGGER.error("StorageWrapperRepository: Wrapper with UUID {} is not an instance of {}. Replacing with new instance...", uuid,
	 * wrapperClass); uuidStorageWrappers.invalidate(uuid); storageWrapper = instantiateWrapper(factory); uuidStorageWrappers.put(uuid, storageWrapper); }
	 * return wrapperClass.cast(storageWrapper); }
	 */

	private static <T extends IStorageWrapper> T instantiateWrapper(ItemStack stack, Function<ItemStack, T> instantiate) {
		return instantiate.apply(stack);
	}

	public static void migrateToUuid(IStorageWrapper storageWrapper, ItemStack stack, UUID storageUuid) {
		getStackStorageWrappers().invalidate(stack);
		getUuidStorageWrappers().put(storageUuid, storageWrapper);
		registerStorageWrapper(storageUuid, storageWrapper);
		wrapperCacheChangeCounter.incrementAndGet();
	}

	public static void clearCache() {
		stackStorageWrappers.values().forEach(Cache::invalidateAll);
		uuidStorageWrappers.values().forEach(Cache::invalidateAll);
		storageWrappersByUuid.values().forEach(Cache::invalidateAll);
		stackStorageWrappers.clear();
		uuidStorageWrappers.clear();
		storageWrappersByUuid.clear();
		wrapperCacheChangeCounter.incrementAndGet();
	}

	private static Cache<ItemStack, IStorageWrapper> getStackStorageWrappers() {
		return stackStorageWrappers.computeIfAbsent(Thread.currentThread().getThreadGroup(),
				threadGroup -> CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build());
	}

	private static Cache<UUID, IStorageWrapper> getUuidStorageWrappers() {
		return uuidStorageWrappers.computeIfAbsent(Thread.currentThread().getThreadGroup(),
				threadGroup -> CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build());
	}

	private static Cache<UUID, Set<IStorageWrapper>> getStorageWrappersByUuid() {
		return storageWrappersByUuid.computeIfAbsent(Thread.currentThread().getThreadGroup(),
				threadGroup -> CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build());
	}
}
