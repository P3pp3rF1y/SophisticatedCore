package net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter;

import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterRecipeControl;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterUpgradeTab;

public class StonecutterUpgradeTab extends BlockConverterUpgradeTab<StonecutterRecipe, StonecutterRecipeContainer, StonecutterUpgradeContainer> {
	public StonecutterUpgradeTab(StonecutterUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, ButtonDefinition.Toggle<Boolean> shiftClickTargetButton, ButtonDefinition.Toggle<Boolean> refillInputButton) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("stonecutter"), TranslationHelper.INSTANCE.translUpgradeTooltip("stonecutter"), shiftClickTargetButton, refillInputButton);
	}

	@Override
	protected BlockConverterRecipeControl<StonecutterRecipe, StonecutterRecipeContainer> createRecipeControl(StorageScreenBase<?> screen, StonecutterRecipeContainer recipeContainer, Position position) {
		return new StonecutterRecipeControl(screen, recipeContainer, position);
	}
}
