package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CoreRecipeViewerDisplays;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.IRecipeViewerDisplayCatalog;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.RecipeViewerDisplayCatalog;

@SuppressWarnings("unused")
@JeiPlugin
public class CoreJeiPlugin implements IModPlugin {
	private IRecipeViewerDisplayCatalog catalog = null;

	public CoreJeiPlugin() {
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onRecipesUpdated);
	}

	@Override
	public ResourceLocation getPluginUid() {
		return ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "default");
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		IModPlugin.super.registerGuiHandlers(registration);
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(RecipeTypes.CRAFTING, getCatalog().getCraftingRecipes());
	}

	private IRecipeViewerDisplayCatalog getCatalog() {
		if (catalog == null) {
			catalog = new RecipeViewerDisplayCatalog();
			CoreRecipeViewerDisplays.register(catalog);
		}
		return catalog;
	}

	private void onRecipesUpdated(RecipesUpdatedEvent event) {
		catalog = null;
	}
}
