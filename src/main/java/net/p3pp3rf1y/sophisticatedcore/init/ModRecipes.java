package net.p3pp3rf1y.sophisticatedcore.init;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.crafting.EnderLinkerClearRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.EnderLinkerEndpointRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.ItemEnabledCondition;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeClearRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;

import java.util.function.Supplier;

public class ModRecipes {
	private ModRecipes() {
	}

	private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER,
			SophisticatedCore.MOD_ID);
	private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS,
			SophisticatedCore.MOD_ID);

	public static final Supplier<RecipeSerializer<UpgradeNextTierRecipe>> UPGRADE_NEXT_TIER_SERIALIZER = RECIPE_SERIALIZERS.register("upgrade_next_tier",
			UpgradeNextTierRecipe.Serializer::new);
	public static final Supplier<CustomRecipe.Serializer<UpgradeClearRecipe>> UPGRADE_CLEAR_SERIALIZER = RECIPE_SERIALIZERS.register("upgrade_clear",
			() -> new CustomRecipe.Serializer<>(UpgradeClearRecipe::new));
	public static final Supplier<CustomRecipe.Serializer<EnderLinkerEndpointRecipe>> ENDER_LINKER_ENDPOINT_SERIALIZER = RECIPE_SERIALIZERS
			.register("ender_linker_endpoint", () -> new CustomRecipe.Serializer<>(EnderLinkerEndpointRecipe::new));
	public static final Supplier<CustomRecipe.Serializer<EnderLinkerClearRecipe>> ENDER_LINKER_CLEAR_SERIALIZER = RECIPE_SERIALIZERS
			.register("ender_linker_clear", () -> new CustomRecipe.Serializer<>(EnderLinkerClearRecipe::new));

	public static void registerHandlers(IEventBus modBus) {
		RECIPE_SERIALIZERS.register(modBus);
		CONDITION_CODECS.register(modBus);
	}

	public static final Supplier<MapCodec<ItemEnabledCondition>> ITEM_ENABLED_CONDITION = CONDITION_CODECS.register("item_enabled",
			() -> ItemEnabledCondition.CODEC);
}
