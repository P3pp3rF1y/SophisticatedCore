package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotStackAccessor;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class SlotSuppliedHandler extends Slot {
	private static final Container emptyInventory = new SimpleContainer(0);
	private @Nullable ItemStack cachedReturnedStack = null;
	private @Nullable ItemStack lastHandlerStack = null;
	private final Supplier<ResourceHandler<ItemResource>> itemHandlerSupplier;
	public final int slot;

	public SlotSuppliedHandler(Supplier<ResourceHandler<ItemResource>> itemHandlerSupplier, int slot, int xPosition, int yPosition) {
		super(emptyInventory, 0, xPosition, yPosition);
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
	public ItemStack getItem() {
		ItemStack handlerStack = getHandlerStack();
		if (cachedReturnedStack == null || lastHandlerStack == null || !ItemStack.matches(lastHandlerStack, handlerStack)) {
			cachedReturnedStack = handlerStack.copy();
			lastHandlerStack = handlerStack;
		}

		return cachedReturnedStack;
	}

	private ItemStack getHandlerStack() {
		return itemHandlerSupplier.get().getResource(slot).toStack(itemHandlerSupplier.get().getAmountAsInt(slot));
	}

	public void set(ItemStack stack) {
		if (itemHandlerSupplier.get() instanceof ISlotStackAccessor slotStackAccessor) {
			slotStackAccessor.setStackInSlot(slot, stack);
		} else if (itemHandlerSupplier.get() instanceof IndexModifier<?> indexModifier) {
			//noinspection unchecked
			((IndexModifier<ItemResource>) indexModifier).set(slot, ItemResource.of(stack), stack.getCount());
		} else {
			InventoryHelper.set(itemHandlerSupplier.get(), slot, ItemResource.of(stack), stack.getCount());
		}
		this.cachedReturnedStack = stack;
	}

	@Override
	public void setChanged() {
		if (cachedReturnedStack != null && !ItemStack.matches(cachedReturnedStack, getHandlerStack())) {
			set(cachedReturnedStack);
		}
	}

	@Override
	public ItemStack remove(int amount) {
		ItemStack stack = getHandlerStack().copy();
		ItemStack ret = stack.split(amount);
		this.set(stack);
		this.cachedReturnedStack = null;
		return ret;
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
