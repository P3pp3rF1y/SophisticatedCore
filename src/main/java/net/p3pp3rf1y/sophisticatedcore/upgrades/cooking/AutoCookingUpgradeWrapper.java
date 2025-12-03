package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedcore.util.XpHelper;

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

	public AutoCookingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler, RecipeType<R> recipeType, ResourceKey<RecipePropertySet> acceptedInputs, float burnTimeModifier) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		this.recipeType = recipeType;
		RecipePropertySet validInput = RecipeHelper.getPropertySet(acceptedInputs);
		AutoCookingUpgradeConfig autoCookingUpgradeConfig = upgradeItem.getAutoCookingUpgradeConfig();
		inputFilterLogic = new FilterLogic(upgrade, upgradeSaveHandler, autoCookingUpgradeConfig.inputFilterSlots.get(),
				validInput::test, ModCoreDataComponents.INPUT_FILTER_ATTRIBUTES);
		fuelFilterLogic = new FilterLogic(upgrade, upgradeSaveHandler, autoCookingUpgradeConfig.fuelFilterSlots.get(),
				s -> s.getBurnTime(recipeType, WorldHelper.getFuelValues()) > 0, ModCoreDataComponents.FUEL_FILTER_ATTRIBUTES);
		fuelFilterLogic.setAllowByDefault(true);
		fuelFilterLogic.setEmptyAllowListMatchesEverything();

		isValidInput = s -> validInput.test(s) && inputFilterLogic.matchesFilter(s);
		isValidFuel = s -> s.getBurnTime(recipeType, WorldHelper.getFuelValues()) > 0 && fuelFilterLogic.matchesFilter(s);
		cookingLogic = new CookingLogic<>(upgrade, upgradeSaveHandler, isValidFuel, isValidInput, autoCookingUpgradeConfig, recipeType, burnTimeModifier);
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (!enabled) {
			pauseAndRemoveRenderData();
		}
		super.setEnabled(enabled);
	}

	private void pauseAndRemoveRenderData() {
		cookingLogic.pause();
		RenderDataHandler renderDataHandler = storageWrapper.getRenderDataHandler();
		renderDataHandler.removeUpgradeClientData(CookingUpgradeClientData.TYPE);
	}

	@Override
	public void onBeforeRemoved() {
		pauseAndRemoveRenderData();
	}

	private void tryPushingOutput() {
		if (outputCooldown > 0) {
			outputCooldown--;
			return;
		}

		ItemStack output = cookingLogic.getCookOutput();
		ITrackedContentsItemResourceHandler inventory = storageWrapper.getInventoryForUpgradeProcessing();
		ItemResource resource = ItemResource.of(output);
		int inserted = output.isEmpty() ? 0 : InventoryHelper.insert(inventory, resource, output.getCount());
		if (inserted > 0) {
			InventoryHelper.extract(cookingLogic.getCookingInventory(), resource, inserted);
			tryPushingXpToTanks();
		} else {
			outputCooldown = NO_INVENTORY_SPACE_COOLDOWN;
		}

		ItemStack fuel = cookingLogic.getFuel();
		if (!fuel.isEmpty() && fuel.getBurnTime(recipeType, WorldHelper.getFuelValues()) <= 0) {
			resource = ItemResource.of(fuel);
			inserted = InventoryHelper.insert(inventory, resource, fuel.getCount());
			if (inserted > 0) {
				InventoryHelper.extract(cookingLogic.getCookingInventory(), CookingLogic.FUEL_SLOT, resource, inserted);
			}
		}
	}

	private void tryPushingXpToTanks() {
		storageWrapper.getFluidHandler().ifPresent(fluidHandler -> {
			float storedExperience = cookingLogic.getStoredExperience();
			for (int i = 0; i < fluidHandler.size(); i++) {
				FluidResource xpFluid = FluidResource.of(ModFluids.XP_STILL.get());
				try (Transaction tx = Transaction.openRoot()) {
					int filled = fluidHandler.insert(xpFluid, XpHelper.experienceToLiquid(storedExperience), tx);
					if (filled > 0) {
						tx.commit();
						cookingLogic.drainStoredExperience(XpHelper.liquidToExperience(filled));
						storedExperience -= XpHelper.liquidToExperience(filled);
						if (storedExperience <= 0) {
							break;
						}
					}
				}
			}
		});
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
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
		RenderDataHandler renderDataHandler = storageWrapper.getRenderDataHandler();
		if (renderDataHandler.getUpgradeClientData(CookingUpgradeClientData.TYPE).map(CookingUpgradeClientData::burning).orElse(false) != isBurning) {
			if (isBurning) {
				renderDataHandler.setUpgradeClientData(CookingUpgradeClientData.TYPE, new CookingUpgradeClientData(true));
			} else {
				renderDataHandler.removeUpgradeClientData(CookingUpgradeClientData.TYPE);
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
		ITrackedContentsItemResourceHandler inventory = storageWrapper.getInventoryForUpgradeProcessing();
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

		int extracted = InventoryHelper.extract(inventory, toExtract);
		if (extracted > 0) {
			setSlot.accept(toExtract.copyWithCount(extracted + stack.getCount()));
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
			super(storageWrapper, upgrade, upgradeSaveHandler, RecipeType.SMELTING, RecipePropertySet.FURNACE_INPUT, 1);
		}
	}

	public static class AutoSmokingUpgradeWrapper extends AutoCookingUpgradeWrapper<AutoSmokingUpgradeWrapper, AutoSmokingUpgradeItem, SmokingRecipe> {
		public AutoSmokingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler, RecipeType.SMOKING, RecipePropertySet.SMOKER_INPUT, 0.5f);
		}
	}

	public static class AutoBlastingUpgradeWrapper extends AutoCookingUpgradeWrapper<AutoBlastingUpgradeWrapper, AutoBlastingUpgradeItem, BlastingRecipe> {
		public AutoBlastingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler, RecipeType.BLASTING, RecipePropertySet.BLAST_FURNACE_INPUT, 0.5f);
		}
	}
}
