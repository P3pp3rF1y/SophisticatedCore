package net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public abstract class BlockConverterUpgradeTab<R extends SingleItemRecipe, RC extends BlockConverterRecipeContainer<R, ?, RC, C>, C extends BlockConverterUpgradeContainer<R, ?, C, RC>> extends UpgradeSettingsTab<C> {
	private final BlockConverterRecipeControl<R, RC> recipeControl;

	public BlockConverterUpgradeTab(C upgradeContainer, Position position, StorageScreenBase<?> screen, Component tabLabel, Component closedTooltip, ButtonDefinition.Toggle<Boolean> shiftClickTargetButton) {
		super(upgradeContainer, position, screen, tabLabel, closedTooltip);
		addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), shiftClickTargetButton, button -> getContainer().setShiftClickIntoStorage(!getContainer().shouldShiftClickIntoStorage()),
				getContainer()::shouldShiftClickIntoStorage));
		recipeControl = createRecipeControl(screen, upgradeContainer.getRecipeContainer(), new Position(x + 3, y + 24));
		addHideableChild(recipeControl);
	}

	protected abstract BlockConverterRecipeControl<R, RC> createRecipeControl(StorageScreenBase<?> screen, RC recipeContainer, Position position);

	@Override
	protected void moveSlotsToTab() {
		recipeControl.moveSlotsToView();
	}
}
