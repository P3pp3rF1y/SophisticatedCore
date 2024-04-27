package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentInternals;
import net.neoforged.neoforge.network.PacketDistributor;

public class PacketHelper {
	private PacketHelper() {
	}

	public static ItemStack readOversizedItemStack(FriendlyByteBuf buffer) {
		if (!buffer.readBoolean()) {
			return ItemStack.EMPTY;
		} else {
			Item item = buffer.readById(BuiltInRegistries.ITEM);
			int count = buffer.readInt();
			return item == null ? ItemStack.EMPTY : AttachmentInternals.reconstructItemStack(item, count, buffer.readNbt());
		}
	}

	public static void writeOversizedItemStack(ItemStack stack, FriendlyByteBuf buffer) {
		if (stack.isEmpty()) {
			buffer.writeBoolean(false);
		} else {
			buffer.writeBoolean(true);
			Item item = stack.getItem();
			buffer.writeId(BuiltInRegistries.ITEM, item);
			buffer.writeInt(stack.getCount());
			CompoundTag compoundtag = null;
			if (item.isDamageable(stack) || item.shouldOverrideMultiplayerNbt()) {
				compoundtag = stack.getTag();
			}
			compoundtag = AttachmentInternals.addAttachmentsToTag(compoundtag, stack, false);

			buffer.writeNbt(compoundtag);
		}
	}

	public static void sendToAllNear(CustomPacketPayload packet, Entity entity, double range) {
		PacketDistributor.NEAR.with(
				PacketDistributor.TargetPoint.p(entity.getX(), entity.getY(), entity.getZ(), range, entity.level().dimension()).get()
		).send(packet);
	}

	public static void sendToAllNear(CustomPacketPayload packet, ResourceKey<Level> dimension, Vec3 position, double range) {
		PacketDistributor.NEAR.with(
				PacketDistributor.TargetPoint.p(position.x(), position.y(), position.z(), range, dimension).get()
		).send(packet);
	}

	public static void sendToPlayer(CustomPacketPayload packet, Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			sendToPlayer(packet, serverPlayer);
		}
	}
	public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
		PacketDistributor.PLAYER.with(player).send(packet);
	}
}
