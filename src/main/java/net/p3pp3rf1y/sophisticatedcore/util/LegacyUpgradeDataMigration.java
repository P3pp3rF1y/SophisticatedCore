package net.p3pp3rf1y.sophisticatedcore.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.EntityMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterAttributes;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyCondition;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyFilterAttribute;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.battery.BatteryUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.compacting.CompactingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.IAutoCookingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.ICookingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.HungerLevel;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.RepeatMode;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.AutomationDirection;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class LegacyUpgradeDataMigration {
	private static final String ENABLED = "enabled";
	private static final List<String> FILTER_KEYS = List.of("tags", "isAllowList", "matchDurability", "matchNbt", "primaryMatch", "matchAnyTag", "filters",
			"filterByStorage", "filterByInventory");
	private static final Map<Class<?>, LegacyUpgradeDataMigrator> CORE_MIGRATORS = new LinkedHashMap<>();
	private static final Map<Class<? extends Item>, LegacyUpgradeDataMigrator> MODULE_MIGRATORS = new ConcurrentHashMap<>();

	static {
		CORE_MIGRATORS.put(CompactingUpgradeItem.class, LegacyUpgradeDataMigration::migrateCompactingData);
		CORE_MIGRATORS.put(FeedingUpgradeItem.class, LegacyUpgradeDataMigration::migrateFeedingData);
		CORE_MIGRATORS.put(FilterUpgradeItem.class, LegacyUpgradeDataMigration::migrateFilterData);
		CORE_MIGRATORS.put(PickupUpgradeItem.class, LegacyUpgradeDataMigration::migratePickupData);
		CORE_MIGRATORS.put(MagnetUpgradeItem.class, LegacyUpgradeDataMigration::migrateMagnetData);
		CORE_MIGRATORS.put(VoidUpgradeItem.class, LegacyUpgradeDataMigration::migrateVoidData);
		CORE_MIGRATORS.put(BatteryUpgradeItem.class, LegacyUpgradeDataMigration::migrateBatteryData);
		CORE_MIGRATORS.put(CraftingUpgradeItem.class, LegacyUpgradeDataMigration::migrateCraftingData);
		CORE_MIGRATORS.put(BlockConverterUpgradeItem.class, LegacyUpgradeDataMigration::migrateBlockConverterData);
		CORE_MIGRATORS.put(JukeboxUpgradeItem.class, LegacyUpgradeDataMigration::migrateJukeboxData);
		CORE_MIGRATORS.put(IAutoCookingUpgradeItem.class, LegacyUpgradeDataMigration::migrateAutoCookingData);
		CORE_MIGRATORS.put(ICookingUpgradeItem.class, LegacyUpgradeDataMigration::migrateCookingData);
		CORE_MIGRATORS.put(XpPumpUpgradeItem.class, LegacyUpgradeDataMigration::migrateXpPumpData);
		CORE_MIGRATORS.put(TankUpgradeItem.class, LegacyUpgradeDataMigration::migrateTankData);
		CORE_MIGRATORS.put(PumpUpgradeItem.class, LegacyUpgradeDataMigration::migratePumpData);
		CORE_MIGRATORS.put(AlchemyUpgradeItem.class, LegacyUpgradeDataMigration::migrateAlchemyData);
	}

	private LegacyUpgradeDataMigration() {
	}

	public static void registerMigrator(Class<? extends Item> upgradeItemClass, LegacyUpgradeDataMigrator migrator) {
		MODULE_MIGRATORS.put(upgradeItemClass, migrator);
	}

	public static void registerBlockTransformationMigrator(Class<? extends Item> upgradeItemClass) {
		registerMigrator(upgradeItemClass, LegacyUpgradeDataMigration::migrateBlockTransformationData);
	}

	public static boolean migrate(ItemStack upgradeStack, RegistryAccess registryAccess) {
		CompoundTag legacyData = upgradeStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		CompoundTag originalLegacyData = legacyData.copy();

		boolean coreMigrated = migrateCoreData(upgradeStack, legacyData, registryAccess);
		LegacyUpgradeDataMigrator migrator = MODULE_MIGRATORS.get(upgradeStack.getItem().getClass());
		boolean moduleMigrated = migrator != null && migrator.migrate(upgradeStack, legacyData, registryAccess);
		boolean legacyDataChanged = !legacyData.equals(originalLegacyData);
		if (legacyDataChanged) {
			CustomData.set(DataComponents.CUSTOM_DATA, upgradeStack, legacyData);
		}
		return coreMigrated || moduleMigrated || legacyDataChanged;
	}

	@FunctionalInterface
	public interface LegacyUpgradeDataMigrator {
		boolean migrate(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess);
	}

	private static boolean migrateCoreData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateComponent(upgradeStack, legacyData, ENABLED, ModCoreDataComponents.ENABLED.get(), LegacyUpgradeDataMigration::parseBoolean);
		LegacyUpgradeDataMigrator migrator = getCoreMigrator(upgradeStack.getItem());
		if (migrator != null) {
			migrated |= migrator.migrate(upgradeStack, legacyData, registryAccess);
		}
		return migrated;
	}

	static LegacyUpgradeDataMigrator getCoreMigrator(Item upgradeItem) {
		return getCoreMigrator(upgradeItem.getClass());
	}

	static LegacyUpgradeDataMigrator getCoreMigrator(Class<?> upgradeItemClass) {
		for (Map.Entry<Class<?>, LegacyUpgradeDataMigrator> entry : CORE_MIGRATORS.entrySet()) {
			if (entry.getKey().isAssignableFrom(upgradeItemClass)) {
				return entry.getValue();
			}
		}
		return null;
	}

	private static boolean migrateCompactingData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateRootFilterAttributes(upgradeStack, legacyData, registryAccess);
		migrated |= migrateComponent(upgradeStack, legacyData, "compactNonUncraftable", ModCoreDataComponents.COMPACT_NON_UNCRAFTABLE.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		migrated |= migrateComponent(upgradeStack, legacyData, "shouldWorkInGUI", ModCoreDataComponents.SHOULD_WORK_IN_GUI.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		return migrated;
	}

	private static boolean migrateFeedingData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateRootFilterAttributes(upgradeStack, legacyData, registryAccess);
		migrated |= migrateComponent(upgradeStack, legacyData, "feedAtHungerLevel", ModCoreDataComponents.FEED_AT_HUNGER_LEVEL.get(),
				LegacyUpgradeDataMigration::parseHungerLevel);
		migrated |= migrateComponent(upgradeStack, legacyData, "feedImmediatelyWhenHurt", ModCoreDataComponents.FEED_IMMEDIATELY_WHEN_HURT.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		return migrated;
	}

	private static boolean migrateFilterData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateRootFilterAttributes(upgradeStack, legacyData, registryAccess);
		migrated |= migrateComponent(upgradeStack, legacyData, "direction", ModCoreDataComponents.DIRECTION.get(), LegacyUpgradeDataMigration::parseDirection);
		return migrated;
	}

	private static boolean migratePickupData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		return migrateRootFilterAttributes(upgradeStack, legacyData, registryAccess);
	}

	private static boolean migrateMagnetData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateRootFilterAttributes(upgradeStack, legacyData, registryAccess);
		migrated |= migrateComponent(upgradeStack, legacyData, "pickupItems", ModCoreDataComponents.PICKUP_ITEMS.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		migrated |= migrateComponent(upgradeStack, legacyData, "pickupXp", ModCoreDataComponents.PICKUP_XP.get(), LegacyUpgradeDataMigration::parseBoolean);
		return migrated;
	}

	private static boolean migrateVoidData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateRootFilterAttributes(upgradeStack, legacyData, registryAccess, true);
		migrated |= migrateVoidOverflow(upgradeStack, legacyData);
		migrated |= migrateComponent(upgradeStack, legacyData, "shouldWorkInGUI", ModCoreDataComponents.SHOULD_WORK_IN_GUI.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		migrated |= migrateComponent(upgradeStack, legacyData, "fluidFilters", ModCoreDataComponents.FLUID_FILTERS.get(),
				tag -> parseFluidFilters(tag, registryAccess));
		return migrated;
	}

	private static boolean migrateBatteryData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateComponent(upgradeStack, legacyData, "energyStored", ModCoreDataComponents.ENERGY_STORED.get(),
				LegacyUpgradeDataMigration::parseInteger);
		migrated |= migrateInventory(upgradeStack, legacyData, "inventory", registryAccess);
		return migrated;
	}

	private static boolean migrateCraftingData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateInventory(upgradeStack, legacyData, "craftingInventory", registryAccess);
		migrated |= migrateComponent(upgradeStack, legacyData, "shiftClickIntoStorage", ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		migrated |= migrateComponent(upgradeStack, legacyData, "refill_crafting_grid", ModCoreDataComponents.REFILL_CRAFTING_GRID.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		return migrated;
	}

	private static boolean migrateBlockConverterData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateInputItem(upgradeStack, legacyData, registryAccess);
		migrated |= migrateComponent(upgradeStack, legacyData, "recipeId", ModCoreDataComponents.RECIPE_ID.get(),
				LegacyUpgradeDataMigration::parseResourceLocation);
		migrated |= migrateComponent(upgradeStack, legacyData, "shiftClickIntoStorage", ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		migrated |= migrateComponent(upgradeStack, legacyData, "refill_input", ModCoreDataComponents.REFILL_INPUT.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		return migrated;
	}

	private static boolean migrateBlockTransformationData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateInputItem(upgradeStack, legacyData, registryAccess);
		migrated |= migrateComponent(upgradeStack, legacyData, "result", ModCoreDataComponents.RESULT_ITEM.get(),
				tag -> parseSingleItemContent(tag, registryAccess));
		migrated |= migrateComponent(upgradeStack, legacyData, "shiftClickIntoStorage", ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		migrated |= migrateComponent(upgradeStack, legacyData, "refill_input", ModCoreDataComponents.REFILL_INPUT.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		return migrated;
	}

	private static boolean migrateJukeboxData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateInventory(upgradeStack, legacyData, "discInventory", registryAccess);
		migrated |= migrateComponent(upgradeStack, legacyData, "isPlaying", ModCoreDataComponents.IS_PLAYING.get(), LegacyUpgradeDataMigration::parseBoolean);
		migrated |= migrateComponent(upgradeStack, legacyData, "shuffle", ModCoreDataComponents.SHUFFLE.get(), LegacyUpgradeDataMigration::parseBoolean);
		migrated |= migrateComponent(upgradeStack, legacyData, "repeatMode", ModCoreDataComponents.REPEAT_MODE.get(),
				LegacyUpgradeDataMigration::parseRepeatMode);
		migrated |= migrateComponent(upgradeStack, legacyData, "discSlotActive", ModCoreDataComponents.DISC_SLOT_ACTIVE.get(),
				LegacyUpgradeDataMigration::parseInteger);
		migrated |= migrateComponent(upgradeStack, legacyData, "discFinishTime", ModCoreDataComponents.DISC_FINISH_TIME.get(),
				LegacyUpgradeDataMigration::parseLong);
		return migrated;
	}

	private static boolean migrateAutoCookingData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateCookingData(upgradeStack, legacyData, registryAccess);
		migrated |= migrateComponent(upgradeStack, legacyData, "inputFilter", ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES.get(),
				tag -> parseFilterAttributes(tag, registryAccess, true));
		migrated |= migrateComponent(upgradeStack, legacyData, "fuelFilter", ModCoreDataComponents.FUEL_FILTER_ATTRIBUTES.get(),
				tag -> parseFilterAttributes(tag, registryAccess, true));
		return migrated;
	}

	private static boolean migrateCookingData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateCookingInventory(upgradeStack, legacyData, registryAccess);
		migrated |= migrateComponent(upgradeStack, legacyData, "storedXp", ModCoreDataComponents.STORED_XP.get(), LegacyUpgradeDataMigration::parseFloat);
		migrated |= migrateComponent(upgradeStack, legacyData, "recipesUsed", ModCoreDataComponents.RECIPES_USED.get(),
				LegacyUpgradeDataMigration::parseRecipesUsed);
		migrated |= migrateComponent(upgradeStack, legacyData, "burnTimeFinish", ModCoreDataComponents.BURN_TIME_FINISH.get(),
				LegacyUpgradeDataMigration::parseLong);
		migrated |= migrateComponent(upgradeStack, legacyData, "burnTimeTotal", ModCoreDataComponents.BURN_TIME_TOTAL.get(),
				LegacyUpgradeDataMigration::parseInteger);
		migrated |= migrateComponent(upgradeStack, legacyData, "cookTimeFinish", ModCoreDataComponents.COOK_TIME_FINISH.get(),
				LegacyUpgradeDataMigration::parseLong);
		migrated |= migrateComponent(upgradeStack, legacyData, "cookTimeTotal", ModCoreDataComponents.COOK_TIME_TOTAL.get(),
				LegacyUpgradeDataMigration::parseInteger);
		migrated |= migrateComponent(upgradeStack, legacyData, "isCooking", ModCoreDataComponents.IS_COOKING.get(), LegacyUpgradeDataMigration::parseBoolean);
		return migrated;
	}

	private static boolean migrateXpPumpData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateComponent(upgradeStack, legacyData, "direction", ModCoreDataComponents.AUTOMATION_DIRECTION.get(),
				LegacyUpgradeDataMigration::parseAutomationDirection);
		migrated |= migrateComponent(upgradeStack, legacyData, "level", ModCoreDataComponents.LEVEL.get(), LegacyUpgradeDataMigration::parseInteger);
		migrated |= migrateComponent(upgradeStack, legacyData, "levelsToStore", ModCoreDataComponents.LEVELS_TO_STORE.get(),
				LegacyUpgradeDataMigration::parseInteger);
		migrated |= migrateComponent(upgradeStack, legacyData, "levelsToTake", ModCoreDataComponents.LEVELS_TO_TAKE.get(),
				LegacyUpgradeDataMigration::parseInteger);
		migrated |= migrateComponent(upgradeStack, legacyData, "mendItems", ModCoreDataComponents.MEND_ITEMS.get(), LegacyUpgradeDataMigration::parseBoolean);
		return migrated;
	}

	private static boolean migrateTankData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateInventory(upgradeStack, legacyData, "inventory", registryAccess);
		migrated |= migrateComponent(upgradeStack, legacyData, "contents", ModCoreDataComponents.FLUID_CONTENTS.get(),
				tag -> parseFluidContent(tag, registryAccess));
		return migrated;
	}

	private static boolean migratePumpData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateComponent(upgradeStack, legacyData, "fluidFilters", ModCoreDataComponents.FLUID_FILTERS.get(),
				tag -> parseFluidFilters(tag, registryAccess));
		migrated |= migrateComponent(upgradeStack, legacyData, "input", ModCoreDataComponents.IS_INPUT.get(), LegacyUpgradeDataMigration::parseBoolean);
		migrated |= migrateComponent(upgradeStack, legacyData, "interactWithHand", ModCoreDataComponents.INTERACT_WITH_HAND.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		migrated |= migrateComponent(upgradeStack, legacyData, "interactWithWorld", ModCoreDataComponents.INTERACT_WITH_WORLD.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		migrated |= migrateComponent(upgradeStack, legacyData, "interactWithFluidHandlers", ModCoreDataComponents.INTERACT_WITH_FLUID_HANDLERS.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		return migrated;
	}

	private static boolean migrateAlchemyData(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		boolean migrated = migrateComponent(upgradeStack, legacyData, "alchemyFilterAttributes", ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES.get(),
				tag -> parseAlchemyFilterAttributes(tag, registryAccess));
		migrated |= migrateComponent(upgradeStack, legacyData, "matchAllEffects", ModCoreDataComponents.MATCH_ALL_EFFECTS.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		migrated |= migrateComponent(upgradeStack, legacyData, "matchEffectDuration", ModCoreDataComponents.MATCH_EFFECT_DURATION.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		migrated |= migrateComponent(upgradeStack, legacyData, "matchEffectAmplifier", ModCoreDataComponents.MATCH_EFFECT_AMPLIFIER.get(),
				LegacyUpgradeDataMigration::parseBoolean);
		migrated |= migrateComponent(upgradeStack, legacyData, "entityMatch", ModCoreDataComponents.ENTITY_MATCH.get(),
				LegacyUpgradeDataMigration::parseEntityMatch);
		return migrated;
	}

	static boolean migrateCookingInventory(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		if (upgradeStack.has(ModCoreDataComponents.COOKING_INVENTORY.get())) {
			boolean removed = legacyData.contains("smeltingInventory") || legacyData.contains("cookingInventory");
			legacyData.remove("smeltingInventory");
			legacyData.remove("cookingInventory");
			return removed;
		}
		Tag smeltingInventory = legacyData.get("smeltingInventory");
		if (smeltingInventory == null) {
			return migrateComponent(upgradeStack, legacyData, "cookingInventory", ModCoreDataComponents.COOKING_INVENTORY.get(),
					tag -> parseItemStackHandler(tag, registryAccess));
		}
		Optional<ItemContainerContents> contents = parseItemStackHandler(smeltingInventory, registryAccess);
		if (contents.isEmpty()) {
			return false;
		}
		upgradeStack.set(ModCoreDataComponents.COOKING_INVENTORY.get(), contents.get());
		legacyData.remove("smeltingInventory");
		legacyData.remove("cookingInventory");
		return true;
	}

	public static boolean migrateInventory(ItemStack upgradeStack, CompoundTag legacyData, String legacyKey, RegistryAccess registryAccess) {
		return migrateComponent(upgradeStack, legacyData, legacyKey, ModCoreDataComponents.LENIENT_CONTAINER.get(),
				tag -> parseItemStackHandler(tag, registryAccess));
	}

	static boolean migrateInputItem(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		return migrateComponent(upgradeStack, legacyData, "input", ModCoreDataComponents.INPUT_ITEM.get(), tag -> parseSingleItemContent(tag, registryAccess));
	}

	public static <T> boolean migrateComponent(ItemStack upgradeStack, CompoundTag legacyData, String legacyKey, DataComponentType<T> component,
			Function<Tag, Optional<T>> parser) {
		Tag legacyValue = legacyData.get(legacyKey);
		if (legacyValue == null) {
			return false;
		}
		if (upgradeStack.has(component)) {
			legacyData.remove(legacyKey);
			return true;
		}
		Optional<T> value = parser.apply(legacyValue);
		if (value.isEmpty()) {
			return false;
		}
		upgradeStack.set(component, value.get());
		legacyData.remove(legacyKey);
		return true;
	}

	public static boolean migrateRootFilterAttributes(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess) {
		return migrateRootFilterAttributes(upgradeStack, legacyData, registryAccess, false);
	}

	public static boolean migrateRootFilterAttributes(ItemStack upgradeStack, CompoundTag legacyData, RegistryAccess registryAccess,
			boolean defaultIsAllowList) {
		CompoundTag filterData = new CompoundTag();
		for (String key : FILTER_KEYS) {
			Tag value = legacyData.get(key);
			if (value != null) {
				filterData.put(key, value.copy());
			}
		}
		if (filterData.isEmpty()) {
			return false;
		}
		if (upgradeStack.has(ModCoreDataComponents.FILTER_ATTRIBUTES.get())) {
			FILTER_KEYS.forEach(legacyData::remove);
			return true;
		}
		Optional<FilterAttributes> filterAttributes = parseFilterAttributes(filterData, registryAccess, defaultIsAllowList);
		if (filterAttributes.isEmpty()) {
			return false;
		}
		upgradeStack.set(ModCoreDataComponents.FILTER_ATTRIBUTES.get(), filterAttributes.get());
		FILTER_KEYS.forEach(legacyData::remove);
		return true;
	}

	static boolean migrateVoidOverflow(ItemStack upgradeStack, CompoundTag legacyData) {
		Tag legacyOverflow = legacyData.get("shouldVoidOverflow");
		Tag legacyVoidType = legacyData.get("voidType");
		if (legacyOverflow == null && legacyVoidType == null) {
			return false;
		}
		if (upgradeStack.has(ModCoreDataComponents.VOID_TYPE.get())) {
			legacyData.remove("shouldVoidOverflow");
			legacyData.remove("voidType");
			return true;
		}
		Optional<Boolean> shouldVoidOverflow = legacyOverflow == null ? Optional.empty() : parseBoolean(legacyOverflow);
		if (shouldVoidOverflow.isPresent()) {
			upgradeStack.set(ModCoreDataComponents.VOID_TYPE.get(), shouldVoidOverflow.get() ? VoidType.ALWAYS : VoidType.SLOT_OVERFLOW);
		} else {
			Optional<VoidType> voidType = legacyVoidType == null ? Optional.empty() : parseVoidType(legacyVoidType);
			if (voidType.isEmpty()) {
				return false;
			}
			upgradeStack.set(ModCoreDataComponents.VOID_TYPE.get(), voidType.get());
		}
		legacyData.remove("shouldVoidOverflow");
		legacyData.remove("voidType");
		return true;
	}

	public static Optional<ItemContainerContents> parseItemStackHandler(Tag tag, RegistryAccess registryAccess) {
		if (!(tag instanceof CompoundTag inventory) || !inventory.contains("Size", Tag.TAG_INT) || !inventory.contains("Items", Tag.TAG_LIST)) {
			return Optional.empty();
		}
		int size = inventory.getInt("Size");
		if (size < 0 || size > 256) {
			return Optional.empty();
		}
		ListTag itemsTag = (ListTag) inventory.get("Items");
		NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
		Set<Integer> occupiedSlots = new HashSet<>();
		for (Tag itemTag : itemsTag) {
			if (!(itemTag instanceof CompoundTag itemData) || !itemData.contains("Slot", Tag.TAG_ANY_NUMERIC)) {
				return Optional.empty();
			}
			int slot = itemData.getInt("Slot");
			if (slot < 0 || slot >= size || !occupiedSlots.add(slot)) {
				return Optional.empty();
			}
			Optional<ItemStack> itemStack = ItemStack.parse(registryAccess, LegacyItemStackMigration.normalizeItemStack(itemData.copy()));
			if (itemStack.isEmpty() || itemStack.get().isEmpty()) {
				return Optional.empty();
			}
			items.set(slot, itemStack.get());
		}
		return Optional.of(ItemContainerContents.fromItems(items));
	}

	public static Optional<FilterAttributes> parseFilterAttributes(Tag tag, RegistryAccess registryAccess) {
		return parseFilterAttributes(tag, registryAccess, false);
	}

	public static Optional<FilterAttributes> parseFilterAttributes(Tag tag, RegistryAccess registryAccess, boolean defaultIsAllowList) {
		if (!(tag instanceof CompoundTag legacyData)) {
			return Optional.empty();
		}
		Optional<Set<TagKey<Item>>> tagKeys = parseTagKeys(legacyData);
		Optional<Boolean> isAllowList = parseOptionalBoolean(legacyData, "isAllowList", defaultIsAllowList);
		Optional<Boolean> matchDurability = parseOptionalBoolean(legacyData, "matchDurability", false);
		Optional<Boolean> matchComponents = parseOptionalBoolean(legacyData, "matchNbt", false);
		Optional<PrimaryMatch> primaryMatch = parsePrimaryMatch(legacyData);
		Optional<Boolean> matchAnyTag = parseOptionalBoolean(legacyData, "matchAnyTag", true);
		Optional<ItemContainerContents> filters = parseFilters(legacyData, registryAccess);
		Optional<Boolean> filterByStorage = parseOptionalBoolean(legacyData, "filterByStorage", false);
		Optional<Boolean> filterByInventory = parseOptionalBoolean(legacyData, "filterByInventory", false);
		if (tagKeys.isEmpty() || isAllowList.isEmpty() || matchDurability.isEmpty() || matchComponents.isEmpty() || primaryMatch.isEmpty()
				|| matchAnyTag.isEmpty() || filters.isEmpty() || filterByStorage.isEmpty() || filterByInventory.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new FilterAttributes(tagKeys.get(), isAllowList.get(), matchDurability.get(), matchComponents.get(), primaryMatch.get(),
				matchAnyTag.get(), filters.get(), filterByStorage.get(), filterByInventory.get()));
	}

	static Optional<Boolean> parseBoolean(Tag tag) {
		if (tag.getId() != Tag.TAG_BYTE) {
			return Optional.empty();
		}
		byte value = ((ByteTag) tag).getAsByte();
		return value == 0 || value == 1 ? Optional.of(value == 1) : Optional.empty();
	}

	static Optional<Integer> parseInteger(Tag tag) {
		return tag.getId() == Tag.TAG_INT ? Optional.of(((IntTag) tag).getAsInt()) : Optional.empty();
	}

	static Optional<Long> parseLong(Tag tag) {
		return tag.getId() == Tag.TAG_LONG ? Optional.of(((LongTag) tag).getAsLong()) : Optional.empty();
	}

	static Optional<Float> parseFloat(Tag tag) {
		return tag.getId() == Tag.TAG_FLOAT ? Optional.of(((FloatTag) tag).getAsFloat()) : Optional.empty();
	}

	static Optional<Map<ResourceLocation, Integer>> parseRecipesUsed(Tag tag) {
		if (!(tag instanceof CompoundTag recipesTag)) {
			return Optional.empty();
		}
		Map<ResourceLocation, Integer> recipesUsed = new HashMap<>();
		for (String recipeId : recipesTag.getAllKeys()) {
			ResourceLocation resourceLocation = ResourceLocation.tryParse(recipeId);
			Tag countTag = recipesTag.get(recipeId);
			if (resourceLocation == null || countTag == null || countTag.getId() != Tag.TAG_INT) {
				return Optional.empty();
			}
			int count = ((IntTag) countTag).getAsInt();
			if (count < 0) {
				return Optional.empty();
			}
			recipesUsed.put(resourceLocation, count);
		}
		return Optional.of(Map.copyOf(recipesUsed));
	}

	static Optional<ResourceLocation> parseResourceLocation(Tag tag) {
		return tag instanceof StringTag stringTag ? Optional.ofNullable(ResourceLocation.tryParse(stringTag.getAsString())) : Optional.empty();
	}

	static Optional<SimpleItemContent> parseSingleItemContent(Tag tag, RegistryAccess registryAccess) {
		if (!(tag instanceof CompoundTag itemData)) {
			return Optional.empty();
		}
		return ItemStack.parse(registryAccess, LegacyItemStackMigration.normalizeItemStack(itemData.copy())).map(SimpleItemContent::copyOf);
	}

	static Optional<SimpleFluidContent> parseFluidContent(Tag tag, RegistryAccess registryAccess) {
		if (!(tag instanceof CompoundTag fluidData) || !fluidData.contains("FluidName", Tag.TAG_STRING) || !fluidData.contains("Amount", Tag.TAG_INT)) {
			return Optional.empty();
		}
		if (fluidData.contains("Tag") && !fluidData.contains("Tag", Tag.TAG_COMPOUND)) {
			return Optional.empty();
		}
		ResourceLocation fluidId = ResourceLocation.tryParse(fluidData.getString("FluidName"));
		if (fluidId == null) {
			return Optional.empty();
		}
		var fluidRegistry = registryAccess.registryOrThrow(Registries.FLUID);
		if (!fluidRegistry.containsKey(fluidId)) {
			return Optional.empty();
		}
		Fluid fluid = fluidRegistry.get(fluidId);
		int amount = fluidData.getInt("Amount");
		if (fluid != Fluids.EMPTY && amount <= 0) {
			return Optional.empty();
		}
		FluidStack fluidStack = new FluidStack(fluid, amount);
		if (fluidData.contains("Tag", Tag.TAG_COMPOUND)) {
			fluidStack.set(DataComponents.CUSTOM_DATA, CustomData.of(fluidData.getCompound("Tag")));
		}
		return Optional.of(SimpleFluidContent.copyOf(fluidStack));
	}

	static Optional<List<SimpleFluidContent>> parseFluidFilters(Tag tag, RegistryAccess registryAccess) {
		if (!(tag instanceof ListTag fluidFilters)) {
			return Optional.empty();
		}
		List<SimpleFluidContent> parsedFilters = new ArrayList<>();
		for (Tag fluidFilter : fluidFilters) {
			Optional<SimpleFluidContent> parsedFilter = parseFluidContent(fluidFilter, registryAccess);
			if (parsedFilter.isEmpty()) {
				return Optional.empty();
			}
			parsedFilters.add(parsedFilter.get());
		}
		return Optional.of(List.copyOf(parsedFilters));
	}

	static Optional<HungerLevel> parseHungerLevel(Tag tag) {
		return parseEnum(tag, HungerLevel.CODEC);
	}

	static Optional<Direction> parseDirection(Tag tag) {
		return parseEnum(tag, Direction.CODEC);
	}

	static Optional<VoidType> parseVoidType(Tag tag) {
		return parseEnum(tag, VoidType.CODEC);
	}

	static Optional<AutomationDirection> parseAutomationDirection(Tag tag) {
		return parseEnum(tag, AutomationDirection.CODEC);
	}

	static Optional<RepeatMode> parseRepeatMode(Tag tag) {
		return parseEnum(tag, RepeatMode.CODEC);
	}

	static Optional<EntityMatch> parseEntityMatch(Tag tag) {
		return parseEnum(tag, EntityMatch.CODEC);
	}

	static Optional<List<AlchemyFilterAttribute>> parseAlchemyFilterAttributes(Tag tag, RegistryAccess registryAccess) {
		if (!(tag instanceof ListTag attributes)) {
			return Optional.empty();
		}
		List<AlchemyFilterAttribute> parsedAttributes = new ArrayList<>();
		for (Tag attributeTag : attributes) {
			if (!(attributeTag instanceof CompoundTag attribute)) {
				return Optional.empty();
			}
			Optional<ItemStack> filter = parseAlchemyFilter(attribute, registryAccess);
			Optional<AlchemyCondition> condition = parseAlchemyCondition(attribute);
			Optional<Float> value = parseAlchemyValue(attribute);
			if (filter.isEmpty() || condition.isEmpty() || value.isEmpty()) {
				return Optional.empty();
			}
			parsedAttributes.add(new AlchemyFilterAttribute(filter.get(), condition.get(), value.get()));
		}
		return Optional.of(List.copyOf(parsedAttributes));
	}

	private static Optional<ItemStack> parseAlchemyFilter(CompoundTag attribute, RegistryAccess registryAccess) {
		Tag filterTag = attribute.get("filter");
		if (filterTag == null) {
			return Optional.of(ItemStack.EMPTY);
		}
		if (!(filterTag instanceof CompoundTag filterData) || filterData.isEmpty()) {
			return Optional.empty();
		}
		if (filterData.size() == 2 && filterData.contains("id", Tag.TAG_STRING) && "minecraft:air".equals(filterData.getString("id"))
				&& filterData.contains("Count", Tag.TAG_BYTE) && (filterData.getByte("Count") == 0 || filterData.getByte("Count") == 1)) {
			return Optional.of(ItemStack.EMPTY);
		}
		return ItemStack.parse(registryAccess, LegacyItemStackMigration.normalizeItemStack(filterData.copy()));
	}

	private static Optional<AlchemyCondition> parseAlchemyCondition(CompoundTag attribute) {
		Tag conditionTag = attribute.get("condition");
		if (conditionTag == null) {
			return Optional.of(AlchemyCondition.NEVER);
		}
		return conditionTag instanceof StringTag ? AlchemyCondition.CODEC.parse(NbtOps.INSTANCE, conditionTag).result() : Optional.empty();
	}

	private static Optional<Float> parseAlchemyValue(CompoundTag attribute) {
		Tag valueTag = attribute.get("value");
		return valueTag == null ? Optional.of(0F) : valueTag instanceof NumericTag numericTag ? Optional.of(numericTag.getAsFloat()) : Optional.empty();
	}

	private static <T> Optional<T> parseEnum(Tag tag, Codec<T> codec) {
		return tag instanceof StringTag ? codec.parse(NbtOps.INSTANCE, tag).result() : Optional.empty();
	}

	private static Optional<Boolean> parseOptionalBoolean(CompoundTag legacyData, String key, boolean defaultValue) {
		Tag value = legacyData.get(key);
		return value == null ? Optional.of(defaultValue) : parseBoolean(value);
	}

	private static Optional<Set<TagKey<Item>>> parseTagKeys(CompoundTag legacyData) {
		Tag value = legacyData.get("tags");
		if (value == null) {
			return Optional.of(Set.of());
		}
		if (!(value instanceof ListTag tags)) {
			return Optional.empty();
		}
		Set<TagKey<Item>> tagKeys = new HashSet<>();
		for (Tag tag : tags) {
			if (!(tag instanceof StringTag stringTag)) {
				return Optional.empty();
			}
			ResourceLocation location = ResourceLocation.tryParse(stringTag.getAsString());
			if (location == null) {
				return Optional.empty();
			}
			tagKeys.add(TagKey.create(Registries.ITEM, location));
		}
		return Optional.of(Set.copyOf(tagKeys));
	}

	private static Optional<PrimaryMatch> parsePrimaryMatch(CompoundTag legacyData) {
		Tag value = legacyData.get("primaryMatch");
		if (value == null) {
			return Optional.of(PrimaryMatch.ITEM);
		}
		if (!(value instanceof StringTag)) {
			return Optional.empty();
		}
		return PrimaryMatch.CODEC.parse(NbtOps.INSTANCE, value).result();
	}

	private static Optional<ItemContainerContents> parseFilters(CompoundTag legacyData, RegistryAccess registryAccess) {
		Tag value = legacyData.get("filters");
		return value == null ? Optional.of(ItemContainerContents.EMPTY) : parseItemStackHandler(value, registryAccess);
	}
}
