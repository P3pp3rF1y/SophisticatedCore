package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class ComponentItemStacksHandler extends ItemStacksResourceHandler implements IndexModifier<ItemResource> {
	protected final MutableDataComponentHolder parent;
	protected final DataComponentType<ItemContainerContents> component;

	public ComponentItemStacksHandler(MutableDataComponentHolder parent, DataComponentType<ItemContainerContents> component, int size) {
		super(size);
		this.parent = parent;
		this.component = component;
		getContents().copyInto(stacks);
	}

	protected ItemContainerContents getContents() {
		return parent.getOrDefault(component, ItemContainerContents.EMPTY);
	}

	@Override
	protected void onContentsChanged(int index, ItemStack previousContents) {
		super.onContentsChanged(index, previousContents);
		updateContents(stacks.get(index), index);
	}

	protected void updateContents(ItemStack stack, int slot) {
		stacks.set(slot, stack);
		parent.set(component, ItemContainerContents.fromItems(stacks));
	}

	@Override
	public boolean isValid(int index, ItemResource resource) {
		ItemStack stack = resource.toStack();
		return stack.getItem().canFitInsideContainerItems(stack);
	}

	public ItemStack getStackInSlot(int slot) {
		return stacks.get(slot);
	}
}
