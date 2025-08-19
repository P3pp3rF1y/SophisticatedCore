/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.google.common.base.Preconditions;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class StatefulComponentItemHandler implements IItemHandlerModifiable, ISlotChangeListener {
	protected NonNullList<ItemStack> stacks;
	protected final MutableDataComponentHolder parent;
	protected final DataComponentType<ItemContainerContents> component;
	protected final int size;

	public StatefulComponentItemHandler(MutableDataComponentHolder parent, DataComponentType<ItemContainerContents> component, int size) {
		this.parent = parent;
		this.component = component;
		this.size = size;
		Preconditions.checkArgument(size <= 256, "The max size of ItemContainerContents is 256 slots.");
		fillStacks();
	}

	private void fillStacks() {
		ItemContainerContents contents = getContents();
		stacks = NonNullList.withSize(size, ItemStack.EMPTY);
		contents.copyInto(stacks);
	}

	@Override
	public int getSlots() {
		return size;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		validateSlotIndex(slot);
		return stacks.get(slot);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		validateSlotIndex(slot);
		if (!isItemValid(slot, stack)) {
			throw new RuntimeException("Invalid stack " + stack + " for slot " + slot + ")");
		}
		ItemStack existing = stacks.get(slot);
		if (!ItemStack.matches(stack, existing)) {
			updateContents(stack, slot);
		}
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack toInsert, boolean simulate) {
		validateSlotIndex(slot);

		if (toInsert.isEmpty()) {
			return ItemStack.EMPTY;
		}

		if (!isItemValid(slot, toInsert)) {
			return toInsert;
		}

		ItemStack existing = stacks.get(slot);

		int insertLimit = Math.min(getSlotLimit(slot), toInsert.getMaxStackSize());

		if (!existing.isEmpty()) {
			if (!ItemStack.isSameItemSameComponents(toInsert, existing)) {
				return toInsert;
			}

			insertLimit -= existing.getCount();
		}

		if (insertLimit <= 0) {
			return toInsert;
		}

		int inserted = Math.min(insertLimit, toInsert.getCount());

		if (!simulate) {
			updateContents(toInsert.copyWithCount(existing.getCount() + inserted), slot);
		}

		return toInsert.copyWithCount(toInsert.getCount() - inserted);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		validateSlotIndex(slot);

		if (amount == 0) {
			return ItemStack.EMPTY;
		}

		ItemStack existing = stacks.get(slot);

		if (existing.isEmpty()) {
			return ItemStack.EMPTY;
		}

		int toExtract = Math.min(amount, existing.getMaxStackSize());

		if (!simulate) {
			updateContents(existing.copyWithCount(existing.getCount() - toExtract), slot);
		}

		return existing.copyWithCount(toExtract);
	}

	@Override
	public int getSlotLimit(int slot) {
		return Item.ABSOLUTE_MAX_STACK_SIZE;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return stack.getItem().canFitInsideContainerItems();
	}

	protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {
	}

	protected ItemContainerContents getContents() {
		return parent.getOrDefault(component, ItemContainerContents.EMPTY);
	}

	protected void updateContents(ItemStack stack, int slot) {
		validateSlotIndex(slot);
		ItemStack oldStack = stacks.get(slot);
		stacks.set(slot, stack);
		parent.set(component, ItemContainerContents.fromItems(stacks));
		onContentsChanged(slot, oldStack, stack);
	}

	protected final void validateSlotIndex(int slot) {
		if (slot < 0 || slot >= getSlots()) {
			throw new RuntimeException("Slot " + slot + " not in valid range - [0," + getSlots() + ")");
		}
	}

	@Override
	public void onSlotChanged(int slot) {
		updateContents(getStackInSlot(slot), slot);
	}
}
