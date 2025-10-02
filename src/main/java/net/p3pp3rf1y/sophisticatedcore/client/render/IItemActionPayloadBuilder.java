package net.p3pp3rf1y.sophisticatedcore.client.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public interface IItemActionPayloadBuilder<T> {
	ResourceLocation getPayloadHandlerId();

	Optional<T> buildClientRequestData(Player player);
}
