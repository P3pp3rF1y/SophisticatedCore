package net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterRecipeContainer;

import java.util.List;
import java.util.function.Consumer;

public class StonecutterRecipeContainer extends BlockConverterRecipeContainer<StonecutterRecipe, StonecutterUpgradeItem.Wrapper, StonecutterRecipeContainer, StonecutterUpgradeContainer> {
	public StonecutterRecipeContainer(StonecutterUpgradeContainer upgradeContainer, Consumer<Slot> addSlot, IServerUpdater serverUpdater, ContainerLevelAccess worldPosCallable, Level level) {
		super(upgradeContainer, addSlot, serverUpdater, worldPosCallable, level, SoundEvents.UI_STONECUTTER_TAKE_RESULT);
	}

	@Override
	protected RecipeType<StonecutterRecipe> getRecipeType() {
		return RecipeType.STONECUTTING;
	}

	@Override
	protected List<RecipeHolder<StonecutterRecipe>> filterAndSortRecipes(List<RecipeHolder<StonecutterRecipe>> recipes) {
		return recipes;
	}

	@Override
	protected int getInputCount() {
		return 1;
	}
}
