package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import net.minecraft.world.item.CreativeModeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public class AdvancedCraftingUpgradeItem extends UpgradeItemBase<AdvancedCraftingUpgradeWrapper> {
	private static final UpgradeType<AdvancedCraftingUpgradeWrapper> TYPE = new UpgradeType<>(AdvancedCraftingUpgradeWrapper::new);

	public AdvancedCraftingUpgradeItem(CreativeModeTab itemGroup, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(itemGroup, upgradeTypeLimitConfig);
	}

	@Override
	public UpgradeType<AdvancedCraftingUpgradeWrapper> getType() {
		return TYPE;
	}
}
