package net.p3pp3rf1y.sophisticatedcore.client.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public interface IItemActionPayloadBuilder<T> {
	ResourceLocation getPayloadHandlerId();

	T buildClientRequestData(Player player);
}
