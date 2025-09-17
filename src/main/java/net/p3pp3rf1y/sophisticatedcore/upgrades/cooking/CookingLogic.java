package net.p3pp3rf1y.sophisticatedcore.upgrades.cooking;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class CookingLogic<T extends AbstractCookingRecipe> {
	private final ItemStack upgrade;
	private final Consumer<ItemStack> saveHandler;

	private CookingComponentItemHandler cookingInventory = null;
	public static final int COOK_INPUT_SLOT = 0;
	public static final int COOK_OUTPUT_SLOT = 2;
	public static final int FUEL_SLOT = 1;
	@Nullable
	private RecipeHolder<T> cookingRecipe = null;
	private boolean cookingRecipeInitialized = false;

	private final float burnTimeModifier;
	private final Predicate<ItemStack> isFuel;
	private final Predicate<ItemStack> isInput;
	private final double cookingSpeedMultiplier;
	private final double fuelEfficiencyMultiplier;
	private final RecipeType<T> recipeType;

	private boolean paused = false;
	private long remainingCookTime = 0;
	private long remainingBurnTime = 0;

	public static final Codec<Map<ResourceLocation, Integer>> RECIPES_USED_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, ExtraCodecs.NON_NEGATIVE_INT);

	public static final StreamCodec<FriendlyByteBuf, Map<ResourceLocation, Integer>> RECIPES_USED_STREAM_CODEC =
			StreamCodec.of((buf, map) -> buf.writeMap(map, ResourceLocation.STREAM_CODEC, ByteBufCodecs.INT),
					buf -> buf.readMap(ResourceLocation.STREAM_CODEC, ByteBufCodecs.INT));

	public CookingLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, CookingUpgradeConfig cookingUpgradeConfig, RecipeType<T> recipeType, float burnTimeModifier) {
		this(upgrade, saveHandler, s -> getBurnTime(s, recipeType, burnTimeModifier) > 0, s -> RecipeHelper.getCookingRecipe(s, recipeType).isPresent(), cookingUpgradeConfig, recipeType, burnTimeModifier);
	}

	public CookingLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, Predicate<ItemStack> isFuel, Predicate<ItemStack> isInput, CookingUpgradeConfig cookingUpgradeConfig, RecipeType<T> recipeType, float burnTimeModifier) {
		this.upgrade = upgrade;
		this.saveHandler = saveHandler;
		this.isFuel = isFuel;
		this.isInput = isInput;
		cookingSpeedMultiplier = cookingUpgradeConfig.cookingSpeedMultiplier.get();
		fuelEfficiencyMultiplier = cookingUpgradeConfig.fuelEfficiencyMultiplier.get();
		this.recipeType = recipeType;
		this.burnTimeModifier = burnTimeModifier;
	}

	private void save() {
		saveHandler.accept(upgrade);
	}

	public boolean tick(Level level) {
		updateTimes(level);

		AtomicBoolean didSomething = new AtomicBoolean(true);
		if (isBurning(level) || readyToStartCooking()) {
			Optional<T> fr = getCookingRecipe();
			if (fr.isEmpty() && isCooking()) {
				setIsCooking(false);
			}
			fr.ifPresent(recipe -> {
				updateFuel(level, recipe);

				if (isBurning(level) && canSmelt(recipe, level)) {
					updateCookingProgress(level, recipe);
				} else if (!isBurning(level)) {
					didSomething.set(false);
				}
			});
		}

		if (!isBurning(level) && isCooking()) {
			updateCookingCooldown(level);
		} else {
			didSomething.set(false);
		}
		return didSomething.get();
	}

	private void updateTimes(Level level) {
		if (paused) {
			unpause(level);
			return;
		}

		if (isBurning(level)) {
			remainingBurnTime = getBurnTimeFinish() - level.getGameTime();
		} else {
			remainingBurnTime = 0;
		}
		if (isCooking()) {
			remainingCookTime = getCookTimeFinish() - level.getGameTime();
		} else {
			remainingCookTime = 0;
		}
	}

	private void unpause(Level level) {
		paused = false;

		if (remainingBurnTime > 0) {
			setBurnTimeFinish(level.getGameTime() + remainingBurnTime);
		}
		if (remainingCookTime > 0) {
			setCookTimeFinish(level.getGameTime() + remainingCookTime);
			setIsCooking(true);
		}
	}

	public boolean isBurning(Level level) {
		return getBurnTimeFinish() >= level.getGameTime();
	}

	private Optional<T> getCookingRecipe() {
		if (!cookingRecipeInitialized) {
			cookingRecipe = RecipeHelper.getCookingRecipe(getCookInput(), recipeType).orElse(null);
			cookingRecipeInitialized = true;
		}
		return cookingRecipe != null ? Optional.of(cookingRecipe.value()) : Optional.empty();
	}

	private void updateCookingCooldown(Level level) {
		if (getRemainingCookTime(level) + 2 > getCookTimeTotal()) {
			setIsCooking(false);
		} else {
			setCookTimeFinish(level.getGameTime() + Math.min(getRemainingCookTime(level) + 2, getCookTimeTotal()));
		}
	}

	private void updateCookingProgress(Level level, T cookingRecipe) {
		if (isCooking() && finishedCooking(level)) {
			smelt(cookingRecipe, level);
			if (canSmelt(cookingRecipe, level)) {
				setCookTime(level, (int) (cookingRecipe.getCookingTime() * (1 / cookingSpeedMultiplier)));
			} else {
				setIsCooking(false);
			}
		} else if (!isCooking()) {
			setIsCooking(true);
			setCookTime(level, (int) (cookingRecipe.getCookingTime() * (1 / cookingSpeedMultiplier)));
		}
	}

	private boolean finishedCooking(Level level) {
		return getCookTimeFinish() <= level.getGameTime();
	}

	private boolean readyToStartCooking() {
		return !getFuel().isEmpty() && !getCookInput().isEmpty();
	}

	private void smelt(Recipe<?> recipe, Level level) {
		if (!canSmelt(recipe, level)) {
			return;
		}

		ItemStack input = getCookInput();
		ItemStack recipeOutput = recipe.getResultItem(level.registryAccess());
		ItemStack output = getCookOutput();
		if (output.isEmpty()) {
			setCookOutput(recipeOutput.copy());
		} else if (output.getItem() == recipeOutput.getItem()) {
			output.grow(recipeOutput.getCount());
			setCookOutput(output);
		}

		if (input.getItem() == Blocks.WET_SPONGE.asItem() && !getFuel().isEmpty() && getFuel().getItem() == Items.BUCKET) {
			setFuel(new ItemStack(Items.WATER_BUCKET));
		}

		if (cookingRecipe != null) {
			incrementRecipeUsed(cookingRecipe.id());
			addStoredExperience(cookingRecipe.value().getExperience());
		}
		input.shrink(1);
		setCookInput(input);
	}

	private void addStoredExperience(float experience) {
		upgrade.set(ModCoreDataComponents.STORED_XP, getStoredExperience() + experience);
		save();
	}

	public float getStoredExperience() {
		return upgrade.getOrDefault(ModCoreDataComponents.STORED_XP, 0f);
	}

	public void drainStoredExperience(float xp) {
		float storedXp = getStoredExperience();
		storedXp = Math.max(0, storedXp - xp);
		upgrade.set(ModCoreDataComponents.STORED_XP, storedXp);
		save();
	}

	private void incrementRecipeUsed(ResourceLocation recipeId) {
		Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>(getRecipesUsed());
		recipesUsed.addTo(recipeId, 1);
		upgrade.set(ModCoreDataComponents.RECIPES_USED, recipesUsed);
		save();
	}

	private void clearRecipesUsed() {
		upgrade.set(ModCoreDataComponents.RECIPES_USED, Map.of());
		save();
	}

	private Map<ResourceLocation, Integer> getRecipesUsed() {
		return upgrade.getOrDefault(ModCoreDataComponents.RECIPES_USED, Map.of());
	}

	public void setCookInput(ItemStack input) {
		cookingInventory.setStackInSlot(COOK_INPUT_SLOT, input);
	}

	private void setCookOutput(ItemStack stack) {
		getCookingInventory().setStackInSlot(COOK_OUTPUT_SLOT, stack);
	}

	private int getRemainingCookTime(Level level) {
		return (int) (getCookTimeFinish() - level.getGameTime());
	}

	private void setCookTime(Level level, int cookTime) {
		setCookTimeFinish(level.getGameTime() + cookTime);
		setCookTimeTotal(cookTime);
	}

	public void pause() {
		paused = true;
		setCookTimeFinish(0);
		setIsCooking(false);
		setBurnTimeFinish(0);
	}

	private void updateFuel(Level level, T cookingRecipe) {
		ItemStack fuel = getFuel();
		if (!isBurning(level) && canSmelt(cookingRecipe, level)) {
			if (getBurnTime(fuel, recipeType, burnTimeModifier) <= 0) {
				return;
			}
			setBurnTime(level, (int) (getBurnTime(fuel, recipeType, burnTimeModifier) * fuelEfficiencyMultiplier / cookingSpeedMultiplier));
			if (isBurning(level)) {
				if (fuel.hasCraftingRemainingItem()) {
					setFuelWithoutValidation(fuel.getCraftingRemainingItem());
				} else if (!fuel.isEmpty()) {
					fuel.shrink(1);
					setFuel(fuel);
					if (fuel.isEmpty()) {
						setFuel(fuel.getCraftingRemainingItem());
					}
				}
			}
		}
	}

	private void setBurnTime(Level level, int burnTime) {
		setBurnTimeFinish(level.getGameTime() + burnTime);
		setBurnTimeTotal(burnTime);
	}

	protected boolean canSmelt(Recipe<?> cookingRecipe, Level level) {
		if (getCookInput().isEmpty()) {
			return false;
		}
		ItemStack recipeOutput = cookingRecipe.getResultItem(level.registryAccess());
		if (recipeOutput.isEmpty()) {
			return false;
		} else {
			ItemStack output = getCookOutput();
			if (output.isEmpty()) {
				return true;
			} else if (output.getItem() != recipeOutput.getItem()) {
				return false;
			} else if (output.getCount() + recipeOutput.getCount() <= 99 && output.getCount() + recipeOutput.getCount() <= output.getMaxStackSize()) {
				return true;
			} else {
				return output.getCount() + recipeOutput.getCount() <= recipeOutput.getMaxStackSize();
			}
		}
	}

	private static <T extends AbstractCookingRecipe> int getBurnTime(ItemStack fuel, RecipeType<T> recipeType, float burnTimeModifier) {
		return (int) (fuel.getBurnTime(recipeType) * burnTimeModifier);
	}

	public ItemStack getCookOutput() {
		return getCookingInventory().getStackInSlot(COOK_OUTPUT_SLOT);
	}

	public ItemStack getCookInput() {
		return getCookingInventory().getStackInSlot(COOK_INPUT_SLOT);
	}

	public ItemStack getFuel() {
		return getCookingInventory().getStackInSlot(FUEL_SLOT);
	}

	public void setFuel(ItemStack fuel) {
		getCookingInventory().setStackInSlot(FUEL_SLOT, fuel);
	}

	private void setFuelWithoutValidation(ItemStack fuel) {
		getCookingInventory().setStackInSlotWithoutValidation(FUEL_SLOT, fuel);
	}

	public CookingComponentItemHandler getCookingInventory() {
		if (cookingInventory == null) {
			cookingInventory = new CookingComponentItemHandler();
		}
		return cookingInventory;
	}

	public long getBurnTimeFinish() {
		return upgrade.getOrDefault(ModCoreDataComponents.BURN_TIME_FINISH, 0L);
	}

	private void setBurnTimeFinish(long burnTimeFinish) {
		upgrade.set(ModCoreDataComponents.BURN_TIME_FINISH, burnTimeFinish);
		save();
	}

	public int getBurnTimeTotal() {
		return upgrade.getOrDefault(ModCoreDataComponents.BURN_TIME_TOTAL, 0);
	}

	private void setBurnTimeTotal(int burnTimeTotal) {
		upgrade.set(ModCoreDataComponents.BURN_TIME_TOTAL, burnTimeTotal);
		save();
	}

	public long getCookTimeFinish() {
		return upgrade.getOrDefault(ModCoreDataComponents.COOK_TIME_FINISH, -1L);
	}

	private void setCookTimeFinish(long cookTimeFinish) {
		upgrade.set(ModCoreDataComponents.COOK_TIME_FINISH, cookTimeFinish);
		save();
	}

	public int getCookTimeTotal() {
		return upgrade.getOrDefault(ModCoreDataComponents.COOK_TIME_TOTAL, 0);
	}

	private void setCookTimeTotal(int cookTimeTotal) {
		upgrade.set(ModCoreDataComponents.COOK_TIME_TOTAL, cookTimeTotal);
		save();
	}

	public boolean isCooking() {
		return upgrade.getOrDefault(ModCoreDataComponents.IS_COOKING, false);
	}

	private void setIsCooking(boolean isCooking) {
		upgrade.set(ModCoreDataComponents.IS_COOKING, isCooking);
		save();
	}

	public void awardUsedRecipesAndPopExperience(ServerPlayer serverPlayer) {
		List<RecipeHolder<?>> recipes = getRecipesToAwardAndPopExperience(serverPlayer.serverLevel(), serverPlayer.position());
		serverPlayer.awardRecipes(recipes);

		List<ItemStack> items = InventoryHelper.getStacks(cookingInventory);

		for(RecipeHolder<?> recipeholder : recipes) {
			if (recipeholder != null) {
				serverPlayer.triggerRecipeCrafted(recipeholder, items);
			}
		}

		clearRecipesUsed();
	}

	public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel serverLevel, Vec3 position) {
		List<RecipeHolder<?>> recipes = Lists.newArrayList();

		for (Map.Entry<ResourceLocation, Integer> entry : getRecipesUsed().entrySet()) {
			serverLevel.getRecipeManager().byKey(entry.getKey()).ifPresent(recipes::add);
		}
		createExperience(serverLevel, position);
		return recipes;
	}

	private void createExperience(ServerLevel serverLevel, Vec3 position) {
		float storedXp = getStoredExperience();
		int i = Mth.floor(storedXp);
		float f = Mth.frac(storedXp);
		if (f != 0.0F && Math.random() < (double)f) {
			++i;
		}

		ExperienceOrb.award(serverLevel, position, i);
		drainStoredExperience(i);
	}

	public class CookingComponentItemHandler extends ComponentItemHandler {
		public CookingComponentItemHandler() {
			super(CookingLogic.this.upgrade, ModCoreDataComponents.COOKING_INVENTORY.get(), 3);
		}

		@Override
		protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {
			super.onContentsChanged(slot, oldStack, newStack);
			save();
			if (slot == COOK_INPUT_SLOT) {
				cookingRecipeInitialized = false;
			}
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			if (stack.isEmpty() || ItemStack.isSameItemSameComponents(getStackInSlot(slot), stack)) {
				return true;
			}

			return switch (slot) {
				case COOK_INPUT_SLOT -> isInput.test(stack);
				case FUEL_SLOT -> isFuel.test(stack);
				default -> true;
			};
		}

		public void setStackInSlotWithoutValidation(int slot, ItemStack stack) {
			ItemContainerContents contents = getContents();
			ItemStack existing = getStackFromContents(contents, slot);
			if (!ItemStack.matches(stack, existing)) {
				updateContents(contents, stack, slot);
			}
		}
	}
}
