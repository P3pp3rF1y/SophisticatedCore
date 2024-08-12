package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class AutoCookingUpgradeWrapper<W extends AutoCookingUpgradeWrapper<W, U, R>, U extends UpgradeItemBase<W> & IAutoCookingUpgradeItem, R extends AbstractCookingRecipe>
		extends UpgradeWrapperBase<W, U>
		implements ITickableUpgrade, ICookingUpgrade<R> {
	private static final int NOTHING_TO_DO_COOLDOWN = 10;
	private static final int NO_INVENTORY_SPACE_COOLDOWN = 60;

	private final FilterLogic inputFilterLogic;
	private final FilterLogic fuelFilterLogic;
	private final CookingLogic<R> cookingLogic;
	private final Predicate<ItemStack> isValidInput;
	private final Predicate<ItemStack> isValidFuel;
	private final RecipeType<R> recipeType;
	private int outputCooldown = 0;
	private int fuelCooldown = 0;
	private int inputCooldown = 0;

	public AutoCookingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler, RecipeType<R> recipeType, float burnTimeModifier) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		this.recipeType = recipeType;
		AutoCookingUpgradeConfig autoCookingUpgradeConfig = upgradeItem.getAutoCookingUpgradeConfig();
		inputFilterLogic = new FilterLogic(upgrade, upgradeSaveHandler, autoCookingUpgradeConfig.inputFilterSlots.get(),
				s -> RecipeHelper.getCookingRecipe(s, recipeType).isPresent(), ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES);
		fuelFilterLogic = new FilterLogic(upgrade, upgradeSaveHandler, autoCookingUpgradeConfig.fuelFilterSlots.get(),
				s -> s.getBurnTime(recipeType) > 0, ModCoreDataComponents.FUEL_FILTER_ATTRIBUTES);
		fuelFilterLogic.setAllowByDefault(true);
		fuelFilterLogic.setEmptyAllowListMatchesEverything();

		isValidInput = s -> RecipeHelper.getCookingRecipe(s, recipeType).isPresent() && inputFilterLogic.matchesFilter(s);
		isValidFuel = s -> s.getBurnTime(recipeType) > 0 && fuelFilterLogic.matchesFilter(s);
		cookingLogic = new CookingLogic<>(upgrade, upgradeSaveHandler, isValidFuel, isValidInput, autoCookingUpgradeConfig, recipeType, burnTimeModifier);
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (!enabled) {
			pauseAndRemoveRenderInfo();
		}
		super.setEnabled(enabled);
	}

	private void pauseAndRemoveRenderInfo() {
		cookingLogic.pause();
		RenderInfo renderInfo = storageWrapper.getRenderInfo();
		renderInfo.removeUpgradeRenderData(CookingUpgradeRenderData.TYPE);
	}

	@Override
	public void onBeforeRemoved() {
		pauseAndRemoveRenderInfo();
	}

	private void tryPushingOutput() {
		if (outputCooldown > 0) {
			outputCooldown--;
			return;
		}

		ItemStack output = cookingLogic.getCookOutput();
		IItemHandlerSimpleInserter inventory = storageWrapper.getInventoryForUpgradeProcessing();
		if (!output.isEmpty() && inventory.insertItem(output, true).getCount() < output.getCount()) {
			ItemStack ret = inventory.insertItem(output, false);
			cookingLogic.getCookingInventory().extractItem(CookingLogic.COOK_OUTPUT_SLOT, output.getCount() - ret.getCount(), false);
		} else {
			outputCooldown = NO_INVENTORY_SPACE_COOLDOWN;
		}

		ItemStack fuel = cookingLogic.getFuel();
		if (!fuel.isEmpty() && fuel.getBurnTime(recipeType) <= 0 && inventory.insertItem(fuel, true).getCount() < fuel.getCount()) {
			ItemStack ret = inventory.insertItem(fuel, false);
			cookingLogic.getCookingInventory().extractItem(CookingLogic.FUEL_SLOT, fuel.getCount() - ret.getCount(), false);
		}
	}

	@Override
	public void tick(@Nullable LivingEntity entity, Level level, BlockPos pos) {
		if (isInCooldown(level)) {
			return;
		}
		tryPushingOutput();
		tryPullingFuel();
		tryPullingInput();

		if (!cookingLogic.tick(level) && outputCooldown <= 0 && fuelCooldown <= 0 && inputCooldown <= 0) {
			setCooldown(level, NOTHING_TO_DO_COOLDOWN);
		}
		boolean isBurning = cookingLogic.isBurning(level);
		RenderInfo renderInfo = storageWrapper.getRenderInfo();
		if (renderInfo.getUpgradeRenderData(CookingUpgradeRenderData.TYPE).map(CookingUpgradeRenderData::isBurning).orElse(false) != isBurning) {
			if (isBurning) {
				renderInfo.setUpgradeRenderData(CookingUpgradeRenderData.TYPE, new CookingUpgradeRenderData(true));
			} else {
				renderInfo.removeUpgradeRenderData(CookingUpgradeRenderData.TYPE);
			}
		}
	}

	private void tryPullingInput() {
		if (inputCooldown > 0) {
			inputCooldown--;
			return;
		}

		if (tryPullingGetUnsucessful(cookingLogic.getCookInput(), cookingLogic::setCookInput, isValidInput)) {
			inputCooldown = NO_INVENTORY_SPACE_COOLDOWN;
		}
	}

	private void tryPullingFuel() {
		if (fuelCooldown > 0) {
			fuelCooldown--;
			return;
		}

		if (tryPullingGetUnsucessful(cookingLogic.getFuel(), cookingLogic::setFuel, isValidFuel)) {
			fuelCooldown = NO_INVENTORY_SPACE_COOLDOWN;
		}
	}

	private boolean tryPullingGetUnsucessful(ItemStack stack, Consumer<ItemStack> setSlot, Predicate<ItemStack> isItemValid) {
		ItemStack toExtract;
		IItemHandlerModifiable inventory = storageWrapper.getInventoryForUpgradeProcessing();
		if (stack.isEmpty()) {
			AtomicReference<ItemStack> ret = new AtomicReference<>(ItemStack.EMPTY);
			InventoryHelper.iterate(inventory, (slot, st) -> {
				if (isItemValid.test(st)) {
					ret.set(st.copy());
				}
			}, () -> !ret.get().isEmpty());
			if (!ret.get().isEmpty()) {
				toExtract = ret.get();
				toExtract.setCount(toExtract.getMaxStackSize());
			} else {
				return true;
			}
		} else if (stack.getCount() == stack.getMaxStackSize() || !isItemValid.test(stack)) {
			return true;
		} else {
			toExtract = stack.copy();
			toExtract.setCount(stack.getMaxStackSize() - stack.getCount());
		}

		if (InventoryHelper.extractFromInventory(toExtract, inventory, true).getCount() > 0) {
			ItemStack toSet = InventoryHelper.extractFromInventory(toExtract, inventory, false);
			toSet.grow(stack.getCount());
			setSlot.accept(toSet);
		} else {
			return true;
		}
		return false;
	}

	@Override
	public CookingLogic<R> getCookingLogic() {
		return cookingLogic;
	}

	public FilterLogic getInputFilterLogic() {
		return inputFilterLogic;
	}

	public FilterLogic getFuelFilterLogic() {
		return fuelFilterLogic;
	}

	public static class AutoSmeltingUpgradeWrapper extends AutoCookingUpgradeWrapper<AutoSmeltingUpgradeWrapper, AutoSmeltingUpgradeItem, SmeltingRecipe> {
		public AutoSmeltingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler, RecipeType.SMELTING, 1);
		}
	}

	public static class AutoSmokingUpgradeWrapper extends AutoCookingUpgradeWrapper<AutoSmokingUpgradeWrapper, AutoSmokingUpgradeItem, SmokingRecipe> {
		public AutoSmokingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler, RecipeType.SMOKING, 0.5f);
		}
	}

	public static class AutoBlastingUpgradeWrapper extends AutoCookingUpgradeWrapper<AutoBlastingUpgradeWrapper, AutoBlastingUpgradeItem, BlastingRecipe> {
		public AutoBlastingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler, RecipeType.BLASTING, 0.5f);
		}
	}
}
