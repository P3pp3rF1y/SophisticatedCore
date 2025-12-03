package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.world.inventory.StackCopySlot;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotStackAccessor;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.function.Supplier;

public class SlotSuppliedHandler extends StackCopySlot {
	private final Supplier<ResourceHandler<ItemResource>> itemHandlerSupplier;
	public final int slot;
	private final IndexModifier<ItemResource> slotModifier;

	public SlotSuppliedHandler(Supplier<ResourceHandler<ItemResource>> itemHandlerSupplier, int slot, int xPosition, int yPosition) {
		this(itemHandlerSupplier, (i, resource, amount) -> {
			if (itemHandlerSupplier.get() instanceof IndexModifier<?> indexModifier) {
				//noinspection unchecked
				((IndexModifier<ItemResource>) indexModifier).set(i, resource, amount);
			} else if (itemHandlerSupplier.get() instanceof ISlotStackAccessor slotStackAccessor) {
				slotStackAccessor.setStackInSlot(i, resource.toStack(amount));
			} else {
				InventoryHelper.set(itemHandlerSupplier.get(), i, resource, amount);
			}
		}, slot, xPosition, yPosition);
	}

	public SlotSuppliedHandler(Supplier<ResourceHandler<ItemResource>> itemHandlerSupplier, IndexModifier<ItemResource> slotModifier, int slot, int xPosition, int yPosition) {
		super(xPosition, yPosition);
		this.slotModifier = slotModifier;
		this.itemHandlerSupplier = itemHandlerSupplier;
		this.slot = slot;
	}

	public ResourceHandler<ItemResource> getResourceHandler() {
		return itemHandlerSupplier.get();
	}

	@Override
	public void onQuickCraft(ItemStack oldStackIn, ItemStack newStackIn) {
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return itemHandlerSupplier.get().isValid(slot, ItemResource.of(stack));
	}
	@Override
	protected ItemStack getStackCopy() {
		return itemHandlerSupplier.get().getResource(slot).toStack(itemHandlerSupplier.get().getAmountAsInt(slot));
	}

	@Override
	protected void setStackCopy(ItemStack stack) {
		slotModifier.set(slot, ItemResource.of(stack), stack.getCount());
	}

	@Override
	public int getMaxStackSize() {
		return itemHandlerSupplier.get().getCapacityAsInt(slot, ItemResource.EMPTY);
	}

	@Override
	public boolean mayPickup(Player player) {
		ItemResource resource = itemHandlerSupplier.get().getResource(slot);
		if (resource.isEmpty()) {
			return false;
		} else {
			try (Transaction tx = Transaction.openRoot()) {
				return itemHandlerSupplier.get().extract(slot, resource, 1, tx) == 1;
			}
		}
	}

	@Override
	public int getSlotIndex() {
		return slot;
	}

	@Override
	public boolean isSameInventory(Slot other) {
		return other instanceof SlotSuppliedHandler rhs && rhs.itemHandlerSupplier.get() == this.itemHandlerSupplier.get();
	}
}
