package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicControl;

public abstract class AutoCookingUpgradeTab<R extends AbstractCookingRecipe, W extends AutoCookingUpgradeWrapper<W, ?, R>>
		extends
			UpgradeSettingsTab<AutoCookingUpgradeContainer<R, W>> {
	private final FilterLogicControl<FilterLogic, FilterLogicContainer<FilterLogic>> inputFilterLogicControl;
	private final FilterLogicControl<FilterLogic, FilterLogicContainer<FilterLogic>> fuelFilterLogicControl;
	private final CookingLogicControl<R> cookingLogicControl;

	protected AutoCookingUpgradeTab(AutoCookingUpgradeContainer<R, W> upgradeContainer, Position position, StorageScreenBase<?> screen, String tabLabel,
			String closedTooltip, int inputFilterSlotsPerRow, int fuelFilterSlotsPerRow) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade(tabLabel),
				TranslationHelper.INSTANCE.translUpgradeTooltip(closedTooltip));
		int controlsX = x + 3;
		int inputFilterY = y + 24;
		inputFilterLogicControl = addHideableChild(new FilterLogicControl.Advanced(screen, new Position(controlsX, inputFilterY),
				getContainer().getInputFilterLogicContainer(), inputFilterSlotsPerRow));
		int cookingY = inputFilterY + inputFilterLogicControl.getHeight() + 3;
		cookingLogicControl = addHideableChild(new CookingLogicControl<>(new Position(controlsX, cookingY), getContainer().getCookingLogicContainer()));
		fuelFilterLogicControl = addHideableChild(new FilterLogicControl<>(screen, new Position(controlsX, cookingY + cookingLogicControl.getHeight() + 4),
				getContainer().getFuelFilterLogicContainer(), fuelFilterSlotsPerRow));
	}

	@Override
	protected void moveSlotsToTab() {
		inputFilterLogicControl.moveSlotsToView();
		cookingLogicControl.moveSlotsToView(screen.getGuiLeft(), screen.getGuiTop());
		fuelFilterLogicControl.moveSlotsToView();
	}

	public static class AutoSmeltingUpgradeTab extends AutoCookingUpgradeTab<SmeltingRecipe, AutoCookingUpgradeWrapper.AutoSmeltingUpgradeWrapper> {
		public AutoSmeltingUpgradeTab(AutoCookingUpgradeContainer<SmeltingRecipe, AutoCookingUpgradeWrapper.AutoSmeltingUpgradeWrapper> upgradeContainer,
				Position position, StorageScreenBase<?> screen, int inputFilterSlotsPerRow, int fuelFilterSlotsPerRow) {
			super(upgradeContainer, position, screen, "auto_smelting", "auto_smelting", inputFilterSlotsPerRow, fuelFilterSlotsPerRow);
		}
	}

	public static class AutoSmokingUpgradeTab extends AutoCookingUpgradeTab<SmokingRecipe, AutoCookingUpgradeWrapper.AutoSmokingUpgradeWrapper> {
		public AutoSmokingUpgradeTab(AutoCookingUpgradeContainer<SmokingRecipe, AutoCookingUpgradeWrapper.AutoSmokingUpgradeWrapper> upgradeContainer,
				Position position, StorageScreenBase<?> screen, int inputFilterSlotsPerRow, int fuelFilterSlotsPerRow) {
			super(upgradeContainer, position, screen, "auto_smoking", "auto_smoking", inputFilterSlotsPerRow, fuelFilterSlotsPerRow);
		}
	}

	public static class AutoBlastingUpgradeTab extends AutoCookingUpgradeTab<BlastingRecipe, AutoCookingUpgradeWrapper.AutoBlastingUpgradeWrapper> {
		public AutoBlastingUpgradeTab(AutoCookingUpgradeContainer<BlastingRecipe, AutoCookingUpgradeWrapper.AutoBlastingUpgradeWrapper> upgradeContainer,
				Position position, StorageScreenBase<?> screen, int inputFilterSlotsPerRow, int fuelFilterSlotsPerRow) {
			super(upgradeContainer, position, screen, "auto_blasting", "auto_blasting", inputFilterSlotsPerRow, fuelFilterSlotsPerRow);
		}
	}
}
