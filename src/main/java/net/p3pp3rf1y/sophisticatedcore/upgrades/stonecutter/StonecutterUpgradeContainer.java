package net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterUpgradeContainer;

public class StonecutterUpgradeContainer
		extends
			BlockConverterUpgradeContainer<StonecutterRecipe, StonecutterUpgradeItem.Wrapper, StonecutterUpgradeContainer, StonecutterRecipeContainer> {
	public StonecutterUpgradeContainer(Player player, int upgradeContainerId, StonecutterUpgradeItem.Wrapper upgradeWrapper,
			UpgradeContainerType<StonecutterUpgradeItem.Wrapper, StonecutterUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
	}

	@Override
	protected StonecutterRecipeContainer createRecipeContainer(ContainerLevelAccess worldPosCallable) {
		return new StonecutterRecipeContainer(this, slots::add, this, worldPosCallable, player.level());
	}
}
