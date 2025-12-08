package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CraftingItemHandler extends TransientCraftingContainer {
	private final Supplier<ResourceHandler<ItemResource>> supplyInventory;
	private final Consumer<Container> onCraftingMatrixChanged;
	private boolean itemsInitialized = false;
	private List<ItemStack> items = List.of();

	public CraftingItemHandler(Supplier<ResourceHandler<ItemResource>> supplyInventory, Consumer<Container> onCraftingMatrixChanged) {
		super(new AbstractContainerMenu(null, -1) {
			@Override
			public ItemStack quickMoveStack(Player player, int index) {
				return ItemStack.EMPTY;
			}

			@Override
			public boolean stillValid(Player playerIn) {
				return false;
			}
		}, 3, 3);
		this.supplyInventory = supplyInventory;
		this.onCraftingMatrixChanged = onCraftingMatrixChanged;
	}

	@Override
	public int getContainerSize() {
		return supplyInventory.get().size();
	}

	@Override
	public boolean isEmpty() {
		return ResourceHandlerUtil.isEmpty(supplyInventory.get());
	}

	@Override
	public ItemStack getItem(int index) {
		ResourceHandler<ItemResource> itemHandler = supplyInventory.get();
		return index >= itemHandler.size() ? ItemStack.EMPTY : itemHandler.getResource(index).toStack(itemHandler.getAmountAsInt(index));
	}

	@Override
	public List<ItemStack> getItems() {
		if (!itemsInitialized) {
			items = InventoryHelper.getStacks(supplyInventory.get());
			itemsInitialized = true;
		}
		return items;
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		return InventoryHelper.getAndRemove(supplyInventory.get(), index);
	}

	@Override
	public ItemStack removeItem(int index, int count) {
		ResourceHandler<ItemResource> inventory = supplyInventory.get();
		ItemResource resource = inventory.getResource(index);
		int extracted = InventoryHelper.extract(supplyInventory.get(), index, resource, count);
		if (extracted > 0) {
			itemsInitialized = false;
			onCraftingMatrixChanged.accept(this);
		}

		return resource.toStack(extracted);
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		InventoryHelper.set(supplyInventory.get(), index, ItemResource.of(stack), stack.getCount());
		onCraftingMatrixChanged.accept(this);
		itemsInitialized = false;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		itemsInitialized = false;
	}

	@Override
	public void fillStackedContents(StackedItemContents helper) {
		InventoryHelper.iterate(supplyInventory.get(), (slot, resource, amount) -> helper.accountSimpleStack(resource.toStack(amount)));
	}

}
