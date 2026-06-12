package net.p3pp3rf1y.sophisticatedcore.upgrades.compacting;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.p3pp3rf1y.sophisticatedcore.HelperAssertions.assertStackEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

class CompactingUpgradeConfigTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void getsUncompactingResultForConfiguredDonutShape() throws NoSuchFieldException, IllegalAccessException {
		RecipeHelper.CompactingRecipeShape donutShape = RecipeHelper.CompactingRecipeShape.parse("111/101/111").orElseThrow();
		CompactingUpgradeConfig config = getConfigWithCachedShape(donutShape);
		ItemStack compactedStack = new ItemStack(Items.ECHO_SHARD);
		ItemStack uncompactedStack = new ItemStack(Items.POISONOUS_POTATO, 8);

		try (MockedStatic<RecipeHelper> recipeHelper = Mockito.mockStatic(RecipeHelper.class, Mockito.CALLS_REAL_METHODS)) {
			recipeHelper.when(() -> RecipeHelper.getUncompactResultItems(stackOf(Items.ECHO_SHARD))).thenReturn(List.of(uncompactedStack));
			recipeHelper.when(() -> RecipeHelper.getCompactingResult(stackOf(Items.POISONOUS_POTATO), eq(donutShape)))
					.thenReturn(new RecipeHelper.CompactingResult(compactedStack, Collections.emptyList()));

			Optional<CompactingUpgradeConfig.UncompactingDefinition> uncompactingResult = config.getUncompactingResult(compactedStack, 3, 3);

			assertTrue(uncompactingResult.isPresent(), "Configured donut shape should support recipe-backed uncompacting");
			CompactingUpgradeConfig.UncompactingDefinition result = uncompactingResult.orElseThrow();
			assertEquals(8, result.count());
			assertStackEquals(new ItemStack(Items.POISONOUS_POTATO), result.result(), "Configured donut shape should uncompact back to the recipe result");
		}
	}

	private static CompactingUpgradeConfig getConfigWithCachedShape(RecipeHelper.CompactingRecipeShape shape) throws NoSuchFieldException, IllegalAccessException {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		CompactingUpgradeConfig config = new CompactingUpgradeConfig(builder, "Compacting Upgrade", "compactingUpgrade", 9, 3);
		builder.build();
		setCachedShapes(config, shape);
		return config;
	}

	private static ItemStack stackOf(Item item) {
		return argThat(stack -> stack != null && stack.getItem() == item);
	}

	private static void setCachedShapes(CompactingUpgradeConfig config, RecipeHelper.CompactingRecipeShape shape) throws NoSuchFieldException, IllegalAccessException {
		java.lang.reflect.Field additionalCompactingShapesList = CompactingUpgradeConfig.class.getDeclaredField("additionalCompactingShapesList");
		additionalCompactingShapesList.setAccessible(true);
		additionalCompactingShapesList.set(config, List.of(shape));

		java.lang.reflect.Field additionalCompactingShapeOverridesMap = CompactingUpgradeConfig.class.getDeclaredField("additionalCompactingShapeOverridesMap");
		additionalCompactingShapeOverridesMap.setAccessible(true);
		additionalCompactingShapeOverridesMap.set(config, Map.of());
	}
}
