package net.p3pp3rf1y.sophisticatedcore.compat.create;

import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SophisticatedMenuProvider;

import java.util.OptionalInt;

public abstract class MountedStorageBase extends MountedItemStorage implements SyncedMountedStorage {

	private ItemStack storageStack;
	private boolean dirty = false;

	public MountedStorageBase(MountedItemStorageType<?> type, ItemStack storageStack) {
		super(type);
		this.storageStack = storageStack;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void markClean() {
		dirty = false;
	}

	protected void setDirty() {
		dirty = true;
	}

	public ItemStack getStorageStack() {
		return storageStack;
	}

	public void setStorageStack(ItemStack stack) {
		storageStack = stack;
	}

	public abstract IStorageWrapper getStorageWrapper();

	protected IItemHandlerModifiable getExternalItemHandler() {
		return getStorageWrapper().getInventoryForInputOutput();
	}

	@Override
	public void setStackInSlot(int i, ItemStack itemStack) {
		getExternalItemHandler().setStackInSlot(i, itemStack);
	}

	@Override
	public int getSlots() {
		return getExternalItemHandler().getSlots();
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return getExternalItemHandler().getStackInSlot(i);
	}

	@Override
	public ItemStack insertItem(int i, ItemStack itemStack, boolean b) {
		return getExternalItemHandler().insertItem(i, itemStack, b);
	}

	@Override
	public ItemStack extractItem(int i, int i1, boolean b) {
		return getExternalItemHandler().extractItem(i, i1, b);
	}

	@Override
	public int getSlotLimit(int i) {
		return getExternalItemHandler().getSlotLimit(i);
	}

	@Override
	public boolean isItemValid(int i, ItemStack itemStack) {
		return getExternalItemHandler().isItemValid(i, itemStack);
	}

	public void onClose(Player player, Vec3 pos) {

	}

	protected void onOpen(ServerLevel level, Vec3 pos) {

	}

	public abstract MountedStorageContainerMenuBase createMenu(int id, Player pl, int contraptionEntityId, BlockPos localPos);

	public OptionalInt openMenu(ServerPlayer player, int contraptionEntityId, BlockPos localPos) {
		return player.openMenu(new SophisticatedMenuProvider((w, p, pl) -> createMenu(w, pl, contraptionEntityId, localPos), getStorageStack().getHoverName(), false),
				buffer -> {
					buffer.writeInt(contraptionEntityId);
					buffer.writeBlockPos(localPos);
				});
	}
}
