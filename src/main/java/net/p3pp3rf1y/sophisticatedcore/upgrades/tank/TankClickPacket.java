package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

public class TankClickPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = new ResourceLocation(SophisticatedCore.MOD_ID, "tank_click");
	private final int upgradeSlot;

	public TankClickPacket(int upgradeSlot) {
		this.upgradeSlot = upgradeSlot;
	}

	public TankClickPacket(FriendlyByteBuf buffer) {
		this(buffer.readInt());
	}

	public void handle(PlayPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(player.containerMenu instanceof StorageContainerMenuBase<?> storageContainerMenu)) {
			return;
		}
		AbstractContainerMenu containerMenu = player.containerMenu;
		UpgradeContainerBase<?, ?> upgradeContainer = storageContainerMenu.getUpgradeContainers().get(upgradeSlot);
		if (!(upgradeContainer instanceof TankUpgradeContainer tankContainer)) {
			return;
		}
		ItemStack cursorStack = containerMenu.getCarried();
		IFluidHandlerItem fluidHandler = cursorStack.getCapability(Capabilities.FluidHandler.ITEM);
		if (fluidHandler == null) {
			return;
		}

		TankUpgradeWrapper tankWrapper = tankContainer.getUpgradeWrapper();
		FluidStack tankContents = tankWrapper.getContents();
		if (tankContents.isEmpty()) {
			drainHandler(serverPlayer, containerMenu, fluidHandler, tankWrapper);
		} else {
			if (!tankWrapper.fillHandler(fluidHandler, itemStackIn -> {
				containerMenu.setCarried(itemStackIn);
				PacketDistributor.PLAYER.with(serverPlayer).send(new ClientboundContainerSetSlotPacket(-1, containerMenu.incrementStateId(), -1, containerMenu.getCarried()));
			})) {
				drainHandler(serverPlayer, containerMenu, fluidHandler, tankWrapper);
			}
		}

	}

	private static void drainHandler(ServerPlayer player, AbstractContainerMenu containerMenu, IFluidHandlerItem fluidHandler, TankUpgradeWrapper tankWrapper) {
		tankWrapper.drainHandler(fluidHandler, itemStackIn -> {
			containerMenu.setCarried(itemStackIn);
			PacketDistributor.PLAYER.with(player).send(new ClientboundContainerSetSlotPacket(-1, containerMenu.incrementStateId(), -1, containerMenu.getCarried()));
		});
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(upgradeSlot);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
