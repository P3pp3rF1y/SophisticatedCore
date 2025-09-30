package net.p3pp3rf1y.sophisticatedcore.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;

import java.util.Map;

public interface IItemActionPayloadHandler<T> {
	ResourceLocation id();

	StreamCodec<ByteBuf, T> codec();

	HighlightResult computeHighlight(ServerPlayer player,
									 ItemStackKey stackKey,
									 T clientData);

	Map<Vec3, InventoryHandler> getTargetInventories(Player player, T clientData);

	record HighlightResult(int stackCounts, int itemCounts) {}
}
