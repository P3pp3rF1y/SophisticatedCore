package net.p3pp3rf1y.sophisticatedcore.common.gui;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.HashCode;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.HashOps;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.network.SyncContainerStacksPayload;
import net.p3pp3rf1y.sophisticatedcore.network.SyncSlotStackPayload;

import java.util.List;

public class HighStackCountSynchronizer implements ContainerSynchronizer {
	private final ServerPlayer player;

	private final LoadingCache<TypedDataComponent<?>, Integer> cache;

	public HighStackCountSynchronizer(ServerPlayer player) {
		this.player = player;
		cache = CacheBuilder.newBuilder().maximumSize(256L).build(new CacheLoader<>() {
			private final DynamicOps<HashCode> registryHashOps = player.registryAccess().createSerializationContext(HashOps.CRC32C_INSTANCE);

			@Override
			public Integer load(TypedDataComponent<?> key) {
				return key.encodeValue(registryHashOps).getOrThrow(e -> new IllegalArgumentException("Failed to hash " + key + ": " + e)).asInt();
			}
		});
	}

	@Override
	public void sendInitialData(AbstractContainerMenu containerMenu, List<ItemStack> stacks, ItemStack carriedStack, int[] dataSlots) {
		PacketDistributor.sendToPlayer(player,
				new SyncContainerStacksPayload(containerMenu.containerId, containerMenu.incrementStateId(), stacks, carriedStack));
	}

	@Override
	public void sendSlotChange(AbstractContainerMenu containerMenu, int slotInd, ItemStack stack) {
		PacketDistributor.sendToPlayer(player, new SyncSlotStackPayload(containerMenu.containerId, containerMenu.incrementStateId(), slotInd, stack));
	}

	@Override
	public void sendCarriedChange(AbstractContainerMenu containerMenu, ItemStack stack) {
		player.connection.send(new ClientboundSetCursorItemPacket(stack));
	}

	@Override
	public void sendDataChange(AbstractContainerMenu containerMenu, int slotInd, int data) {
		// noop - not used in StorageContainer
	}

	@Override
	public RemoteSlot createSlot() {
		return new RemoteSlot.Synchronized(cache::getUnchecked);
	}
}
