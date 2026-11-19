package net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StonecutterRecipeContainerTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@AfterEach
	void clearRecipeHelperLevel() {
		RecipeHelper.onRecipesUpdated(null);
	}

	@Test
	void updateAvailableRecipesResetsStateWhenRemovingInputAfterImmutableRecipeList() {
		ItemStackHandler inputInventory = new ItemStackHandler(1);
		StonecutterUpgradeWrapper upgradeWrapper = mock(StonecutterUpgradeWrapper.class);
		when(upgradeWrapper.getInputInventory()).thenReturn(inputInventory);
		when(upgradeWrapper.getRecipeId()).thenReturn(Optional.empty());

		StonecutterUpgradeContainer upgradeContainer = mock(StonecutterUpgradeContainer.class);
		when(upgradeContainer.getUpgradeWrapper()).thenReturn(upgradeWrapper);
		Level level = mock(Level.class);
		RecipeManager recipeManager = mock(RecipeManager.class);
		when(level.getRecipeManager()).thenReturn(recipeManager);
		RegistryAccess registryAccess = mock(RegistryAccess.class);
		when(level.registryAccess()).thenReturn(registryAccess);
		StonecutterRecipe recipe = mock(StonecutterRecipe.class);
		when(recipe.getId()).thenReturn(new ResourceLocation("test", "stonecutting"));
		when(recipe.assemble(any(Container.class), eq(registryAccess))).thenReturn(new ItemStack(Items.DIAMOND));
		when(recipeManager.getRecipesFor(eq(RecipeType.STONECUTTING), any(Container.class), eq(level))).thenReturn(List.of(recipe));
		RecipeHelper.setLevel(level);
		StonecutterRecipeContainer recipeContainer = new StonecutterRecipeContainer(upgradeContainer, slot -> {
		}, mock(IServerUpdater.class), ContainerLevelAccess.NULL, level);

		recipeContainer.getInputSlot().set(new ItemStack(Items.STONE));
		recipeContainer.selectRecipe(0);
		recipeContainer.getInputSlot().remove(1);

		assertTrue(recipeContainer.getRecipeList().isEmpty());
		assertEquals(-1, recipeContainer.getSelectedRecipe());
		assertTrue(recipeContainer.getOutputSlot().getItem().isEmpty());
	}
}
