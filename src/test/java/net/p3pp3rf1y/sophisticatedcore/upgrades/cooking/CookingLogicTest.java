package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CookingLogicTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		Items.COAL.builtInRegistryHolder().bindComponents(components);
		Items.DIAMOND.builtInRegistryHolder().bindComponents(components);
		Items.FURNACE.builtInRegistryHolder().bindComponents(components);
	}

	@Test
	void smeltPreservesInputDerivedResultData() throws ReflectiveOperationException {
		ItemStack input = markedInput();
		CookingLogic<SmeltingRecipe> cookingLogic = cookingLogic(input);

		smelt(cookingLogic, new InputAwareSmeltingRecipe());

		ItemStack output = cookingLogic.getCookOutput();
		assertEquals("marker", output.get(DataComponents.CUSTOM_DATA).copyTag().getString("inputMarker").orElseThrow());
		assertTrue(output.has(DataComponents.CUSTOM_NAME));
		assertEquals(input.getHoverName(), output.getHoverName());
	}

	@Test
	void smeltRejectsIncompatibleSameItemOutputWithoutConsumingInput() throws ReflectiveOperationException {
		ItemStack input = markedInput();
		CookingLogic<SmeltingRecipe> cookingLogic = cookingLogic(input);
		ItemStack existingOutput = new ItemStack(Items.DIAMOND);
		cookingLogic.getCookingInventory().setStackInSlot(CookingLogic.COOK_OUTPUT_SLOT, existingOutput);

		smelt(cookingLogic, new InputAwareSmeltingRecipe());

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

		smelt(cookingLogic, new InputAwareSmeltingRecipe());

		assertTrue(cookingLogic.getCookInput().isEmpty());
		assertEquals(5, cookingLogic.getCookOutput().getCount());
		assertEquals("marker", cookingLogic.getCookOutput().get(DataComponents.CUSTOM_DATA).copyTag().getString("inputMarker").orElseThrow());
	}

	@Test
	void smeltConsumesFinalInputBeforeCanSmeltSkipsAssembly() throws ReflectiveOperationException {
		CookingLogic<SmeltingRecipe> cookingLogic = cookingLogic(markedInput());
		InputAwareSmeltingRecipe recipe = new InputAwareSmeltingRecipe();

		smelt(cookingLogic, recipe);
		boolean canSmelt = cookingLogic.canSmelt(recipe);

		assertFalse(canSmelt);
		assertEquals(1, recipe.getAssemblyCalls());
	}

	private static CookingLogic<SmeltingRecipe> cookingLogic(ItemStack input) throws ReflectiveOperationException {
		ModConfigSpec.Builder configBuilder = new ModConfigSpec.Builder();
		CookingUpgradeConfig config = new CookingUpgradeConfig(configBuilder, "Test", "test");
		configBuilder.build();
		setConfigValue(config, "cookingSpeedMultiplier");
		setConfigValue(config, "fuelEfficiencyMultiplier");
		CookingLogic<SmeltingRecipe> cookingLogic = new CookingLogic<>(new ItemStack(Items.FURNACE), stack -> {
		}, stack -> true, stack -> true, config, RecipeType.SMELTING, 1.0F);
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
		data.putString("inputMarker", input.get(DataComponents.CUSTOM_DATA).copyTag().getString("marker").orElseThrow());
		result.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
		result.set(DataComponents.CUSTOM_NAME, input.getHoverName());
		return result;
	}

	private static void smelt(CookingLogic<SmeltingRecipe> cookingLogic, SmeltingRecipe recipe) throws ReflectiveOperationException {
		Method smelt = CookingLogic.class.getDeclaredMethod("smelt", AbstractCookingRecipe.class);
		smelt.setAccessible(true);
		smelt.invoke(cookingLogic, recipe);
	}

	private static class InputAwareSmeltingRecipe extends SmeltingRecipe {
		private int assemblyCalls = 0;

		private InputAwareSmeltingRecipe() {
			super(new Recipe.CommonInfo(true), new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.MISC, ""), Ingredient.of(Items.COAL),
					ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.DIAMOND, 2)), 0, 200);
		}

		@Override
		public ItemStack assemble(SingleRecipeInput input) {
			assemblyCalls++;
			return assembledResult(input.item());
		}

		private int getAssemblyCalls() {
			return assemblyCalls;
		}
	}
}
