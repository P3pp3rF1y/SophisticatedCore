package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import javax.annotation.Nullable;

import java.util.function.Consumer;

public abstract class CookingUpgradeWrapper<W extends CookingUpgradeWrapper<W, U, R>, U extends UpgradeItemBase<W> & ICookingUpgradeItem, R extends AbstractCookingRecipe>
		extends
			UpgradeWrapperBase<W, U>
		implements
			ITickableUpgrade,
			ICookingUpgrade<R> {
	private static final int NOTHING_TO_DO_COOLDOWN = 10;
	protected final CookingLogic<R> cookingLogic;

	protected CookingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler, RecipeType<R> recipeType,
			ResourceKey<RecipePropertySet> acceptedInputs, float burnTimeModifier) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		RecipePropertySet validInput = RecipeHelper.getPropertySet(acceptedInputs);
		cookingLogic = new CookingLogic<>(upgrade, upgradeSaveHandler, upgradeItem.getCookingUpgradeConfig(), recipeType, validInput::test, burnTimeModifier);
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (isInCooldown(level)) {
			return;
		}

		if (!cookingLogic.tick(level)) {
			setCooldown(level, NOTHING_TO_DO_COOLDOWN);
		}

		boolean isBurning = cookingLogic.isBurning(level);
		RenderDataHandler renderDataHandler = storageWrapper.getRenderDataHandler();
		if (renderDataHandler.getUpgradeClientData(CookingUpgradeClientData.TYPE).map(CookingUpgradeClientData::burning).orElse(false) != isBurning) {
			if (isBurning) {
				renderDataHandler.setUpgradeClientData(CookingUpgradeClientData.TYPE, new CookingUpgradeClientData(true));
			} else {
				renderDataHandler.removeUpgradeClientData(CookingUpgradeClientData.TYPE);
			}
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (!enabled) {
			pauseAndRemoveRenderData();
		}
		super.setEnabled(enabled);
	}

	@Override
	public void onBeforeRemoved() {
		pauseAndRemoveRenderData();
	}

	private void pauseAndRemoveRenderData() {
		cookingLogic.pause();
		RenderDataHandler renderDataHandler = storageWrapper.getRenderDataHandler();
		renderDataHandler.removeUpgradeClientData(CookingUpgradeClientData.TYPE);
	}

	public CookingLogic<R> getCookingLogic() {
		return cookingLogic;
	}

	public static class SmeltingUpgradeWrapper extends CookingUpgradeWrapper<SmeltingUpgradeWrapper, SmeltingUpgradeItem, SmeltingRecipe> {
		public SmeltingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler, RecipeType.SMELTING, RecipePropertySet.FURNACE_INPUT, 1);
		}
	}

	public static class SmokingUpgradeWrapper extends CookingUpgradeWrapper<SmokingUpgradeWrapper, SmokingUpgradeItem, SmokingRecipe> {
		public SmokingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler, RecipeType.SMOKING, RecipePropertySet.SMOKER_INPUT, 0.5f);
		}
	}

	public static class BlastingUpgradeWrapper extends CookingUpgradeWrapper<BlastingUpgradeWrapper, BlastingUpgradeItem, BlastingRecipe> {
		public BlastingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler, RecipeType.BLASTING, RecipePropertySet.BLAST_FURNACE_INPUT, 0.5f);
		}
	}
}
