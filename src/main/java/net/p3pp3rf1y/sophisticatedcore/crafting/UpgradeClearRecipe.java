package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;

public class UpgradeClearRecipe extends CustomRecipe {
	public UpgradeClearRecipe(ResourceLocation registryName, CraftingBookCategory category) {
		super(registryName, category);
	}

	@Override
	public boolean matches(CraftingContainer inventory, Level level) {
		boolean upgradePresent = false;
		for (ItemStack stack : inventory.getItems()) {
			if (!stack.isEmpty()) {
				if (stack.getItem() instanceof UpgradeItemBase && stack.hasTag() && !upgradePresent) {
					upgradePresent = true;
				} else {
					return false;
				}
			}
		}

		return upgradePresent;
	}

	@Override
	public ItemStack assemble(CraftingContainer inventory, RegistryAccess registryAccess) {
		ItemStack upgrade = ItemStack.EMPTY;
		for (ItemStack stack : inventory.getItems()) {
			if (!stack.isEmpty() && stack.getItem() instanceof UpgradeItemBase) {
				upgrade = stack;
			}
		}
		ItemStack copy = upgrade.copy();
		copy.setTag(null);
		return copy;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width >= 1 && height >= 1;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModRecipes.UPGRADE_CLEAR_SERIALIZER.get();
	}
}
