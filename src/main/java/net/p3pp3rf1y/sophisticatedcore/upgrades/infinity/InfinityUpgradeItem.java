package net.p3pp3rf1y.sophisticatedcore.upgrades.infinity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.inventory.IInventoryPartHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryPartRegistry;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class InfinityUpgradeItem extends UpgradeItemBase<InfinityUpgradeItem.Wrapper> {
	public static final List<UpgradeConflictDefinition> UPGRADE_CONFLICT_DEFINITIONS = List.of(new UpgradeConflictDefinition(i -> true, 0,
			TranslationHelper.INSTANCE.translError("add.any_upgrade_exists"), TranslationHelper.INSTANCE.translError("add.no_upgrade_can_be_added")));
	public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);

	static {
		InventoryPartRegistry.registerFactory(InfinityInventoryPart.Admin.NAME,
				(parent, slotRange, getMemorySettings) -> new InfinityInventoryPart.Admin(parent, slotRange));
		InventoryPartRegistry.registerFactory(InfinityInventoryPart.Survival.NAME,
				(parent, slotRange, getMemorySettings) -> new InfinityInventoryPart.Survival(parent, slotRange));
	}

	private final boolean admin;

	public InfinityUpgradeItem(IUpgradeCountLimitConfig upgradeTypeLimitConfig, boolean admin, Properties properties) {
		super(upgradeTypeLimitConfig, properties);
		this.admin = admin;
	}

	@Override
	public UpgradeType<Wrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return UPGRADE_CONFLICT_DEFINITIONS;
	}

	@Override
	public UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storageWrapper, boolean isClientSide, Player player) {
		if (player.hasPermissions(getPermissionLevel())) {
			return super.canRemoveUpgradeFrom(storageWrapper, isClientSide, player);
		}

		return UpgradeSlotChangeResult.fail(TranslationHelper.INSTANCE.translError("remove.infinity_upgrade_only_admin"), Set.of(), Set.of(), Set.of());
	}

	public int getPermissionLevel() {
		return admin ? 2 : 0;
	}

	public IInventoryPartHandler createInventoryPartHandler(InventoryHandler parent, SlotRange slotRange) {
		return admin ? new InfinityInventoryPart.Admin(parent, slotRange) : new InfinityInventoryPart.Survival(parent, slotRange);
	}

	public static class Wrapper extends UpgradeWrapperBase<Wrapper, InfinityUpgradeItem> {

		protected Wrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler);
		}

		@Override
		public boolean canBeDisabled() {
			return false;
		}

		@Override
		public void onAdded() {
			super.onAdded();

			InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
			inventoryHandler.getInventoryPartitioner().addInventoryPart(0, Integer.MAX_VALUE,
					upgradeItem.createInventoryPartHandler(inventoryHandler, new SlotRange(0, inventoryHandler.getSlots())));
			storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged();
		}

		@Override
		public void onBeforeRemoved() {
			super.onBeforeRemoved();
			storageWrapper.getInventoryHandler().getInventoryPartitioner().removeInventoryPart(0);
			save();
		}

		public int getPermissionLevel() {
			return upgradeItem.getPermissionLevel();
		}
	}
}
