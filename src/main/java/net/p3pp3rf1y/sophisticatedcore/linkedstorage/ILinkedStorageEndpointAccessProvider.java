package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.network.ItemStackReference;

@FunctionalInterface
public interface ILinkedStorageEndpointAccessProvider {
	ItemStack resolveEndpointStack(ServerPlayer player, ItemStackReference reference);
}
