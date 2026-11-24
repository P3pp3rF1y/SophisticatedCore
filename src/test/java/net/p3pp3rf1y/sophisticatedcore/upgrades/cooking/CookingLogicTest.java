package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CookingLogicTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void smeltPreservesInputDerivedResultData() throws ReflectiveOperationException {
		ItemStack input = markedInput();
		CookingLogic<SmeltingRecipe> cookingLogic = cookingLogic(input);

		smelt(cookingLogic, new InputAwareSmeltingRecipe(), level());

		ItemStack output = cookingLogic.getCookOutput();
		assertEquals("marker", output.get(DataComponents.CUSTOM_DATA).copyTag().getString("inputMarker"));
		assertTrue(output.has(DataComponents.CUSTOM_NAME));
		assertEquals(input.getHoverName(), output.getHoverName());
	}

	@Test
	void smeltRejectsIncompatibleSameItemOutputWithoutConsumingInput() throws ReflectiveOperationException {
		ItemStack input = markedInput();
		CookingLogic<SmeltingRecipe> cookingLogic = cookingLogic(input);
		ItemStack existingOutput = new ItemStack(Items.DIAMOND);
		cookingLogic.getCookingInventory().setStackInSlot(CookingLogic.COOK_OUTPUT_SLOT, existingOutput);

		smelt(cookingLogic, new InputAwareSmeltingRecipe(), level());

		assertEquals(1, cookingLogic.getCookInput().getCount());
		assertEquals(1, cookingLogic.getCookOutput().getCount());
		assertFalse(cookingLogic.getCookOutput().has(DataComponents.CUSTOM_DATA));
	}

	@Test
	void smeltGrowsCompatibleSameStateOutputWithDifferentCount() throws ReflectiveOperationException {
		ItemStack input = markedInput();
		CookingLogic<SmeltingRecipe> cookingLogic = cookingLogic(input);
		ItemStack existingOutput = assembledResult(input);
		existingOutput.setCount(3);
		cookingLogic.getCookingInventory().setStackInSlot(CookingLogic.COOK_OUTPUT_SLOT, existingOutput);

		smelt(cookingLogic, new InputAwareSmeltingRecipe(), level());

		assertTrue(cookingLogic.getCookInput().isEmpty());
		assertEquals(5, cookingLogic.getCookOutput().getCount());
		assertEquals("marker", cookingLogic.getCookOutput().get(DataComponents.CUSTOM_DATA).copyTag().getString("inputMarker"));
	}

	@Test
	void smeltConsumesFinalInputBeforeCanSmeltSkipsAssembly() throws ReflectiveOperationException {
		CookingLogic<SmeltingRecipe> cookingLogic = cookingLogic(markedInput());
		InputAwareSmeltingRecipe recipe = new InputAwareSmeltingRecipe();
		Level level = level();

		smelt(cookingLogic, recipe, level);
		boolean canSmelt = cookingLogic.canSmelt(recipe, level);

		assertFalse(canSmelt);
		assertEquals(1, recipe.getAssemblyCalls());
	}

	@Test
	void smeltRetainsWaterBucketFromWetSpongeWhenFuelPredicateRejectsWaterBucket() throws ReflectiveOperationException {
		ItemStack upgrade = new ItemStack(Items.FURNACE);
		List<ItemStack> savedUpgrades = new ArrayList<>();
		CookingLogic<SmeltingRecipe> cookingLogic = cookingLogic(upgrade, stack -> savedUpgrades.add(stack.copy()), stack -> stack.is(Items.BUCKET),
				ItemStack.EMPTY);
		cookingLogic.setCookInput(new ItemStack(Items.WET_SPONGE));
		cookingLogic.setFuel(new ItemStack(Items.BUCKET));
		assertThrows(RuntimeException.class, () -> cookingLogic.setFuel(new ItemStack(Items.WATER_BUCKET)));
		assertTrue(cookingLogic.getFuel().is(Items.BUCKET));

		smelt(cookingLogic, new WetSpongeSmeltingRecipe(), level());

		assertTrue(cookingLogic.getCookInput().isEmpty());
		assertTrue(cookingLogic.getCookOutput().is(Items.SPONGE));
		assertTrue(cookingLogic.getFuel().is(Items.WATER_BUCKET));
		CookingLogic<SmeltingRecipe> reloadedCookingLogic = cookingLogic(savedUpgrades.get(savedUpgrades.size() - 1), stack -> {
		}, stack -> stack.is(Items.BUCKET), ItemStack.EMPTY);
		assertTrue(reloadedCookingLogic.getCookInput().isEmpty());
		assertTrue(reloadedCookingLogic.getCookOutput().is(Items.SPONGE));
		assertTrue(reloadedCookingLogic.getFuel().is(Items.WATER_BUCKET));

		smelt(cookingLogic, new WetSpongeSmeltingRecipe(), level());

		assertTrue(cookingLogic.getCookInput().isEmpty());
		assertEquals(1, cookingLogic.getCookOutput().getCount());
		assertTrue(cookingLogic.getFuel().is(Items.WATER_BUCKET));
	}

	private static CookingLogic<SmeltingRecipe> cookingLogic(ItemStack input) throws ReflectiveOperationException {
		return cookingLogic(new ItemStack(Items.FURNACE), stack -> {
		}, stack -> true, input);
	}

	private static CookingLogic<SmeltingRecipe> cookingLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, Predicate<ItemStack> isFuel, ItemStack input)
			throws ReflectiveOperationException {
		ModConfigSpec.Builder configBuilder = new ModConfigSpec.Builder();
		CookingUpgradeConfig config = new CookingUpgradeConfig(configBuilder, "Test", "test");
		configBuilder.build();
		setConfigValue(config, "cookingSpeedMultiplier");
		setConfigValue(config, "fuelEfficiencyMultiplier");
		CookingLogic<SmeltingRecipe> cookingLogic = new CookingLogic<>(upgrade, saveHandler, isFuel, stack -> true, config, RecipeType.SMELTING, 1.0F);
		cookingLogic.getCookingInventory();
		cookingLogic.setCookInput(input);
		return cookingLogic;
	}

	private static void setConfigValue(CookingUpgradeConfig config, String fieldName) throws ReflectiveOperationException {
		ModConfigSpec.DoubleValue value = mock(ModConfigSpec.DoubleValue.class);
		when(value.get()).thenReturn(1.0D);
		Field field = CookingUpgradeConfig.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(config, value);
	}

	private static ItemStack markedInput() {
		ItemStack input = new ItemStack(Items.COAL);
		CompoundTag data = new CompoundTag();
		data.putString("marker", "marker");
		input.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
		input.set(DataComponents.CUSTOM_NAME, Component.literal("Marked input"));
		return input;
	}

	private static ItemStack assembledResult(ItemStack input) {
		ItemStack result = new ItemStack(Items.DIAMOND, 2);
		CompoundTag data = new CompoundTag();
		data.putString("inputMarker", input.get(DataComponents.CUSTOM_DATA).copyTag().getString("marker"));
		result.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
		result.set(DataComponents.CUSTOM_NAME, input.getHoverName());
		return result;
	}

	private static Level level() {
		Level level = mock(Level.class);
		when(level.registryAccess()).thenReturn(mock(RegistryAccess.class));
		return level;
	}

	private static void smelt(CookingLogic<SmeltingRecipe> cookingLogic, Recipe<SingleRecipeInput> recipe, Level level) throws ReflectiveOperationException {
		Method smelt = CookingLogic.class.getDeclaredMethod("smelt", Recipe.class, Level.class);
		smelt.setAccessible(true);
		smelt.invoke(cookingLogic, recipe, level);
	}

	private static class InputAwareSmeltingRecipe extends SmeltingRecipe {
		private int assemblyCalls = 0;

		private InputAwareSmeltingRecipe() {
			super("", CookingBookCategory.MISC, Ingredient.of(Items.COAL), new ItemStack(Items.DIAMOND, 2), 0, 200);
		}

		@Override
		public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registryAccess) {
			assemblyCalls++;
			return assembledResult(input.item());
		}

		private int getAssemblyCalls() {
			return assemblyCalls;
		}
	}

	private static class WetSpongeSmeltingRecipe extends SmeltingRecipe {
		private WetSpongeSmeltingRecipe() {
			super("", CookingBookCategory.MISC, Ingredient.of(Items.WET_SPONGE), new ItemStack(Items.SPONGE), 0, 200);
		}
	}
}
