package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IFilterSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.Optional;

public record ItemStackReference(int containerId, int slot, String handlerName, String identifier) {
	public static final int PLAYER_INVENTORY = -1;
	public static final int EQUIPMENT_HANDLER = -2;

	public static void encode(ItemStackReference reference, FriendlyByteBuf buffer) {
		buffer.writeInt(reference.containerId);
		buffer.writeInt(reference.slot);
		buffer.writeUtf(reference.handlerName, 256);
		buffer.writeUtf(reference.identifier, 256);
	}

	public static ItemStackReference decode(FriendlyByteBuf buffer) {
		return new ItemStackReference(buffer.readInt(), buffer.readInt(), buffer.readUtf(256), buffer.readUtf(256));
	}

	public static Optional<ItemStackReference> find(Player player, ItemStack stack) {
		for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
			if (ItemStack.isSameItemSameTags(stack, player.getInventory().getItem(index))) {
				return Optional.of(new ItemStackReference(PLAYER_INVENTORY, index, "", ""));
			}
		}
		if (ItemStack.isSameItemSameTags(stack, player.containerMenu.getCarried())) {
			return Optional.of(new ItemStackReference(player.containerMenu.containerId, -1, "", ""));
		}
		int size = getSlotCount(player);
		for (int index = 0; index < size; index++) {
			Slot menuSlot = player.containerMenu.getSlot(index);
			if (isAccessible(menuSlot, player) && ItemStack.isSameItemSameTags(stack, menuSlot.getItem())) {
				return Optional.of(new ItemStackReference(player.containerMenu.containerId, index, "", ""));
			}
		}
		return Optional.empty();
	}

	public ItemStack resolve(Player player) {
		if (!handlerName.isEmpty() || !identifier.isEmpty()) {
			return ItemStack.EMPTY;
		}
		if (containerId == PLAYER_INVENTORY) {
			return slot >= 0 && slot < player.getInventory().getContainerSize() ? player.getInventory().getItem(slot) : ItemStack.EMPTY;
		}
		if (containerId != player.containerMenu.containerId || !player.containerMenu.stillValid(player)) {
			return ItemStack.EMPTY;
		}
		if (slot == -1) {
			return player.containerMenu.getCarried();
		}
		if (slot < 0 || slot >= getSlotCount(player)) {
			return ItemStack.EMPTY;
		}
		Slot menuSlot = player.containerMenu.getSlot(slot);
		return isAccessible(menuSlot, player) ? menuSlot.getItem() : ItemStack.EMPTY;
	}

	private static int getSlotCount(Player player) {
		return player.containerMenu instanceof StorageContainerMenuBase<?> storageMenu ? storageMenu.getTotalSlotsNumber() : player.containerMenu.slots.size();
	}

	private static boolean isAccessible(Slot slot, Player player) {
		return !(slot instanceof IFilterSlot) && slot.isActive() && slot.mayPickup(player);
	}
}
