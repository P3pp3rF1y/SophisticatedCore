package net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;

import java.util.Optional;

public interface IItemDisplaySettingsPreviewProvider {
	default boolean renderItemDisplaySettingsPreview(ItemDisplaySettingsTab tab, SettingsScreen screen, GuiGraphics guiGraphics, int x, int y, int width, int height,
			ItemDisplaySettingsContainer container, int selectedSlot, float xAxisRotation, float yAxisRotation, float partialTicks) {
		return false;
	}

	default Optional<ItemStack> getItemDisplaySettingsPreviewStack(SettingsScreen screen, ItemDisplaySettingsContainer container, int selectedSlot) {
		return Optional.empty();
	}

	default float getItemDisplayPreviewYAxisRotation(float yAxisRotation) {
		return yAxisRotation;
	}

	default float getItemDisplayPreviewScaleMultiplier() {
		return 1;
	}
}
