package net.p3pp3rf1y.sophisticatedcore.upgrades.battery;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.gui.INameableEmptySlot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeWrapper;

import java.util.function.Supplier;

public class BatteryUpgradeContainer extends UpgradeContainerBase<BatteryUpgradeWrapper, BatteryUpgradeContainer> {
	public static final ResourceLocation EMPTY_BATTERY_INPUT_SLOT_BACKGROUND = SophisticatedCore.getRL("container/slot/battery_input");
	public static final ResourceLocation EMPTY_BATTERY_OUTPUT_SLOT_BACKGROUND = SophisticatedCore.getRL("container/slot/battery_output");

	public BatteryUpgradeContainer(Player player, int upgradeContainerId, BatteryUpgradeWrapper upgradeWrapper,
			UpgradeContainerType<BatteryUpgradeWrapper, BatteryUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
		slots.add(new BatteryIOSlot(() -> this.upgradeWrapper.getInventory(), TankUpgradeWrapper.INPUT_SLOT, -100, -100,
				TranslationHelper.INSTANCE.translUpgradeSlotTooltip("battery_input")) {
			@Override
			public int getMaxStackSize(ItemStack stack) {
				return 1;
			}
		}.setBackground(EMPTY_BATTERY_INPUT_SLOT_BACKGROUND));
		slots.add(new BatteryIOSlot(() -> this.upgradeWrapper.getInventory(), TankUpgradeWrapper.OUTPUT_SLOT, -100, -100,
				TranslationHelper.INSTANCE.translUpgradeSlotTooltip("battery_output")) {
			@Override
			public int getMaxStackSize(ItemStack stack) {
				return 1;
			}
		}.setBackground(EMPTY_BATTERY_OUTPUT_SLOT_BACKGROUND));
	}

	@Override
	public void handlePacket(CompoundTag data) {
		// noop
	}

	public int getEnergyStored() {
		return upgradeWrapper.getAmountAsInt();
	}

	public int getMaxEnergyStored() {
		return upgradeWrapper.getCapacityAsInt();
	}

	private static class BatteryIOSlot extends SlotSuppliedHandler implements INameableEmptySlot {
		private final Component emptyTooltip;
		private final int slot;

		public BatteryIOSlot(Supplier<ResourceHandler<ItemResource>> itemHandlerSupplier, int slot, int xPosition, int yPosition, Component emptyTooltip) {
			super(itemHandlerSupplier, slot, xPosition, yPosition);
			this.emptyTooltip = emptyTooltip;
			this.slot = slot;
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
			return super.mayPlace(stack) && isValidEnergyItem(stack, slot == BatteryUpgradeWrapper.OUTPUT_SLOT);
		}

		private boolean isValidEnergyItem(ItemStack stack, boolean isOutput) {
			ItemAccess itemAccess = ItemAccess.forStack(stack);
			return isValidEnergyItem(itemAccess, isOutput);
		}

		private boolean isValidEnergyItem(ItemAccess itemAccess, boolean isOutput) {
			EnergyHandler energyStorage = itemAccess.getCapability(Capabilities.Energy.ITEM);

			if (energyStorage == null) {
				return false;
			}

			if (isOutput) {
				return canReceive(energyStorage);
			} else {
				return canExtract(energyStorage) && energyStorage.getAmountAsLong() > 0;
			}
		}

		private boolean canReceive(EnergyHandler energyHandler) {
			try (Transaction tx = Transaction.openRoot()) {
				return energyHandler.insert(1, tx) > 0;
			}
		}

		private boolean canExtract(EnergyHandler energyHandler) {
			try (Transaction tx = Transaction.openRoot()) {
				return energyHandler.extract(1, tx) > 0;
			}
		}
	}
}
