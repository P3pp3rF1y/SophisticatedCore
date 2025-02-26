package net.p3pp3rf1y.sophisticatedcore.compat.sawmill;

import net.mehvahdjukaar.sawmill.SawmillMod;
import net.mehvahdjukaar.sawmill.WoodcuttingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvent;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterRecipeControl;

public class SawmillRecipeControl extends BlockConverterRecipeControl<WoodcuttingRecipe, SawmillRecipeContainer> {
	protected SawmillRecipeControl(StorageScreenBase<?> screen, SawmillRecipeContainer container, Position position) {
		super(screen, container, position, true);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		super.renderBg(guiGraphics, minecraft, mouseX, mouseY);
		if (getInputCount() > 1) {
			guiGraphics.drawString(minecraft.font,  "x" + getInputCount(), x + 52, y + 10, 4210752, false);
		}
	}

	private int getInputCount() {
		if (container.getSelectedRecipe() < 0 || container.getSelectedRecipe() >= container.getRecipeList().size()) {
			return 0;
		}

		return container.getRecipeList().get(container.getSelectedRecipe()).value().getInputCount();
	}

	@Override
	protected SoundEvent getSelectRecipeSound() {
		return SawmillMod.SAWMILL_SELECT.get();
	}
}
