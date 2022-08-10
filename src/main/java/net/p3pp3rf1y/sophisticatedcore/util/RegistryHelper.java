package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.commons.lang3.Validate;

import java.util.Optional;

public class RegistryHelper {
	private RegistryHelper() {}

	public static ResourceLocation getItemKey(Item item) {
		ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(item);
		Validate.notNull(itemKey, "itemKey");
		return itemKey;
	}

	public static <V> Optional<ResourceLocation> getRegistryName(IForgeRegistry<V> registry, V registryEntry) {
		return Optional.ofNullable(registry.getKey(registryEntry));
	}

	public static Optional<Item> getItemFromName(String itemName) {
		ResourceLocation key = new ResourceLocation(itemName);
		if (ForgeRegistries.ITEMS.containsKey(key)) {
			//noinspection ConstantConditions - checked above with containsKey
			return Optional.of(ForgeRegistries.ITEMS.getValue(key));
		}
		return Optional.empty();
	}
}