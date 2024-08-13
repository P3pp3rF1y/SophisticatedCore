package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.Validate;

import java.util.Optional;

public class RegistryHelper {
	private RegistryHelper() {
	}

	public static ResourceLocation getItemKey(Item item) {
		ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
		Validate.notNull(itemKey, "itemKey");
		return itemKey;
	}

	public static <V> Optional<ResourceLocation> getRegistryName(Registry<V> registry, V registryEntry) {
		return Optional.ofNullable(registry.getKey(registryEntry));
	}

	public static Optional<RegistryAccess> getRegistryAccess() {
		if (Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER && FMLEnvironment.dist.isClient()) {
			return ClientRegistryHelper.getRegistryAccess();
		}

		MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
		if (currentServer == null) {
			return Optional.empty();
		}

		return Optional.of(currentServer.registryAccess());
	}
}