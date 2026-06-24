package net.p3pp3rf1y.sophisticatedcore.compat.sawmill;

import net.mehvahdjukaar.sawmill.WoodcuttingRecipe;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterUpgradeContainer;

public class SawmillUpgradeContainer
		extends
			BlockConverterUpgradeContainer<WoodcuttingRecipe, SawmillUpgradeItem.Wrapper, SawmillUpgradeContainer, SawmillRecipeContainer> {
	public SawmillUpgradeContainer(Player player, int upgradeContainerId, SawmillUpgradeItem.Wrapper upgradeWrapper,
			UpgradeContainerType<SawmillUpgradeItem.Wrapper, SawmillUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
	}

	@Override
	protected SawmillRecipeContainer createRecipeContainer(ContainerLevelAccess worldPosCallable) {
		return new SawmillRecipeContainer(this, slots::add, this, worldPosCallable, player.level());
	}
}
