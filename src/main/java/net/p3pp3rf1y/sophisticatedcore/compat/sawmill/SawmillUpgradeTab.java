package net.p3pp3rf1y.sophisticatedcore.compat.sawmill;

import net.mehvahdjukaar.sawmill.WoodcuttingRecipe;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterRecipeControl;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterUpgradeTab;

public class SawmillUpgradeTab extends BlockConverterUpgradeTab<WoodcuttingRecipe, SawmillRecipeContainer, SawmillUpgradeContainer> {
	public SawmillUpgradeTab(SawmillUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, ButtonDefinition.Toggle<Boolean> shiftClickTargetButton, ButtonDefinition.Toggle<Boolean> refillInputButton) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("sawmill"), TranslationHelper.INSTANCE.translUpgradeTooltip("sawmill"), shiftClickTargetButton, refillInputButton);
	}

	@Override
	protected BlockConverterRecipeControl<WoodcuttingRecipe, SawmillRecipeContainer> createRecipeControl(StorageScreenBase<?> screen, SawmillRecipeContainer recipeContainer, Position position) {
		return new SawmillRecipeControl(screen, recipeContainer, position);
	}
}
