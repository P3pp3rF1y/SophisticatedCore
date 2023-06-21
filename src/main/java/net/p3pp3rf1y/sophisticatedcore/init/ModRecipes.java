package net.p3pp3rf1y.sophisticatedcore.init;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.crafting.ItemEnabledCondition;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeClearRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;

public class ModRecipes {
	private ModRecipes() {}

	private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SophisticatedCore.MOD_ID);
	public static final RegistryObject<RecipeSerializer<?>> UPGRADE_NEXT_TIER_SERIALIZER = RECIPE_SERIALIZERS.register("upgrade_next_tier", UpgradeNextTierRecipe.Serializer::new);
	public static final RegistryObject<SimpleCraftingRecipeSerializer<?>> UPGRADE_CLEAR_SERIALIZER = RECIPE_SERIALIZERS.register("upgrade_clear", () -> new SimpleCraftingRecipeSerializer<>(UpgradeClearRecipe::new));
	public static void registerHandlers(IEventBus modBus) {
		RECIPE_SERIALIZERS.register(modBus);
		modBus.addListener(ModRecipes::registerRecipeCondition);
		MinecraftForge.EVENT_BUS.addListener(ModRecipes::onResourceReload);
	}

	private static void registerRecipeCondition(RegisterEvent event) {
		if (event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_SERIALIZERS)) {
			CraftingHelper.register(ItemEnabledCondition.Serializer.INSTANCE);
		}
	}

	private static void onResourceReload(AddReloadListenerEvent event) {
		UpgradeNextTierRecipe.REGISTERED_RECIPES.clear();
	}
}
