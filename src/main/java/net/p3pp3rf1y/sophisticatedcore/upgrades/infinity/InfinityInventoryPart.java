package net.p3pp3rf1y.sophisticatedcore.upgrades.infinity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.inventory.IInventoryPartHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.IResourceExtractor;
import net.p3pp3rf1y.sophisticatedcore.inventory.IResourceInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;

public abstract class InfinityInventoryPart implements IInventoryPartHandler {
	private final InventoryHandler parent;
	private final SlotRange slotRange;
	private final int permissionLevel;
	private final Map<Integer, ItemResource> cachedResources = new HashMap<>();
	private final Map<Integer, ItemStack> cachedStacks = new HashMap<>();

	protected InfinityInventoryPart(InventoryHandler parent, SlotRange slotRange, int permissionLevel) {
		this.parent = parent;
		this.slotRange = slotRange;
		this.permissionLevel = permissionLevel;
		parent.addListener(this::onParentSlotChanged);
	}

	private void onParentSlotChanged(int slot) {
		if (slotRange.isInRange(slot)) {
			cachedResources.remove(slot);
			cachedStacks.remove(slot);
		}
	}

	@Override
	public boolean isInfinite(int slot) {
		return !parent.getInternalStack(slot).isEmpty();
	}

	@Override
	public int getSlotLimit(int slot) {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isValid(int slot, ItemResource resource, @Nullable Player player, BiPredicate<Integer, ItemResource> isValidSuper) {
		return player != null && player.hasPermissions(permissionLevel) && parent.getInternalStack(slot).isEmpty() && isValidSuper.test(slot, resource);
	}

	@Override
	public boolean isSlotAccessible(int slot) {
		return true;
	}

	@Override
	public int getCapacity(int slot, ItemResource resource) {
		return Integer.MAX_VALUE;
	}

	@Override
	public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction, IResourceExtractor extractSuper) {
		return amount;
	}

	@Override
	public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction, IResourceInserter insertSuper) {
		if (!parent.getInternalStack(slot).isEmpty()) {
			return 0;
		}
		cachedResources.remove(slot);
		cachedStacks.remove(slot);
		return insertSuper.insert(slot, resource, amount, transaction);
	}

	@Override
	public void set(int slot, ItemResource resource, int amount, IndexModifier<ItemResource> setSuper) {
		if (parent.getInternalStack(slot).isEmpty()) {
			setSuper.set(slot, resource, amount);
			cachedResources.remove(slot);
			cachedStacks.remove(slot);
		}
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack, BiConsumer<Integer, ItemStack> setStackInSlotInternal) {
		if (parent.getInternalStack(slot).isEmpty()) {
			setStackInSlotInternal.accept(slot, stack);
			cachedResources.remove(slot);
			cachedStacks.remove(slot);
		}
	}

	@Override
	public ItemStack getStackInSlot(int slot, IntFunction<ItemStack> getStackInSlotSuper) {
		if (cachedStacks.containsKey(slot) && cachedStacks.get(slot).isEmpty() != parent.getInternalStack(slot).isEmpty()) {
			cachedStacks.remove(slot);
		}
		return cachedStacks.computeIfAbsent(slot, s -> parent.getInternalStack(s).copyWithCount(Integer.MAX_VALUE));
	}

	@Override
	public ItemResource getResource(int index, IntFunction<ItemResource> getResourceSuper) {
		if (cachedResources.containsKey(index) && cachedResources.get(index).isEmpty() != parent.getInternalStack(index).isEmpty()) {
			cachedResources.remove(index);
		}

		return cachedResources.computeIfAbsent(index, s -> ItemResource.of(parent.getInternalStack(s)));
	}

	@Override
	public long getAmountAsLong(int index, IntFunction<Long> amountAsLongSuper) {
		return Integer.MAX_VALUE;
	}

	@Override
	public int size() {
		return slotRange.size();
	}

	public static class Admin extends InfinityInventoryPart {
		public static final String NAME = "infinity";

		protected Admin(InventoryHandler parent, SlotRange slotRange) {
			super(parent, slotRange, 2);
		}

		@Override
		public String getName() {
			return NAME;
		}
	}

	public static class Survival extends InfinityInventoryPart {
		public static final String NAME = "survival_infinity";

		protected Survival(InventoryHandler parent, SlotRange slotRange) {
			super(parent, slotRange, 0);
		}

		@Override
		public String getName() {
			return NAME;
		}
	}
}
