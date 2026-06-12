package net.p3pp3rf1y.sophisticatedcore.upgrades.compacting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilteredUpgradeConfigBase;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompactingUpgradeConfig extends FilteredUpgradeConfigBase {
	private static final String COMPACTING_SHAPE_OVERRIDE_MATCHER = "([a-z0-9_.-]+:[a-z0-9_/.-]+)=([01/]+)";

	public final ModConfigSpec.ConfigValue<List<? extends String>> additionalCompactingShapes;
	public final ModConfigSpec.ConfigValue<List<? extends String>> additionalCompactingShapeOverrides;

	@Nullable
	private List<RecipeHelper.CompactingRecipeShape> additionalCompactingShapesList = null;
	@Nullable
	private Map<Item, List<RecipeHelper.CompactingRecipeShape>> additionalCompactingShapeOverridesMap = null;

	public CompactingUpgradeConfig(ModConfigSpec.Builder builder, String name, String path, int defaultFilterSlots, int defaultSlotsInRow) {
		super(builder, name, path, defaultFilterSlots, defaultSlotsInRow);
		additionalCompactingShapes = builder.comment("List of additional crafting shapes compacting and compression upgrades will try when checking if items can be compacted. "
				+ "Shapes are expected as rows of 1s and 0s separated by / where 1 means the tested item is present and 0 means the slot is empty. "
				+ "Example: \"111/101/111\" checks a hollow 3x3 shape with 8 items.")
				.defineList("additionalCompactingShapes", () -> List.of("111/101/111"), () -> "111/101/111", shape -> shape instanceof String str && RecipeHelper.CompactingRecipeShape.parse(str).isPresent());
		additionalCompactingShapeOverrides = builder.comment("List of item-specific additional crafting shapes compacting and compression upgrades will try when checking if items can be compacted. "
				+ "Entries are expected in format of \"mod:item=shape\", for example \"some_mod:tiny_coal=111/101/111\".")
				.defineList("additionalCompactingShapeOverrides", () -> List.of(), () -> "some_mod:tiny_coal=111/101/111", shapeOverride -> shapeOverride instanceof String str && isCompactingShapeOverrideValid(str));
		builder.pop();
	}

	public Optional<CompactingDefinition> getCompactingResult(ItemStack stack, int maxWidth, int maxHeight) {
		return getCompactingResult(stack, maxWidth, maxHeight, (result, count) -> false);
	}

	public Optional<CompactingDefinition> getCompactingResult(ItemStack stack, int maxWidth, int maxHeight, BiPredicate<ItemStack, Integer> additionalResultValidator) {
		Item item = stack.getItem();
		for (RecipeHelper.CompactingRecipeShape shape : getAdditionalCompactingShapeOverrides().getOrDefault(item, List.of())) {
			Optional<CompactingDefinition> compactingResult = getCompactingResult(stack, shape, maxWidth, maxHeight, additionalResultValidator);
			if (compactingResult.isPresent()) {
				return compactingResult;
			}
		}

		for (RecipeHelper.CompactingRecipeShape shape : getAdditionalCompactingShapes()) {
			Optional<CompactingDefinition> compactingResult = getCompactingResult(stack, shape, maxWidth, maxHeight, additionalResultValidator);
			if (compactingResult.isPresent()) {
				return compactingResult;
			}
		}

		return Optional.empty();
	}

	public Optional<UncompactingDefinition> getUncompactingResult(ItemStack stack, int maxWidth, int maxHeight) {
		for (ItemStack result : RecipeHelper.getUncompactResultItems(stack)) {
			Optional<UncompactingDefinition> uncompactingResult = getUncompactingResult(stack, result, maxWidth, maxHeight);
			if (uncompactingResult.isPresent()) {
				return uncompactingResult;
			}
		}

		return Optional.empty();
	}

	private Optional<UncompactingDefinition> getUncompactingResult(ItemStack stack, ItemStack result, int maxWidth, int maxHeight) {
		if (result.isEmpty() || result.getCount() <= 1) {
			return Optional.empty();
		}

		ItemStack normalizedResult = result.copyWithCount(1);
		for (RecipeHelper.CompactingRecipeShape shape : getAdditionalCompactingShapeOverrides().getOrDefault(normalizedResult.getItem(), List.of())) {
			Optional<UncompactingDefinition> uncompactingResult = getUncompactingResult(stack, normalizedResult, result.getCount(), shape, maxWidth, maxHeight);
			if (uncompactingResult.isPresent()) {
				return uncompactingResult;
			}
		}

		for (RecipeHelper.CompactingRecipeShape shape : getAdditionalCompactingShapes()) {
			Optional<UncompactingDefinition> uncompactingResult = getUncompactingResult(stack, normalizedResult, result.getCount(), shape, maxWidth, maxHeight);
			if (uncompactingResult.isPresent()) {
				return uncompactingResult;
			}
		}

		return Optional.empty();
	}

	private Optional<UncompactingDefinition> getUncompactingResult(ItemStack stack, ItemStack result, int count, RecipeHelper.CompactingRecipeShape shape, int maxWidth, int maxHeight) {
		if (!shape.fitsWithin(maxWidth, maxHeight) || shape.ingredientCount() != count) {
			return Optional.empty();
		}

		RecipeHelper.CompactingResult compactingResult = RecipeHelper.getCompactingResult(result, shape);
		return ItemStack.isSameItemSameComponents(compactingResult.getResult(), stack) ? Optional.of(new UncompactingDefinition(result, count)) : Optional.empty();
	}

	private Optional<CompactingDefinition> getCompactingResult(ItemStack stack, RecipeHelper.CompactingRecipeShape shape, int maxWidth, int maxHeight, BiPredicate<ItemStack, Integer> additionalResultValidator) {
		if (!shape.fitsWithin(maxWidth, maxHeight)) {
			return Optional.empty();
		}

		RecipeHelper.CompactingResult compactingResult = RecipeHelper.getCompactingResult(stack, shape);
		ItemStack result = compactingResult.getResult();
		if (result.isEmpty() || ItemStack.isSameItemSameComponents(result, stack)) {
			return Optional.empty();
		}

		if (RecipeHelper.doesUncompactMatch(result, stack, shape.ingredientCount()) || additionalResultValidator.test(result, shape.ingredientCount())) {
			return Optional.of(new CompactingDefinition(compactingResult, shape.ingredientCount()));
		}
		return Optional.empty();
	}

	private List<RecipeHelper.CompactingRecipeShape> getAdditionalCompactingShapes() {
		if (additionalCompactingShapesList == null) {
			additionalCompactingShapesList = additionalCompactingShapes.get().stream()
					.map(RecipeHelper.CompactingRecipeShape::parse)
					.flatMap(Optional::stream)
					.toList();
		}
		return additionalCompactingShapesList;
	}

	private Map<Item, List<RecipeHelper.CompactingRecipeShape>> getAdditionalCompactingShapeOverrides() {
		if (additionalCompactingShapeOverridesMap == null) {
			additionalCompactingShapeOverridesMap = new HashMap<>();
			Pattern pattern = Pattern.compile(COMPACTING_SHAPE_OVERRIDE_MATCHER);
			additionalCompactingShapeOverrides.get().forEach(shapeOverride -> {
				Matcher matcher = pattern.matcher(shapeOverride);
				if (matcher.matches()) {
					Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(matcher.group(1)));
					if (item != Items.AIR) {
						RecipeHelper.CompactingRecipeShape.parse(matcher.group(2)).ifPresent(shape -> additionalCompactingShapeOverridesMap.computeIfAbsent(item, k -> new ArrayList<>()).add(shape));
					}
				}
			});
		}
		return additionalCompactingShapeOverridesMap;
	}

	private static boolean isCompactingShapeOverrideValid(String shapeOverride) {
		Matcher matcher = Pattern.compile(COMPACTING_SHAPE_OVERRIDE_MATCHER).matcher(shapeOverride);
		return matcher.matches() && RecipeHelper.CompactingRecipeShape.parse(matcher.group(2)).isPresent();
	}

	public void clearCache() {
		additionalCompactingShapesList = null;
		additionalCompactingShapeOverridesMap = null;
	}

	public record CompactingDefinition(RecipeHelper.CompactingResult result, int count) {
	}

	public record UncompactingDefinition(ItemStack result, int count) {
		public UncompactingDefinition {
			result = result.copyWithCount(1);
		}
	}
}
