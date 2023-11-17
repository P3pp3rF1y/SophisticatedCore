package net.p3pp3rf1y.sophisticatedcore.compat.chipped;

import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;

public class BlockTransformationUpgradeTab extends UpgradeSettingsTab<BlockTransformationUpgradeContainer> {
	private final BlockTransformationRecipeControl recipeControl;

	public BlockTransformationUpgradeTab(BlockTransformationUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, ButtonDefinition.Toggle<Boolean> shiftClickTargetButton, String upgradeName) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade(upgradeName), TranslationHelper.INSTANCE.translUpgradeTooltip(upgradeName));
		addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), shiftClickTargetButton, button -> getContainer().setShiftClickIntoStorage(!getContainer().shouldShiftClickIntoStorage()),
				getContainer()::shouldShiftClickIntoStorage));
		recipeControl = new BlockTransformationRecipeControl(screen, upgradeContainer.getRecipeContainer(), new Position(x + 3, y + 24));
		addHideableChild(recipeControl);
	}

	@Override
	protected void moveSlotsToTab() {
		recipeControl.moveSlotsToView();
	}
}
