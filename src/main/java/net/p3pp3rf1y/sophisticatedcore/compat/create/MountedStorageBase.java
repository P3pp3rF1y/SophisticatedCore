package net.p3pp3rf1y.sophisticatedcore.compat.create;

import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

//TODO this definitely needs rework once Create updates to 1.21.9+
public abstract class MountedStorageBase extends MountedItemStorage implements SyncedMountedStorage {
	private ItemStack storageStack;

	public MountedStorageBase(MountedItemStorageType<?> type, ItemStack storageStack) {
		super(type);
		this.storageStack = storageStack;
	}

	public ItemStack getStorageStack() {
		return storageStack;
	}

	public void setStorageStack(ItemStack stack) {
		storageStack = stack;
	}

	@Override
	public void afterSync(Contraption contraption, BlockPos localPos) {
		updateWithSyncedStorageStack(storageStack, true);
	}

	@Override
	public void markClean() {
		//noop
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	public abstract void updateWithSyncedStorageStack(ItemStack storageStack, boolean refreshBlockRender);

	public abstract IStorageWrapper getStorageWrapper();

	protected ResourceHandler<ItemResource> getExternalItemHandler() {
		return getStorageWrapper().getInventoryForInputOutput();
	}

	@Override
	public void setStackInSlot(int i, ItemStack itemStack) {
		InventoryHelper.set(getExternalItemHandler(), i, ItemResource.of(itemStack), itemStack.getCount());
	}

	@Override
	public int getSlots() {
		return getExternalItemHandler().size();
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return getExternalItemHandler().getResource(i).toStack(getExternalItemHandler().getAmountAsInt(i));
	}

	@Override
	public ItemStack insertItem(int i, ItemStack itemStack, boolean b) {
		int moved;
		try (Transaction tx = Transaction.openRoot()) {
			moved = getExternalItemHandler().insert(i, ItemResource.of(itemStack), itemStack.getCount(), tx);
			if (!b) {
				tx.commit();
			}
		}

		return moved == itemStack.getCount() ? ItemStack.EMPTY : itemStack.copyWithCount(itemStack.getCount() - moved);
	}

	@Override
	public ItemStack extractItem(int index, int amount, boolean simulate) {
		int extracted;
		ItemResource resource = getExternalItemHandler().getResource(index);
		try (Transaction tx = Transaction.openRoot()) {
			extracted = getExternalItemHandler().extract(index, resource, amount, tx);
			if (!simulate) {
				tx.commit();
			}
		}

		return resource.toStack(extracted);
	}

	@Override
	public int getSlotLimit(int i) {
		return getExternalItemHandler().getCapacityAsInt(i, ItemResource.EMPTY);
	}

	@Override
	public boolean isItemValid(int i, ItemStack itemStack) {
		return getExternalItemHandler().isValid(i, ItemResource.of(itemStack));
	}

	public void onClose(Player player, Vec3 pos) {

	}

	protected void onOpen(ServerLevel level, Vec3 pos) {

	}

	public void onContraptionDestroyed() {

	}
}
