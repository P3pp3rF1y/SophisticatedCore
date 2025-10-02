package net.p3pp3rf1y.sophisticatedcore.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;

import java.util.Map;

public interface IItemActionPayloadHandler<T> {
	ResourceLocation id();

	T decode(FriendlyByteBuf packetBuffer);

	void encode(FriendlyByteBuf packetBuffer, T value);

	HighlightResult computeHighlight(ServerPlayer player,
									 ItemStackKey stackKey,
									 T clientData);

	Map<Vec3, InventoryHandler> getTargetInventories(Player player, T clientData);

	record HighlightResult(int stackCounts, int itemCounts) {
	}
}
