package net.p3pp3rf1y.sophisticatedcore.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;

public class UpgradeClearRecipe extends CustomRecipe {
	public static final UpgradeClearRecipe INSTANCE = new UpgradeClearRecipe(CraftingBookCategory.MISC);
	public static final MapCodec<UpgradeClearRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
	public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeClearRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
	public static final RecipeSerializer<UpgradeClearRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	public UpgradeClearRecipe(CraftingBookCategory category) {
		super();
	}

	@Override
	public boolean matches(CraftingInput inventory, Level level) {
		boolean upgradePresent = false;
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty()) {
				if (stack.getItem() instanceof UpgradeItemBase && !stack.getComponents().isEmpty() && !upgradePresent) {
					upgradePresent = true;
				} else {
					return false;
				}
			}
		}

		return upgradePresent;
	}

	@Override
	public ItemStack assemble(CraftingInput inventory) {
		ItemStack upgrade = ItemStack.EMPTY;
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && stack.getItem() instanceof UpgradeItemBase) {
				upgrade = stack;
			}
		}
		return new ItemStack(upgrade.getItem(), 1);
	}

	@Override
	public RecipeSerializer<UpgradeClearRecipe> getSerializer() {
		return SERIALIZER;
	}
}
