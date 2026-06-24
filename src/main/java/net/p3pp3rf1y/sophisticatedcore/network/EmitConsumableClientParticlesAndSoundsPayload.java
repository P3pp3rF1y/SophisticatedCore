package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

public record EmitConsumableClientParticlesAndSoundsPayload(ItemStack stack) implements CustomPacketPayload {
	public static final Type<EmitConsumableClientParticlesAndSoundsPayload> TYPE = new Type<>(
			SophisticatedCore.getIdentifier("emit_consumable_client_particles_and_sounds"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EmitConsumableClientParticlesAndSoundsPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.OPTIONAL_STREAM_CODEC, EmitConsumableClientParticlesAndSoundsPayload::stack, EmitConsumableClientParticlesAndSoundsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(EmitConsumableClientParticlesAndSoundsPayload payload, IPayloadContext context) {
		Player player = context.player();

		ItemStack stack = payload.stack();
		Consumable consumable = stack.get(DataComponents.CONSUMABLE);
		if (consumable == null) {
			return;
		}
		consumable.emitParticlesAndSounds(player.getRandom(), player, stack, 5);
	}
}
