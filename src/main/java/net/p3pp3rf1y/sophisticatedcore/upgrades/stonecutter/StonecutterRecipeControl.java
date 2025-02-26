package net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterRecipeControl;

public class StonecutterRecipeControl extends BlockConverterRecipeControl<StonecutterRecipe, StonecutterRecipeContainer> {
	protected StonecutterRecipeControl(StorageScreenBase<?> screen, StonecutterRecipeContainer container, Position position) {
		super(screen, container, position, false);
	}

	@Override
	protected SoundEvent getSelectRecipeSound() {
		return SoundEvents.UI_STONECUTTER_SELECT_RECIPE;
	}
}
