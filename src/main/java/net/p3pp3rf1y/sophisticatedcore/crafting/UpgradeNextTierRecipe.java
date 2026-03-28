package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;

import java.util.List;
import java.util.Optional;

public class UpgradeNextTierRecipe implements CraftingRecipe, IWrapperRecipe<ShapedRecipe> {
	public static final RecipeSerializer<UpgradeNextTierRecipe> SERIALIZER = RecipeWrapperSerializer.create(UpgradeNextTierRecipe::new, ShapedRecipe.SERIALIZER);
	private final ShapedRecipe compose;

	public UpgradeNextTierRecipe(ShapedRecipe compose) {
		this.compose = compose;
	}

	@Override
	public ShapedRecipe getCompose() {
		return compose;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return compose.matches(input, level);
	}

	@Override
	public ItemStack assemble(CraftingInput inv) {
		ItemStack nextTier = compose.assemble(inv);
		getUpgrade(inv).map(ItemStack::getComponentsPatch).ifPresent(nextTier::applyComponents);
		return nextTier;
	}

	private Optional<ItemStack> getUpgrade(CraftingInput inv) {
		for (int slot = 0; slot < inv.size(); slot++) {
			ItemStack slotStack = inv.getItem(slot);
			if (slotStack.getItem() instanceof IUpgradeItem) {
				return Optional.of(slotStack);
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public boolean showNotification() {
		return compose.showNotification();
	}

	@Override
	public String group() {
		return compose.group();
	}

	@Override
	public net.minecraft.world.item.crafting.CraftingBookCategory category() {
		return compose.category();
	}

	@Override
	public RecipeSerializer<UpgradeNextTierRecipe> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public PlacementInfo placementInfo() {
		return compose.placementInfo();
	}

	@Override
	public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
		return compose.display();
	}

	@Override
	public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
		return compose.recipeBookCategory();
	}
}
