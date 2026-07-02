package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.gui.INameableEmptySlot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;

import java.util.function.Supplier;

public class TankUpgradeContainer extends UpgradeContainerBase<TankUpgradeWrapper, TankUpgradeContainer> {
	public static final Identifier EMPTY_TANK_INPUT_SLOT_BACKGROUND = SophisticatedCore.getIdentifier("container/slot/tank_input");
	public static final Identifier EMPTY_TANK_OUTPUT_SLOT_BACKGROUND = SophisticatedCore.getIdentifier("container/slot/tank_output");

	public TankUpgradeContainer(Player player, int upgradeContainerId, TankUpgradeWrapper upgradeWrapper,
			UpgradeContainerType<TankUpgradeWrapper, TankUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
		slots.add(new TankIOSlot(supplyFromWrapper(TankUpgradeWrapper::getInventory), TankUpgradeWrapper.INPUT_SLOT, -100, -100,
				TranslationHelper.INSTANCE.translUpgradeSlotTooltip("tank_input")).setBackground(EMPTY_TANK_INPUT_SLOT_BACKGROUND));
		slots.add(new TankIOSlot(supplyFromWrapper(TankUpgradeWrapper::getInventory), TankUpgradeWrapper.OUTPUT_SLOT, -100, -100,
				TranslationHelper.INSTANCE.translUpgradeSlotTooltip("tank_output")).setBackground(EMPTY_TANK_OUTPUT_SLOT_BACKGROUND));
		slots.add(new TakeOnlySlot(supplyFromWrapper(TankUpgradeWrapper::getInventory), TankUpgradeWrapper.INPUT_RESULT_SLOT, -100, -100));
		slots.add(new TakeOnlySlot(supplyFromWrapper(TankUpgradeWrapper::getInventory), TankUpgradeWrapper.OUTPUT_RESULT_SLOT, -100, -100));
	}

	@Override
	public void handlePacket(CompoundTag data) {
		// noop
	}

	public FluidStack getContents() {
		return upgradeWrapper.getContents();
	}

	public int getTankCapacity() {
		return upgradeWrapper.getCapacity();
	}

	private static class TankIOSlot extends SlotSuppliedHandler implements INameableEmptySlot {
		private final Supplier<TankUpgradeWrapper.TankComponentItemHandler> itemHandlerSupplier;
		private final Component emptyTooltip;

		public TankIOSlot(Supplier<TankUpgradeWrapper.TankComponentItemHandler> itemHandlerSupplier, int slot, int xPosition, int yPosition,
				Component emptyTooltip) {
			super(itemHandlerSupplier::get, slot, xPosition, yPosition);
			this.itemHandlerSupplier = itemHandlerSupplier;
			this.emptyTooltip = emptyTooltip;
		}

		@Override
		public boolean hasEmptyTooltip() {
			return true;
		}

		@Override
		public Component getEmptyTooltip() {
			return emptyTooltip;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return itemHandlerSupplier.get().isValid(slot, ItemResource.of(stack));
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return Math.min(stack.getMaxStackSize(), getMaxStackSize());
		}
	}

	private static class TakeOnlySlot extends SlotSuppliedHandler {
		public TakeOnlySlot(Supplier<TankUpgradeWrapper.TankComponentItemHandler> itemHandlerSupplier, int slot, int xPosition, int yPosition) {
			super(itemHandlerSupplier::get, slot, xPosition, yPosition);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}
}
