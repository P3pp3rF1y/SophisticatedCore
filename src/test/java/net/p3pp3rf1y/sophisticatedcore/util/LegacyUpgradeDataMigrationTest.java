package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.EntityMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterAttributes;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyCondition;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyFilterAttribute;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.battery.BatteryUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.compacting.CompactingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoBlastingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoSmeltingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoSmokingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.BlastingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.IAutoCookingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.ICookingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.SmeltingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.SmokingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.HungerLevel;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter.StonecutterUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.AutomationDirection;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class LegacyUpgradeDataMigrationTest {
	private static final RegistryAccess REGISTRIES = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void migrateConvertsEnabledAndDropsLegacyDataForExistingComponent() {
		ItemStack migratedStack = new ItemStack(Items.STICK);
		CompoundTag legacyData = new CompoundTag();
		legacyData.putBoolean("enabled", true);
		migratedStack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));

		boolean migrated = LegacyUpgradeDataMigration.migrate(migratedStack, REGISTRIES);
		boolean migratedAgain = LegacyUpgradeDataMigration.migrate(migratedStack, REGISTRIES);

		assertTrue(migrated);
		assertFalse(migratedAgain);
		assertTrue(migratedStack.get(ModCoreDataComponents.ENABLED.get()));
		assertFalse(migratedStack.has(DataComponents.CUSTOM_DATA));

		ItemStack currentStack = new ItemStack(Items.STICK);
		currentStack.set(ModCoreDataComponents.ENABLED.get(), false);
		CompoundTag currentLegacyData = new CompoundTag();
		currentLegacyData.putBoolean("enabled", true);
		currentStack.set(DataComponents.CUSTOM_DATA, CustomData.of(currentLegacyData));

		boolean currentMigrated = LegacyUpgradeDataMigration.migrate(currentStack, REGISTRIES);

		assertTrue(currentMigrated);
		assertFalse(currentStack.get(ModCoreDataComponents.ENABLED.get()));
		assertFalse(currentStack.has(DataComponents.CUSTOM_DATA));

		CompoundTag malformedLegacyData = new CompoundTag();
		malformedLegacyData.putInt("enabled", 1);
		currentStack.set(DataComponents.CUSTOM_DATA, CustomData.of(malformedLegacyData));

		assertTrue(LegacyUpgradeDataMigration.migrate(currentStack, REGISTRIES));
		assertFalse(currentStack.get(ModCoreDataComponents.ENABLED.get()));
		assertFalse(currentStack.has(DataComponents.CUSTOM_DATA));
	}

	@Test
	void migratePreservesUnknownAndMalformedCustomDataAndIsIdempotent() {
		ItemStack stack = new ItemStack(Items.STICK);
		CompoundTag legacyData = new CompoundTag();
		legacyData.putInt("enabled", 1);
		legacyData.putString("unknown", "preserved");
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));

		boolean migrated = LegacyUpgradeDataMigration.migrate(stack, REGISTRIES);
		boolean migratedAgain = LegacyUpgradeDataMigration.migrate(stack, REGISTRIES);

		assertFalse(migrated);
		assertFalse(migratedAgain);
		CompoundTag residualData = stack.get(DataComponents.CUSTOM_DATA).copyTag();
		assertEquals(1, residualData.getInt("enabled"));
		assertEquals("preserved", residualData.getString("unknown"));
	}

	@Test
	void parseItemStackHandlerNormalizesAndPlacesItemsAndRejectsInvalidSlots() {
		CompoundTag inventory = new CompoundTag();
		inventory.putInt("Size", 3);
		ListTag items = new ListTag();
		CompoundTag item = new CompoundTag();
		item.putString("id", "minecraft:stone");
		item.putByte("Count", (byte) 1);
		item.putInt("Slot", 2);
		items.add(item);
		inventory.put("Items", items);

		ItemContainerContents contents = LegacyUpgradeDataMigration.parseItemStackHandler(inventory, REGISTRIES).orElseThrow();

		assertEquals(3, contents.getSlots());
		assertTrue(contents.getStackInSlot(0).isEmpty());
		assertEquals(Items.STONE, contents.getStackInSlot(2).getItem());

		CompoundTag duplicateSlotInventory = inventory.copy();
		duplicateSlotInventory.getList("Items", Tag.TAG_COMPOUND).add(item.copy());
		assertTrue(LegacyUpgradeDataMigration.parseItemStackHandler(duplicateSlotInventory, REGISTRIES).isEmpty());
	}

	@Test
	void parseFilterAttributesUsesLegacyMatchAnyTagDefaultAndRejectsMalformedDataAtomically() {
		CompoundTag legacyData = new CompoundTag();
		legacyData.putBoolean("isAllowList", true);
		legacyData.putBoolean("matchDurability", true);
		legacyData.putBoolean("matchNbt", true);
		legacyData.putString("primaryMatch", "tags");
		legacyData.putBoolean("matchAnyTag", false);
		legacyData.putBoolean("filterByStorage", true);
		ListTag tags = new ListTag();
		tags.add(StringTag.valueOf("minecraft:logs"));
		legacyData.put("tags", tags);
		legacyData.put("filters", emptyInventory());

		FilterAttributes attributes = LegacyUpgradeDataMigration.parseFilterAttributes(legacyData, REGISTRIES).orElseThrow();

		assertTrue(attributes.isAllowList());
		assertTrue(attributes.matchDurability());
		assertTrue(attributes.matchComponents());
		assertEquals(PrimaryMatch.TAGS, attributes.primaryMatch());
		assertFalse(attributes.matchAnyTag());
		assertEquals(1, attributes.tagKeys().size());
		assertTrue(attributes.filterByStorage());

		CompoundTag malformedData = legacyData.copy();
		malformedData.putString("matchNbt", "true");
		CompoundTag sourceData = new CompoundTag();
		sourceData.put("filter", malformedData);
		ItemStack stack = new ItemStack(Items.STICK);
		boolean migrated = LegacyUpgradeDataMigration.migrateComponent(stack, sourceData, "filter", ModCoreDataComponents.FILTER_ATTRIBUTES.get(),
				tag -> LegacyUpgradeDataMigration.parseFilterAttributes(tag, REGISTRIES));

		assertFalse(migrated);
		assertFalse(stack.has(ModCoreDataComponents.FILTER_ATTRIBUTES.get()));
		assertTrue(sourceData.contains("filter"));
	}

	@Test
	void migrateMapsFilterFamiliesAndCompactingScalarsWithoutRemovingUnknownData() {
		CompoundTag compactingData = rootFilterData();
		compactingData.putBoolean("compactNonUncraftable", true);
		compactingData.putBoolean("shouldWorkInGUI", true);
		compactingData.putString("unknown", "preserved");
		ItemStack compactingStack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateRootFilterAttributes(compactingStack, compactingData, REGISTRIES));
		assertTrue(migrateBoolean(compactingStack, compactingData, "compactNonUncraftable", ModCoreDataComponents.COMPACT_NON_UNCRAFTABLE.get()));
		assertTrue(migrateBoolean(compactingStack, compactingData, "shouldWorkInGUI", ModCoreDataComponents.SHOULD_WORK_IN_GUI.get()));
		assertTrue(compactingStack.has(ModCoreDataComponents.FILTER_ATTRIBUTES.get()));
		assertTrue(compactingStack.get(ModCoreDataComponents.COMPACT_NON_UNCRAFTABLE));
		assertTrue(compactingStack.get(ModCoreDataComponents.SHOULD_WORK_IN_GUI));
		assertEquals("preserved", compactingData.getString("unknown"));
		assertFalse(compactingData.contains("isAllowList"));
		assertFalse(LegacyUpgradeDataMigration.migrateRootFilterAttributes(compactingStack, compactingData, REGISTRIES));

		CompoundTag malformedFilterData = rootFilterData();
		malformedFilterData.putString("matchNbt", "true");
		assertFalse(LegacyUpgradeDataMigration.migrateRootFilterAttributes(new ItemStack(Items.STICK), malformedFilterData, REGISTRIES));
		assertTrue(malformedFilterData.contains("isAllowList"));
		assertTrue(malformedFilterData.contains("matchNbt"));

		ItemStack existingFilterStack = new ItemStack(Items.STICK);
		existingFilterStack.set(ModCoreDataComponents.FILTER_ATTRIBUTES, compactingStack.get(ModCoreDataComponents.FILTER_ATTRIBUTES));
		assertTrue(LegacyUpgradeDataMigration.migrateRootFilterAttributes(existingFilterStack, malformedFilterData, REGISTRIES));
		assertTrue(malformedFilterData.isEmpty());
	}

	@Test
	void migrateVoidDataUsesAllowListDefaultAndDropsLegacyDataForExistingComponents() {
		ItemStack stack = stackWithUpgradeItem(mock(VoidUpgradeItem.class));
		stack.set(ModCoreDataComponents.SHOULD_WORK_IN_GUI, false);
		stack.set(ModCoreDataComponents.FLUID_FILTERS, List.of(SimpleFluidContent.copyOf(new FluidStack(Fluids.LAVA, 1))));
		CompoundTag legacyData = new CompoundTag();
		legacyData.put("filters", emptyInventory());
		legacyData.putBoolean("shouldWorkInGUI", true);
		legacyData.put("fluidFilters", legacyFluidFilters(legacyFluid("minecraft:water", 1000)));
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));

		boolean migrated = LegacyUpgradeDataMigration.migrate(stack, REGISTRIES);
		boolean migratedAgain = LegacyUpgradeDataMigration.migrate(stack, REGISTRIES);

		assertTrue(migrated);
		assertFalse(migratedAgain);
		assertTrue(stack.get(ModCoreDataComponents.FILTER_ATTRIBUTES).isAllowList());
		assertFalse(stack.get(ModCoreDataComponents.SHOULD_WORK_IN_GUI));
		assertEquals(Fluids.LAVA, stack.get(ModCoreDataComponents.FLUID_FILTERS).getFirst().getFluid());
		assertFalse(stack.has(DataComponents.CUSTOM_DATA));
	}

	@Test
	void migrateVoidDataPreservesMalformedFluidFilters() {
		ItemStack stack = stackWithUpgradeItem(mock(VoidUpgradeItem.class));
		CompoundTag legacyData = new CompoundTag();
		legacyData.put("fluidFilters", legacyFluidFilters(legacyFluid("minecraft:water", 0)));
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));

		assertFalse(LegacyUpgradeDataMigration.migrate(stack, REGISTRIES));
		assertFalse(stack.has(ModCoreDataComponents.FLUID_FILTERS));
		assertTrue(stack.get(DataComponents.CUSTOM_DATA).copyTag().contains("fluidFilters"));
	}

	@Test
	void parseFilterAttributesUsesCallerProvidedAllowListDefault() {
		CompoundTag filterData = new CompoundTag();

		assertFalse(LegacyUpgradeDataMigration.parseFilterAttributes(filterData, REGISTRIES).orElseThrow().isAllowList());
		assertTrue(LegacyUpgradeDataMigration.parseFilterAttributes(filterData, REGISTRIES, true).orElseThrow().isAllowList());
	}

	@Test
	void migrateMapsEnumsScalarsAndVoidFallbackWithStrictPreservation() {
		CompoundTag feedingData = new CompoundTag();
		feedingData.putString("feedAtHungerLevel", "full");
		feedingData.putBoolean("feedImmediatelyWhenHurt", false);
		ItemStack feedingStack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(feedingStack, feedingData, "feedAtHungerLevel", ModCoreDataComponents.FEED_AT_HUNGER_LEVEL.get(),
				LegacyUpgradeDataMigration::parseHungerLevel));
		assertTrue(migrateBoolean(feedingStack, feedingData, "feedImmediatelyWhenHurt", ModCoreDataComponents.FEED_IMMEDIATELY_WHEN_HURT.get()));
		assertEquals(HungerLevel.FULL, feedingStack.get(ModCoreDataComponents.FEED_AT_HUNGER_LEVEL));
		assertFalse(feedingStack.get(ModCoreDataComponents.FEED_IMMEDIATELY_WHEN_HURT));

		CompoundTag filterData = new CompoundTag();
		filterData.putString("direction", "input");
		ItemStack filterStack = new ItemStack(Items.STICK);
		filterStack.set(ModCoreDataComponents.DIRECTION, Direction.OUTPUT);
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(filterStack, filterData, "direction", ModCoreDataComponents.DIRECTION.get(),
				LegacyUpgradeDataMigration::parseDirection));
		assertEquals(Direction.OUTPUT, filterStack.get(ModCoreDataComponents.DIRECTION));
		assertFalse(filterData.contains("direction"));

		CompoundTag magnetData = new CompoundTag();
		magnetData.putBoolean("pickupItems", false);
		magnetData.putBoolean("pickupXp", false);
		ItemStack magnetStack = new ItemStack(Items.STICK);
		assertTrue(migrateBoolean(magnetStack, magnetData, "pickupItems", ModCoreDataComponents.PICKUP_ITEMS.get()));
		assertTrue(migrateBoolean(magnetStack, magnetData, "pickupXp", ModCoreDataComponents.PICKUP_XP.get()));
		assertFalse(magnetStack.get(ModCoreDataComponents.PICKUP_ITEMS));
		assertFalse(magnetStack.get(ModCoreDataComponents.PICKUP_XP));

		CompoundTag voidData = new CompoundTag();
		voidData.putBoolean("shouldVoidOverflow", false);
		ItemStack voidStack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateVoidOverflow(voidStack, voidData));
		assertEquals(VoidType.SLOT_OVERFLOW, voidStack.get(ModCoreDataComponents.VOID_TYPE));
		CompoundTag alwaysVoidData = new CompoundTag();
		alwaysVoidData.putBoolean("shouldVoidOverflow", true);
		ItemStack alwaysVoidStack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateVoidOverflow(alwaysVoidStack, alwaysVoidData));
		assertEquals(VoidType.ALWAYS, alwaysVoidStack.get(ModCoreDataComponents.VOID_TYPE));
		CompoundTag voidTypeData = new CompoundTag();
		voidTypeData.putString("voidType", "storage_overflow");
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(voidStack, voidTypeData, "voidType", ModCoreDataComponents.VOID_TYPE.get(),
				LegacyUpgradeDataMigration::parseVoidType));
		assertEquals(VoidType.SLOT_OVERFLOW, voidStack.get(ModCoreDataComponents.VOID_TYPE));
		assertFalse(voidTypeData.contains("voidType"));
		CompoundTag malformedOverflowData = new CompoundTag();
		malformedOverflowData.putString("shouldVoidOverflow", "invalid");
		assertTrue(LegacyUpgradeDataMigration.migrateVoidOverflow(voidStack, malformedOverflowData));
		assertEquals(VoidType.SLOT_OVERFLOW, voidStack.get(ModCoreDataComponents.VOID_TYPE));
		assertTrue(malformedOverflowData.isEmpty());

		CompoundTag malformedVoidData = new CompoundTag();
		malformedVoidData.putString("voidType", "invalid");
		ItemStack malformedVoidStack = new ItemStack(Items.STICK);
		assertFalse(LegacyUpgradeDataMigration.migrateComponent(malformedVoidStack, malformedVoidData, "voidType", ModCoreDataComponents.VOID_TYPE.get(),
				LegacyUpgradeDataMigration::parseVoidType));
		assertFalse(malformedVoidStack.has(ModCoreDataComponents.VOID_TYPE));
		assertTrue(malformedVoidData.contains("voidType"));

		CompoundTag malformedScalarData = new CompoundTag();
		malformedScalarData.putByte("pickupItems", (byte) 2);
		assertFalse(migrateBoolean(new ItemStack(Items.STICK), malformedScalarData, "pickupItems", ModCoreDataComponents.PICKUP_ITEMS.get()));
		assertTrue(malformedScalarData.contains("pickupItems"));
	}

	@Test
	void migrateVoidAliasesUsesLegacyPrecedenceAndCleansUpInOnePass() {
		CompoundTag conflictingAliases = new CompoundTag();
		conflictingAliases.putBoolean("shouldVoidOverflow", true);
		conflictingAliases.putString("voidType", "storage_overflow");
		ItemStack conflictingStack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateVoidOverflow(conflictingStack, conflictingAliases));
		assertEquals(VoidType.ALWAYS, conflictingStack.get(ModCoreDataComponents.VOID_TYPE));
		assertTrue(conflictingAliases.isEmpty());

		CompoundTag validCurrentAlias = new CompoundTag();
		validCurrentAlias.putString("shouldVoidOverflow", "invalid");
		validCurrentAlias.putString("voidType", "storage_overflow");
		ItemStack currentAliasStack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateVoidOverflow(currentAliasStack, validCurrentAlias));
		assertEquals(VoidType.STORAGE_OVERFLOW, currentAliasStack.get(ModCoreDataComponents.VOID_TYPE));
		assertTrue(validCurrentAlias.isEmpty());
		assertFalse(LegacyUpgradeDataMigration.migrateVoidOverflow(currentAliasStack, validCurrentAlias));

		CompoundTag malformedAliases = new CompoundTag();
		malformedAliases.putString("shouldVoidOverflow", "invalid");
		malformedAliases.putString("voidType", "invalid");
		ItemStack malformedStack = new ItemStack(Items.STICK);
		assertFalse(LegacyUpgradeDataMigration.migrateVoidOverflow(malformedStack, malformedAliases));
		assertFalse(malformedStack.has(ModCoreDataComponents.VOID_TYPE));
		assertTrue(malformedAliases.contains("shouldVoidOverflow"));
		assertTrue(malformedAliases.contains("voidType"));

		CompoundTag staleAliases = malformedAliases.copy();
		ItemStack existingStack = new ItemStack(Items.STICK);
		existingStack.set(ModCoreDataComponents.VOID_TYPE, VoidType.SLOT_OVERFLOW);
		assertTrue(LegacyUpgradeDataMigration.migrateVoidOverflow(existingStack, staleAliases));
		assertEquals(VoidType.SLOT_OVERFLOW, existingStack.get(ModCoreDataComponents.VOID_TYPE));
		assertTrue(staleAliases.isEmpty());
	}

	@Test
	void migrateMapsXpPumpAndPumpScalarsAndIsIdempotent() {
		CompoundTag xpPumpData = new CompoundTag();
		xpPumpData.putString("direction", "keep");
		xpPumpData.putInt("level", 10);
		xpPumpData.putInt("levelsToStore", 2);
		xpPumpData.putInt("levelsToTake", 3);
		xpPumpData.putBoolean("mendItems", false);
		ItemStack xpPumpStack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(xpPumpStack, xpPumpData, "direction", ModCoreDataComponents.AUTOMATION_DIRECTION.get(),
				LegacyUpgradeDataMigration::parseAutomationDirection));
		assertTrue(migrateInteger(xpPumpStack, xpPumpData, "level", ModCoreDataComponents.LEVEL.get()));
		assertTrue(migrateInteger(xpPumpStack, xpPumpData, "levelsToStore", ModCoreDataComponents.LEVELS_TO_STORE.get()));
		assertTrue(migrateInteger(xpPumpStack, xpPumpData, "levelsToTake", ModCoreDataComponents.LEVELS_TO_TAKE.get()));
		assertTrue(migrateBoolean(xpPumpStack, xpPumpData, "mendItems", ModCoreDataComponents.MEND_ITEMS.get()));
		assertEquals(AutomationDirection.KEEP, xpPumpStack.get(ModCoreDataComponents.AUTOMATION_DIRECTION));
		assertEquals(10, xpPumpStack.get(ModCoreDataComponents.LEVEL));
		assertEquals(2, xpPumpStack.get(ModCoreDataComponents.LEVELS_TO_STORE));
		assertEquals(3, xpPumpStack.get(ModCoreDataComponents.LEVELS_TO_TAKE));
		assertFalse(xpPumpStack.get(ModCoreDataComponents.MEND_ITEMS));

		CompoundTag pumpData = new CompoundTag();
		pumpData.putBoolean("interactWithHand", true);
		pumpData.putBoolean("interactWithWorld", true);
		pumpData.putBoolean("interactWithFluidHandlers", true);
		pumpData.put("fluidFilters", emptyInventory());
		pumpData.putBoolean("input", false);
		ItemStack pumpStack = new ItemStack(Items.STICK);
		assertTrue(migrateBoolean(pumpStack, pumpData, "interactWithHand", ModCoreDataComponents.INTERACT_WITH_HAND.get()));
		assertTrue(migrateBoolean(pumpStack, pumpData, "interactWithWorld", ModCoreDataComponents.INTERACT_WITH_WORLD.get()));
		assertTrue(migrateBoolean(pumpStack, pumpData, "interactWithFluidHandlers", ModCoreDataComponents.INTERACT_WITH_FLUID_HANDLERS.get()));
		assertTrue(pumpStack.get(ModCoreDataComponents.INTERACT_WITH_HAND));
		assertTrue(pumpStack.get(ModCoreDataComponents.INTERACT_WITH_WORLD));
		assertTrue(pumpStack.get(ModCoreDataComponents.INTERACT_WITH_FLUID_HANDLERS));
		assertTrue(pumpData.contains("fluidFilters"));
		assertTrue(pumpData.contains("input"));
		assertFalse(migrateBoolean(pumpStack, pumpData, "interactWithHand", ModCoreDataComponents.INTERACT_WITH_HAND.get()));
	}

	@Test
	void migrateFluidDataPreservesTagsAmountsOrderingAndValidStaleKeys() {
		CompoundTag taggedWater = legacyFluid("minecraft:water", 125);
		CompoundTag fluidTag = new CompoundTag();
		fluidTag.putString("variant", "cold");
		taggedWater.put("Tag", fluidTag);
		CompoundTag lava = legacyFluid("minecraft:lava", 250);
		ListTag filters = new ListTag();
		filters.add(taggedWater);
		filters.add(lava);

		var parsedFilters = LegacyUpgradeDataMigration.parseFluidFilters(filters, REGISTRIES).orElseThrow();

		assertEquals(2, parsedFilters.size());
		assertEquals(Fluids.WATER, parsedFilters.get(0).getFluid());
		assertEquals(125, parsedFilters.get(0).getAmount());
		assertEquals("cold", parsedFilters.get(0).getComponents().get(DataComponents.CUSTOM_DATA).copyTag().getString("variant"));
		assertEquals(Fluids.LAVA, parsedFilters.get(1).getFluid());
		assertEquals(250, parsedFilters.get(1).getAmount());
		assertFalse(parsedFilters.get(1).getComponents().has(DataComponents.CUSTOM_DATA));

		CompoundTag pumpData = new CompoundTag();
		pumpData.put("fluidFilters", filters.copy());
		pumpData.putBoolean("input", false);
		ItemStack pumpStack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(pumpStack, pumpData, "fluidFilters", ModCoreDataComponents.FLUID_FILTERS.get(),
				tag -> LegacyUpgradeDataMigration.parseFluidFilters(tag, REGISTRIES)));
		assertTrue(migrateBoolean(pumpStack, pumpData, "input", ModCoreDataComponents.IS_INPUT.get()));
		assertEquals(parsedFilters, pumpStack.get(ModCoreDataComponents.FLUID_FILTERS));
		assertFalse(pumpStack.get(ModCoreDataComponents.IS_INPUT));
		assertFalse(pumpData.contains("fluidFilters"));
		assertFalse(pumpData.contains("input"));
		assertFalse(LegacyUpgradeDataMigration.migrateComponent(pumpStack, pumpData, "fluidFilters", ModCoreDataComponents.FLUID_FILTERS.get(),
				tag -> LegacyUpgradeDataMigration.parseFluidFilters(tag, REGISTRIES)));
	}

	@Test
	void migrateFluidDataRetainsMalformedOrUnknownValuesWithoutTargetsAndDropsLegacyDataForExistingTargets() {
		CompoundTag malformed = legacyFluid("minecraft:water", 0);
		CompoundTag malformedData = new CompoundTag();
		malformedData.put("contents", malformed);
		assertFalse(LegacyUpgradeDataMigration.migrateComponent(new ItemStack(Items.STICK), malformedData, "contents",
				ModCoreDataComponents.FLUID_CONTENTS.get(), tag -> LegacyUpgradeDataMigration.parseFluidContent(tag, REGISTRIES)));
		assertTrue(malformedData.contains("contents"));

		CompoundTag unknownData = new CompoundTag();
		unknownData.put("fluidFilters", legacyFluidFilters(legacyFluid("example:unknown", 1000)));
		assertFalse(LegacyUpgradeDataMigration.migrateComponent(new ItemStack(Items.STICK), unknownData, "fluidFilters",
				ModCoreDataComponents.FLUID_FILTERS.get(), tag -> LegacyUpgradeDataMigration.parseFluidFilters(tag, REGISTRIES)));
		assertTrue(unknownData.contains("fluidFilters"));

		ItemStack currentStack = new ItemStack(Items.STICK);
		currentStack.set(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.copyOf(new FluidStack(Fluids.LAVA, 1)));
		CompoundTag currentData = new CompoundTag();
		currentData.put("contents", legacyFluid("minecraft:water", 1000));
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(currentStack, currentData, "contents", ModCoreDataComponents.FLUID_CONTENTS.get(),
				tag -> LegacyUpgradeDataMigration.parseFluidContent(tag, REGISTRIES)));
		assertEquals(Fluids.LAVA, currentStack.get(ModCoreDataComponents.FLUID_CONTENTS).getFluid());
		assertFalse(currentData.contains("contents"));

		ItemStack currentPumpStack = new ItemStack(Items.STICK);
		currentPumpStack.set(ModCoreDataComponents.FLUID_FILTERS, List.of(SimpleFluidContent.copyOf(new FluidStack(Fluids.LAVA, 1))));
		currentPumpStack.set(ModCoreDataComponents.IS_INPUT, true);
		CompoundTag currentPumpData = new CompoundTag();
		currentPumpData.put("fluidFilters", legacyFluidFilters(legacyFluid("minecraft:water", 1000)));
		currentPumpData.putBoolean("input", false);
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(currentPumpStack, currentPumpData, "fluidFilters", ModCoreDataComponents.FLUID_FILTERS.get(),
				tag -> LegacyUpgradeDataMigration.parseFluidFilters(tag, REGISTRIES)));
		assertTrue(migrateBoolean(currentPumpStack, currentPumpData, "input", ModCoreDataComponents.IS_INPUT.get()));
		assertEquals(Fluids.LAVA, currentPumpStack.get(ModCoreDataComponents.FLUID_FILTERS).getFirst().getFluid());
		assertTrue(currentPumpStack.get(ModCoreDataComponents.IS_INPUT));
		assertFalse(currentPumpData.contains("fluidFilters"));
		assertFalse(currentPumpData.contains("input"));
	}

	@Test
	void migrateTankInventoryAndContentsNormalizesItemsAndDropsLegacyDataForExistingComponents() {
		CompoundTag tankData = new CompoundTag();
		tankData.put("inventory", inventoryWithItem(4, 3));
		tankData.put("contents", legacyFluid("minecraft:water", 750));
		ItemStack tankStack = new ItemStack(Items.STICK);
		tankStack.set(ModCoreDataComponents.LENIENT_CONTAINER, ItemContainerContents.EMPTY);

		assertTrue(LegacyUpgradeDataMigration.migrateInventory(tankStack, tankData, "inventory", REGISTRIES));
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(tankStack, tankData, "contents", ModCoreDataComponents.FLUID_CONTENTS.get(),
				tag -> LegacyUpgradeDataMigration.parseFluidContent(tag, REGISTRIES)));
		assertEquals(ItemContainerContents.EMPTY, tankStack.get(ModCoreDataComponents.LENIENT_CONTAINER));
		assertEquals(Fluids.WATER, tankStack.get(ModCoreDataComponents.FLUID_CONTENTS).getFluid());
		assertEquals(750, tankStack.get(ModCoreDataComponents.FLUID_CONTENTS).getAmount());
		assertFalse(tankData.contains("inventory"));
		assertFalse(tankData.contains("contents"));
	}

	@Test
	void migrateMapsBatteryAndCraftingInventoriesWithNestedNormalizationAndExistingComponentPrecedence() {
		CompoundTag batteryData = new CompoundTag();
		batteryData.putInt("energyStored", 1200);
		batteryData.put("inventory", inventoryWithItem(2, 1));
		ItemStack batteryStack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(batteryStack, batteryData, "energyStored", ModCoreDataComponents.ENERGY_STORED.get(),
				LegacyUpgradeDataMigration::parseInteger));
		assertTrue(LegacyUpgradeDataMigration.migrateInventory(batteryStack, batteryData, "inventory", REGISTRIES));
		assertEquals(1200, batteryStack.get(ModCoreDataComponents.ENERGY_STORED));
		assertEquals(Items.STONE, batteryStack.get(ModCoreDataComponents.LENIENT_CONTAINER).getStackInSlot(1).getItem());

		CompoundTag craftingData = new CompoundTag();
		craftingData.put("craftingInventory", inventoryWithItem(9, 8));
		craftingData.putBoolean("shiftClickIntoStorage", false);
		craftingData.putBoolean("refill_crafting_grid", true);
		ItemStack craftingStack = new ItemStack(Items.STICK);
		craftingStack.set(ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE, true);
		assertTrue(LegacyUpgradeDataMigration.migrateInventory(craftingStack, craftingData, "craftingInventory", REGISTRIES));
		assertTrue(migrateBoolean(craftingStack, craftingData, "shiftClickIntoStorage", ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE.get()));
		assertTrue(migrateBoolean(craftingStack, craftingData, "refill_crafting_grid", ModCoreDataComponents.REFILL_CRAFTING_GRID.get()));
		assertTrue(craftingStack.get(ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE));
		assertTrue(craftingStack.get(ModCoreDataComponents.REFILL_CRAFTING_GRID));
		assertEquals(Items.STONE, craftingStack.get(ModCoreDataComponents.LENIENT_CONTAINER).getStackInSlot(8).getItem());
		assertFalse(LegacyUpgradeDataMigration.migrateInventory(craftingStack, craftingData, "craftingInventory", REGISTRIES));
	}

	@Test
	void migrateMapsBlockConverterInputAndJukeboxValuesWithoutRemovingMalformedOrObsoleteData() {
		CompoundTag converterData = new CompoundTag();
		converterData.put("input", oldItemStack());
		converterData.putString("recipeId", "minecraft:stonecutting");
		converterData.putBoolean("shiftClickIntoStorage", false);
		converterData.putBoolean("refill_input", true);
		ItemStack converterStack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateInputItem(converterStack, converterData, REGISTRIES));
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(converterStack, converterData, "recipeId", ModCoreDataComponents.RECIPE_ID.get(),
				LegacyUpgradeDataMigration::parseResourceLocation));
		assertTrue(migrateBoolean(converterStack, converterData, "shiftClickIntoStorage", ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE.get()));
		assertTrue(migrateBoolean(converterStack, converterData, "refill_input", ModCoreDataComponents.REFILL_INPUT.get()));
		assertEquals(Items.STONE, converterStack.get(ModCoreDataComponents.INPUT_ITEM).getItem());
		assertEquals("minecraft:stonecutting", converterStack.get(ModCoreDataComponents.RECIPE_ID).toString());

		CompoundTag malformedRecipeData = new CompoundTag();
		malformedRecipeData.putString("recipeId", "not a resource location");
		assertFalse(LegacyUpgradeDataMigration.migrateComponent(new ItemStack(Items.STICK), malformedRecipeData, "recipeId",
				ModCoreDataComponents.RECIPE_ID.get(), LegacyUpgradeDataMigration::parseResourceLocation));
		assertTrue(malformedRecipeData.contains("recipeId"));

		CompoundTag jukeboxData = new CompoundTag();
		jukeboxData.put("discInventory", inventoryWithItem(3, 2));
		jukeboxData.putBoolean("isPlaying", true);
		jukeboxData.putBoolean("shuffle", true);
		jukeboxData.putString("repeatMode", "all");
		jukeboxData.putInt("discSlotActive", 2);
		jukeboxData.putLong("discFinishTime", 123L);
		jukeboxData.putLong("discLength", 42L);
		ItemStack jukeboxStack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateInventory(jukeboxStack, jukeboxData, "discInventory", REGISTRIES));
		assertTrue(migrateBoolean(jukeboxStack, jukeboxData, "isPlaying", ModCoreDataComponents.IS_PLAYING.get()));
		assertTrue(migrateBoolean(jukeboxStack, jukeboxData, "shuffle", ModCoreDataComponents.SHUFFLE.get()));
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(jukeboxStack, jukeboxData, "repeatMode", ModCoreDataComponents.REPEAT_MODE.get(),
				LegacyUpgradeDataMigration::parseRepeatMode));
		assertTrue(migrateInteger(jukeboxStack, jukeboxData, "discSlotActive", ModCoreDataComponents.DISC_SLOT_ACTIVE.get()));
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(jukeboxStack, jukeboxData, "discFinishTime", ModCoreDataComponents.DISC_FINISH_TIME.get(),
				LegacyUpgradeDataMigration::parseLong));
		assertEquals(123L, jukeboxStack.get(ModCoreDataComponents.DISC_FINISH_TIME));
		assertEquals(Items.STONE, jukeboxStack.get(ModCoreDataComponents.LENIENT_CONTAINER).getStackInSlot(2).getItem());
		assertTrue(jukeboxData.contains("discLength"));
		assertFalse(LegacyUpgradeDataMigration.migrateComponent(jukeboxStack, jukeboxData, "discFinishTime", ModCoreDataComponents.DISC_FINISH_TIME.get(),
				LegacyUpgradeDataMigration::parseLong));

		CompoundTag malformedFinishTimeData = new CompoundTag();
		malformedFinishTimeData.putInt("discFinishTime", 123);
		assertFalse(LegacyUpgradeDataMigration.migrateComponent(new ItemStack(Items.STICK), malformedFinishTimeData, "discFinishTime",
				ModCoreDataComponents.DISC_FINISH_TIME.get(), LegacyUpgradeDataMigration::parseLong));
		assertTrue(malformedFinishTimeData.contains("discFinishTime"));

		CompoundTag malformedInventoryData = new CompoundTag();
		malformedInventoryData.put("discInventory", oldItemStack());
		assertFalse(LegacyUpgradeDataMigration.migrateInventory(new ItemStack(Items.STICK), malformedInventoryData, "discInventory", REGISTRIES));
		assertTrue(malformedInventoryData.contains("discInventory"));
	}

	@Test
	void migrateCookingInventoryUsesSmeltingInventoryBeforeCookingInventory() {
		CompoundTag fallbackData = new CompoundTag();
		fallbackData.put("cookingInventory", inventoryWithItem(3, 1));
		ItemStack fallbackStack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateCookingInventory(fallbackStack, fallbackData, REGISTRIES));
		assertEquals(Items.STONE, fallbackStack.get(ModCoreDataComponents.COOKING_INVENTORY).getStackInSlot(1).getItem());
		assertFalse(fallbackData.contains("cookingInventory"));

		CompoundTag primaryData = new CompoundTag();
		primaryData.put("smeltingInventory", inventoryWithItem(3, 0));
		primaryData.put("cookingInventory", oldItemStack());
		ItemStack primaryStack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateCookingInventory(primaryStack, primaryData, REGISTRIES));
		assertEquals(Items.STONE, primaryStack.get(ModCoreDataComponents.COOKING_INVENTORY).getStackInSlot(0).getItem());
		assertFalse(primaryData.contains("smeltingInventory"));
		assertFalse(primaryData.contains("cookingInventory"));
		assertFalse(LegacyUpgradeDataMigration.migrateCookingInventory(primaryStack, primaryData, REGISTRIES));

		CompoundTag existingTargetData = new CompoundTag();
		existingTargetData.put("smeltingInventory", oldItemStack());
		existingTargetData.put("cookingInventory", oldItemStack());
		ItemStack existingTargetStack = new ItemStack(Items.STICK);
		existingTargetStack.set(ModCoreDataComponents.COOKING_INVENTORY, ItemContainerContents.EMPTY);
		assertTrue(LegacyUpgradeDataMigration.migrateCookingInventory(existingTargetStack, existingTargetData, REGISTRIES));
		assertEquals(ItemContainerContents.EMPTY, existingTargetStack.get(ModCoreDataComponents.COOKING_INVENTORY));
		assertTrue(existingTargetData.isEmpty());

		CompoundTag malformedPrimaryData = new CompoundTag();
		malformedPrimaryData.put("smeltingInventory", oldItemStack());
		malformedPrimaryData.put("cookingInventory", inventoryWithItem(3, 1));
		assertFalse(LegacyUpgradeDataMigration.migrateCookingInventory(new ItemStack(Items.STICK), malformedPrimaryData, REGISTRIES));
		assertTrue(malformedPrimaryData.contains("smeltingInventory"));
		assertTrue(malformedPrimaryData.contains("cookingInventory"));
	}

	@Test
	void migrateCookingValuesParsesRecipesAndPreservesMalformedData() {
		CompoundTag cookingData = new CompoundTag();
		cookingData.putFloat("storedXp", 2.5F);
		CompoundTag recipesUsed = new CompoundTag();
		recipesUsed.putInt("minecraft:iron_ingot", 3);
		cookingData.put("recipesUsed", recipesUsed);
		cookingData.putLong("burnTimeFinish", 10L);
		cookingData.putInt("burnTimeTotal", 20);
		cookingData.putLong("cookTimeFinish", 30L);
		cookingData.putInt("cookTimeTotal", 40);
		cookingData.putBoolean("isCooking", true);
		ItemStack cookingStack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(cookingStack, cookingData, "storedXp", ModCoreDataComponents.STORED_XP.get(),
				LegacyUpgradeDataMigration::parseFloat));
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(cookingStack, cookingData, "recipesUsed", ModCoreDataComponents.RECIPES_USED.get(),
				LegacyUpgradeDataMigration::parseRecipesUsed));
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(cookingStack, cookingData, "burnTimeFinish", ModCoreDataComponents.BURN_TIME_FINISH.get(),
				LegacyUpgradeDataMigration::parseLong));
		assertTrue(migrateInteger(cookingStack, cookingData, "burnTimeTotal", ModCoreDataComponents.BURN_TIME_TOTAL.get()));
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(cookingStack, cookingData, "cookTimeFinish", ModCoreDataComponents.COOK_TIME_FINISH.get(),
				LegacyUpgradeDataMigration::parseLong));
		assertTrue(migrateInteger(cookingStack, cookingData, "cookTimeTotal", ModCoreDataComponents.COOK_TIME_TOTAL.get()));
		assertTrue(migrateBoolean(cookingStack, cookingData, "isCooking", ModCoreDataComponents.IS_COOKING.get()));
		assertEquals(2.5F, cookingStack.get(ModCoreDataComponents.STORED_XP));
		assertEquals(Map.of(net.minecraft.resources.ResourceLocation.withDefaultNamespace("iron_ingot"), 3),
				cookingStack.get(ModCoreDataComponents.RECIPES_USED));
		assertEquals(10L, cookingStack.get(ModCoreDataComponents.BURN_TIME_FINISH));
		assertEquals(20, cookingStack.get(ModCoreDataComponents.BURN_TIME_TOTAL));
		assertEquals(30L, cookingStack.get(ModCoreDataComponents.COOK_TIME_FINISH));
		assertEquals(40, cookingStack.get(ModCoreDataComponents.COOK_TIME_TOTAL));
		assertTrue(cookingStack.get(ModCoreDataComponents.IS_COOKING));

		CompoundTag malformedRecipes = new CompoundTag();
		CompoundTag invalidRecipes = new CompoundTag();
		invalidRecipes.putInt("minecraft:iron_ingot", -1);
		malformedRecipes.put("recipesUsed", invalidRecipes);
		assertFalse(LegacyUpgradeDataMigration.migrateComponent(new ItemStack(Items.STICK), malformedRecipes, "recipesUsed",
				ModCoreDataComponents.RECIPES_USED.get(), LegacyUpgradeDataMigration::parseRecipesUsed));
		assertTrue(malformedRecipes.contains("recipesUsed"));
	}

	@Test
	void migrateCookingFiltersAreAtomicAndCookingClassesAreAudited() {
		CompoundTag filterData = rootFilterData();
		filterData.put("filters", emptyInventory());
		ItemStack stack = new ItemStack(Items.STICK);
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(stack, filterData, "filters", ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES.get(),
				tag -> LegacyUpgradeDataMigration.parseFilterAttributes(tag, REGISTRIES)));

		CompoundTag nestedFilterData = new CompoundTag();
		nestedFilterData.put("inputFilter", rootFilterData());
		nestedFilterData.put("fuelFilter", rootFilterData());
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(stack, nestedFilterData, "inputFilter", ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES.get(),
				tag -> LegacyUpgradeDataMigration.parseFilterAttributes(tag, REGISTRIES)));
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(stack, nestedFilterData, "fuelFilter", ModCoreDataComponents.FUEL_FILTER_ATTRIBUTES.get(),
				tag -> LegacyUpgradeDataMigration.parseFilterAttributes(tag, REGISTRIES)));
		assertFalse(nestedFilterData.contains("inputFilter"));
		assertFalse(nestedFilterData.contains("fuelFilter"));

		CompoundTag malformedFilterData = new CompoundTag();
		CompoundTag malformedFilter = rootFilterData();
		malformedFilter.putString("matchNbt", "true");
		malformedFilterData.put("inputFilter", malformedFilter);
		assertFalse(LegacyUpgradeDataMigration.migrateComponent(new ItemStack(Items.STICK), malformedFilterData, "inputFilter",
				ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES.get(), tag -> LegacyUpgradeDataMigration.parseFilterAttributes(tag, REGISTRIES)));
		assertTrue(malformedFilterData.contains("inputFilter"));

		assertTrue(ICookingUpgradeItem.class.isAssignableFrom(SmeltingUpgradeItem.class));
		assertTrue(ICookingUpgradeItem.class.isAssignableFrom(SmokingUpgradeItem.class));
		assertTrue(ICookingUpgradeItem.class.isAssignableFrom(BlastingUpgradeItem.class));
		assertTrue(IAutoCookingUpgradeItem.class.isAssignableFrom(AutoSmeltingUpgradeItem.class));
		assertTrue(IAutoCookingUpgradeItem.class.isAssignableFrom(AutoSmokingUpgradeItem.class));
		assertTrue(IAutoCookingUpgradeItem.class.isAssignableFrom(AutoBlastingUpgradeItem.class));
	}

	@Test
	void migrateAutoCookingFiltersUseAllowListDefault() {
		ItemStack stack = stackWithUpgradeItem(mock(AutoSmeltingUpgradeItem.class));
		CompoundTag legacyData = new CompoundTag();
		CompoundTag inputFilter = new CompoundTag();
		inputFilter.put("filters", emptyInventory());
		legacyData.put("inputFilter", inputFilter);
		CompoundTag fuelFilter = new CompoundTag();
		fuelFilter.put("filters", emptyInventory());
		legacyData.put("fuelFilter", fuelFilter);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));

		assertTrue(LegacyUpgradeDataMigration.migrate(stack, REGISTRIES));
		assertTrue(stack.get(ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES).isAllowList());
		assertTrue(stack.get(ModCoreDataComponents.FUEL_FILTER_ATTRIBUTES).isAllowList());
	}

	@Test
	void inventoryMigrationDispatchClassesAreAudited() {
		assertTrue(IUpgradeItem.class.isAssignableFrom(BatteryUpgradeItem.class));
		assertTrue(IUpgradeItem.class.isAssignableFrom(CraftingUpgradeItem.class));
		assertTrue(IUpgradeItem.class.isAssignableFrom(BlockConverterUpgradeItem.class));
		assertTrue(BlockConverterUpgradeItem.class.isAssignableFrom(StonecutterUpgradeItem.class));
		assertTrue(IUpgradeItem.class.isAssignableFrom(JukeboxUpgradeItem.class));
	}

	@Test
	void coreMigrationRegistryCoversEveryMigrationFamilyAndInheritedTypes() {
		List<Item> upgradeItems = List.of(mock(CompactingUpgradeItem.class), mock(FeedingUpgradeItem.class), mock(FilterUpgradeItem.class),
				mock(PickupUpgradeItem.class), mock(MagnetUpgradeItem.class), mock(VoidUpgradeItem.class), mock(BatteryUpgradeItem.class),
				mock(CraftingUpgradeItem.class), mock(StonecutterUpgradeItem.class), mock(JukeboxUpgradeItem.class), mock(SmeltingUpgradeItem.class),
				mock(AutoSmeltingUpgradeItem.class), mock(XpPumpUpgradeItem.class), mock(TankUpgradeItem.class), mock(PumpUpgradeItem.class),
				mock(AlchemyUpgradeItem.class));

		upgradeItems.forEach(item -> assertNotNull(LegacyUpgradeDataMigration.getCoreMigrator(item)));
	}

	@Test
	void migrateChippedBlockTransformationDataThroughCompatRegisteredMigrator() {
		LegacyUpgradeDataMigration.registerBlockTransformationMigrator(TestBlockTransformationUpgradeItem.class);
		ItemStack stack = stackWithUpgradeItem(mock(TestBlockTransformationUpgradeItem.class));
		CompoundTag legacyData = new CompoundTag();
		legacyData.put("input", oldItemStack("minecraft:stone"));
		legacyData.put("result", oldItemStack("minecraft:dirt"));
		legacyData.putBoolean("shiftClickIntoStorage", false);
		legacyData.putBoolean("refill_input", true);
		legacyData.putString("unknown", "preserved");
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));

		assertTrue(LegacyUpgradeDataMigration.migrate(stack, REGISTRIES));
		assertTrue(stack.get(ModCoreDataComponents.INPUT_ITEM).is(Items.STONE));
		assertTrue(stack.get(ModCoreDataComponents.RESULT_ITEM).is(Items.DIRT));
		assertFalse(stack.get(ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE));
		assertTrue(stack.get(ModCoreDataComponents.REFILL_INPUT));
		assertEquals("preserved", stack.get(DataComponents.CUSTOM_DATA).copyTag().getString("unknown"));
		assertFalse(LegacyUpgradeDataMigration.migrate(stack, REGISTRIES));

		ItemStack existingStack = stackWithUpgradeItem(mock(TestBlockTransformationUpgradeItem.class));
		existingStack.set(ModCoreDataComponents.INPUT_ITEM, SimpleItemContent.copyOf(new ItemStack(Items.DIAMOND)));
		existingStack.set(ModCoreDataComponents.RESULT_ITEM, SimpleItemContent.copyOf(new ItemStack(Items.GOLD_INGOT)));
		existingStack.set(ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE, true);
		existingStack.set(ModCoreDataComponents.REFILL_INPUT, false);
		CompoundTag staleData = new CompoundTag();
		staleData.putString("input", "invalid");
		staleData.putString("result", "invalid");
		staleData.putString("shiftClickIntoStorage", "invalid");
		staleData.putString("refill_input", "invalid");
		existingStack.set(DataComponents.CUSTOM_DATA, CustomData.of(staleData));

		assertTrue(LegacyUpgradeDataMigration.migrate(existingStack, REGISTRIES));
		assertTrue(existingStack.get(ModCoreDataComponents.INPUT_ITEM).is(Items.DIAMOND));
		assertTrue(existingStack.get(ModCoreDataComponents.RESULT_ITEM).is(Items.GOLD_INGOT));
		assertTrue(existingStack.get(ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE));
		assertFalse(existingStack.get(ModCoreDataComponents.REFILL_INPUT));
		assertFalse(existingStack.has(DataComponents.CUSTOM_DATA));

		CompoundTag malformedData = new CompoundTag();
		malformedData.putString("input", "invalid");
		malformedData.putString("result", "invalid");
		malformedData.putString("shiftClickIntoStorage", "invalid");
		malformedData.putString("refill_input", "invalid");
		ItemStack malformedStack = stackWithUpgradeItem(mock(TestBlockTransformationUpgradeItem.class));
		malformedStack.set(DataComponents.CUSTOM_DATA, CustomData.of(malformedData));
		assertFalse(LegacyUpgradeDataMigration.migrate(malformedStack, REGISTRIES));
		assertEquals(4, malformedStack.get(DataComponents.CUSTOM_DATA).copyTag().size());
	}

	@Test
	void alchemyMigrationDispatchClassIsAudited() {
		assertTrue(IUpgradeItem.class.isAssignableFrom(AlchemyUpgradeItem.class));
	}

	@Test
	void migrateMapsAlchemyFieldsAndRecords() {
		ItemStack stack = new ItemStack(Items.STICK);
		CompoundTag legacyData = new CompoundTag();
		legacyData.put("alchemyFilterAttributes", legacyAlchemyAttributes());
		legacyData.putBoolean("matchAllEffects", false);
		legacyData.putBoolean("matchEffectDuration", false);
		legacyData.putBoolean("matchEffectAmplifier", false);
		legacyData.putString("entityMatch", "entities");
		legacyData.putString("unrelated", "preserved");
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(stack, legacyData, "alchemyFilterAttributes",
				ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES.get(), tag -> LegacyUpgradeDataMigration.parseAlchemyFilterAttributes(tag, REGISTRIES)));
		assertTrue(migrateBoolean(stack, legacyData, "matchAllEffects", ModCoreDataComponents.MATCH_ALL_EFFECTS.get()));
		assertTrue(migrateBoolean(stack, legacyData, "matchEffectDuration", ModCoreDataComponents.MATCH_EFFECT_DURATION.get()));
		assertTrue(migrateBoolean(stack, legacyData, "matchEffectAmplifier", ModCoreDataComponents.MATCH_EFFECT_AMPLIFIER.get()));
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(stack, legacyData, "entityMatch", ModCoreDataComponents.ENTITY_MATCH.get(),
				LegacyUpgradeDataMigration::parseEntityMatch));

		List<AlchemyFilterAttribute> attributes = stack.get(ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES);
		assertEquals(2, attributes.size());
		assertEquals(Items.STONE, attributes.get(0).filter().getItem());
		assertEquals(AlchemyCondition.HURT, attributes.get(0).condition());
		assertEquals(.75F, attributes.get(0).value());
		assertEquals(Items.DIRT, attributes.get(1).filter().getItem());
		assertEquals(AlchemyCondition.ALWAYS, attributes.get(1).condition());
		assertEquals(2F, attributes.get(1).value());
		assertFalse(stack.get(ModCoreDataComponents.MATCH_ALL_EFFECTS));
		assertFalse(stack.get(ModCoreDataComponents.MATCH_EFFECT_DURATION));
		assertFalse(stack.get(ModCoreDataComponents.MATCH_EFFECT_AMPLIFIER));
		assertEquals(EntityMatch.ENTITIES, stack.get(ModCoreDataComponents.ENTITY_MATCH));
		assertEquals("preserved", legacyData.getString("unrelated"));
		assertFalse(legacyData.contains("alchemyFilterAttributes"));
		assertFalse(legacyData.contains("matchAllEffects"));
		assertFalse(legacyData.contains("matchEffectDuration"));
		assertFalse(legacyData.contains("matchEffectAmplifier"));
		assertFalse(legacyData.contains("entityMatch"));
		assertFalse(LegacyUpgradeDataMigration.migrateComponent(stack, legacyData, "alchemyFilterAttributes",
				ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES.get(), tag -> LegacyUpgradeDataMigration.parseAlchemyFilterAttributes(tag, REGISTRIES)));
	}

	@Test
	void migrateAlchemyAcceptsSerializedUnusedSlotsAndDropsLegacyDataForExistingComponents() {
		ItemStack stack = new ItemStack(Items.STICK);
		stack.set(ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES,
				List.of(new AlchemyFilterAttribute(new ItemStack(Items.DIAMOND), AlchemyCondition.ON_FIRE, 4F)));
		stack.set(ModCoreDataComponents.MATCH_ALL_EFFECTS, false);
		stack.set(ModCoreDataComponents.MATCH_EFFECT_DURATION, false);
		stack.set(ModCoreDataComponents.MATCH_EFFECT_AMPLIFIER, false);
		stack.set(ModCoreDataComponents.ENTITY_MATCH, EntityMatch.ENTITIES);
		CompoundTag legacyData = new CompoundTag();
		ListTag attributes = new ListTag();
		attributes.add(legacyAlchemyAttribute("minecraft:stone", "hurt", .75F));
		attributes.add(legacyAlchemyAttribute("minecraft:air", "never", -1F, (byte) 1));
		List<AlchemyFilterAttribute> parsedAttributes = LegacyUpgradeDataMigration.parseAlchemyFilterAttributes(attributes, REGISTRIES).orElseThrow();
		assertEquals(Items.STONE, parsedAttributes.getFirst().filter().getItem());
		AlchemyFilterAttribute unusedSlot = parsedAttributes.get(1);
		assertTrue(unusedSlot.filter().isEmpty());
		assertEquals(AlchemyCondition.NEVER, unusedSlot.condition());
		assertEquals(-1F, unusedSlot.value());
		legacyData.put("alchemyFilterAttributes", attributes);
		legacyData.putBoolean("matchAllEffects", true);
		legacyData.putBoolean("matchEffectDuration", true);
		legacyData.putBoolean("matchEffectAmplifier", true);
		legacyData.putString("entityMatch", "players");
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(stack, legacyData, "alchemyFilterAttributes",
				ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES.get(), tag -> LegacyUpgradeDataMigration.parseAlchemyFilterAttributes(tag, REGISTRIES)));
		assertTrue(migrateBoolean(stack, legacyData, "matchAllEffects", ModCoreDataComponents.MATCH_ALL_EFFECTS.get()));
		assertTrue(migrateBoolean(stack, legacyData, "matchEffectDuration", ModCoreDataComponents.MATCH_EFFECT_DURATION.get()));
		assertTrue(migrateBoolean(stack, legacyData, "matchEffectAmplifier", ModCoreDataComponents.MATCH_EFFECT_AMPLIFIER.get()));
		assertTrue(LegacyUpgradeDataMigration.migrateComponent(stack, legacyData, "entityMatch", ModCoreDataComponents.ENTITY_MATCH.get(),
				LegacyUpgradeDataMigration::parseEntityMatch));

		AlchemyFilterAttribute currentAttribute = stack.get(ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES).getFirst();
		assertEquals(Items.DIAMOND, currentAttribute.filter().getItem());
		assertEquals(AlchemyCondition.ON_FIRE, currentAttribute.condition());
		assertEquals(4F, currentAttribute.value());
		assertFalse(stack.get(ModCoreDataComponents.MATCH_ALL_EFFECTS));
		assertFalse(stack.get(ModCoreDataComponents.MATCH_EFFECT_DURATION));
		assertFalse(stack.get(ModCoreDataComponents.MATCH_EFFECT_AMPLIFIER));
		assertEquals(EntityMatch.ENTITIES, stack.get(ModCoreDataComponents.ENTITY_MATCH));
		assertTrue(legacyData.isEmpty());
	}

	@Test
	void migrateAlchemyRetainsMalformedListsRecordsAndUnknownIdentifiers() {
		for (Tag invalidAttributes : List.of(StringTag.valueOf("not a list"), malformedAlchemyAttributes(), unknownAlchemyConditionAttributes(),
				unknownAlchemyFilterAttributes())) {
			ItemStack stack = new ItemStack(Items.STICK);
			CompoundTag legacyData = new CompoundTag();
			legacyData.put("alchemyFilterAttributes", invalidAttributes.copy());
			legacyData.putString("unrelated", "preserved");
			assertFalse(LegacyUpgradeDataMigration.migrateComponent(stack, legacyData, "alchemyFilterAttributes",
					ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES.get(), tag -> LegacyUpgradeDataMigration.parseAlchemyFilterAttributes(tag, REGISTRIES)));
			assertFalse(stack.has(ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES));
			assertTrue(legacyData.contains("alchemyFilterAttributes"));
			assertEquals("preserved", legacyData.getString("unrelated"));
		}
	}

	private static boolean migrateBoolean(ItemStack stack, CompoundTag data, String key, net.minecraft.core.component.DataComponentType<Boolean> component) {
		return LegacyUpgradeDataMigration.migrateComponent(stack, data, key, component, LegacyUpgradeDataMigration::parseBoolean);
	}

	private static ItemStack stackWithUpgradeItem(Item item) {
		ItemStack stack = spy(new ItemStack(Items.STICK));
		doReturn(item).when(stack).getItem();
		return stack;
	}

	private static class TestBlockTransformationUpgradeItem extends Item {
		private TestBlockTransformationUpgradeItem() {
			super(new Properties());
		}
	}

	private static boolean migrateInteger(ItemStack stack, CompoundTag data, String key, net.minecraft.core.component.DataComponentType<Integer> component) {
		return LegacyUpgradeDataMigration.migrateComponent(stack, data, key, component, LegacyUpgradeDataMigration::parseInteger);
	}

	private static CompoundTag rootFilterData() {
		CompoundTag data = new CompoundTag();
		data.putBoolean("isAllowList", true);
		return data;
	}

	private static CompoundTag emptyInventory() {
		CompoundTag inventory = new CompoundTag();
		inventory.putInt("Size", 0);
		inventory.put("Items", new ListTag());
		return inventory;
	}

	private static CompoundTag inventoryWithItem(int size, int slot) {
		CompoundTag inventory = new CompoundTag();
		inventory.putInt("Size", size);
		ListTag items = new ListTag();
		CompoundTag item = oldItemStack();
		item.putInt("Slot", slot);
		items.add(item);
		inventory.put("Items", items);
		return inventory;
	}

	private static CompoundTag oldItemStack() {
		return oldItemStack("minecraft:stone");
	}

	private static CompoundTag oldItemStack(String itemId) {
		CompoundTag item = new CompoundTag();
		item.putString("id", itemId);
		item.putByte("Count", (byte) 1);
		return item;
	}

	private static CompoundTag legacyFluid(String fluidName, int amount) {
		CompoundTag fluid = new CompoundTag();
		fluid.putString("FluidName", fluidName);
		fluid.putInt("Amount", amount);
		return fluid;
	}

	private static ListTag legacyFluidFilters(CompoundTag... filters) {
		ListTag result = new ListTag();
		for (CompoundTag filter : filters) {
			result.add(filter);
		}
		return result;
	}

	private static ListTag legacyAlchemyAttributes() {
		ListTag attributes = new ListTag();
		attributes.add(legacyAlchemyAttribute("minecraft:stone", "hurt", .75F));
		CompoundTag secondAttribute = legacyAlchemyAttribute("minecraft:dirt", "always", 0F);
		secondAttribute.putInt("value", 2);
		attributes.add(secondAttribute);
		return attributes;
	}

	private static CompoundTag legacyAlchemyAttribute(String itemId, String condition, float value) {
		return legacyAlchemyAttribute(itemId, condition, value, (byte) 1);
	}

	private static CompoundTag legacyAlchemyAttribute(String itemId, String condition, float value, byte count) {
		CompoundTag attribute = new CompoundTag();
		CompoundTag filter = oldItemStack();
		filter.putString("id", itemId);
		filter.putByte("Count", count);
		attribute.put("filter", filter);
		attribute.putString("condition", condition);
		attribute.putFloat("value", value);
		return attribute;
	}

	private static ListTag malformedAlchemyAttributes() {
		ListTag attributes = new ListTag();
		CompoundTag attribute = legacyAlchemyAttribute("minecraft:stone", "hurt", .75F);
		attribute.putString("value", "not numeric");
		attributes.add(attribute);
		return attributes;
	}

	private static ListTag unknownAlchemyConditionAttributes() {
		ListTag attributes = new ListTag();
		attributes.add(legacyAlchemyAttribute("minecraft:stone", "unknown", .75F));
		return attributes;
	}

	private static ListTag unknownAlchemyFilterAttributes() {
		ListTag attributes = new ListTag();
		attributes.add(legacyAlchemyAttribute("example:unknown", "hurt", .75F));
		return attributes;
	}

}
