package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class StorageWrapperRepository {

    private static final Cache<ItemStack, IStorageWrapper> stackStorageWrappers = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
    private static final Cache<UUID, IStorageWrapper> uuidStorageWrappers = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();

    public static <T extends IStorageWrapper> Optional<T> getExistingStorageWrapper(ItemStack stack, Class<T> wrapperClass) {
        IStorageWrapper storageWrapper = stackStorageWrappers.getIfPresent(stack);
        if (wrapperClass.isInstance(storageWrapper)) {
            return Optional.of(wrapperClass.cast(storageWrapper));
        }
        return Optional.empty();
    }

    public static <T extends IStorageWrapper> T getStorageWrapper(ItemStack stack, Class<T> wrapperClass, Function<ItemStack, T> factory) {
        IStorageWrapper storageWrapper = stackStorageWrappers.getIfPresent(stack);
        if (storageWrapper == null) {
            storageWrapper = instantiateWrapper(stack, factory);
            stackStorageWrappers.put(stack, storageWrapper);
        } else if (!wrapperClass.isInstance(storageWrapper)) {
            SophisticatedCore.LOGGER.error("StorageWrapperRepository: Wrapper with ItemStack {} is not an instance of {}. Replacing with new instance...", stack, wrapperClass);
            stackStorageWrappers.invalidate(stack);
            storageWrapper = instantiateWrapper(stack, factory);
            stackStorageWrappers.put(stack, storageWrapper);
        }
        return wrapperClass.cast(storageWrapper);
    }

/*    public static <T extends IStorageWrapper> T getStorageWrapper(UUID uuid, Class<T> wrapperClass, BiFunction<ItemStack, RegistryAccess, T> factory) { //TODO future UUID based caching and retrieval
        IStorageWrapper storageWrapper = uuidStorageWrappers.getIfPresent(uuid);
        if (storageWrapper == null) {
            storageWrapper = instantiateWrapper(factory);
            uuidStorageWrappers.put(uuid, storageWrapper);
        } else if (!wrapperClass.isInstance(storageWrapper)) {
            SophisticatedCore.LOGGER.error("StorageWrapperRepository: Wrapper with UUID {} is not an instance of {}. Replacing with new instance...", uuid, wrapperClass);
            uuidStorageWrappers.invalidate(uuid);
            storageWrapper = instantiateWrapper(factory);
            uuidStorageWrappers.put(uuid, storageWrapper);
        }
        return wrapperClass.cast(storageWrapper);
    }*/

    private static <T extends IStorageWrapper> T instantiateWrapper(ItemStack stack, Function<ItemStack, T> instantiate) {
        return instantiate.apply(stack);
    }

    public static void migrateToUuid(IStorageWrapper storageWrapper, ItemStack stack, UUID storageUuid) {
        stackStorageWrappers.invalidate(stack);
        uuidStorageWrappers.put(storageUuid, storageWrapper);
    }

    public static void clearCache() {
        stackStorageWrappers.invalidateAll();
        uuidStorageWrappers.invalidateAll();
    }
}
