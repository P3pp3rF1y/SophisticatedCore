package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.Validate;

import java.util.Optional;

public class RegistryHelper {
	private RegistryHelper() {
	}

	public static Identifier getItemKey(Item item) {
		Identifier itemKey = BuiltInRegistries.ITEM.getKey(item);
		Validate.notNull(itemKey, "itemKey");
		return itemKey;
	}

	public static <V> Optional<Identifier> getRegistryName(Registry<V> registry, V registryEntry) {
		return Optional.ofNullable(registry.getKey(registryEntry));
	}

	public static Optional<RegistryAccess> getRegistryAccess() {
		if (Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER && FMLEnvironment.getDist().isClient()) {
			return ClientLevelHelper.getRegistryAccess();
		}

		MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
		if (currentServer == null) {
			return Optional.empty();
		}

		return Optional.of(currentServer.registryAccess());
	}

	public static Identifier getBlockKey(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block);
	}
}