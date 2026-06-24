package net.p3pp3rf1y.sophisticatedcore.upgrades.feeding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.ForgeEventFactory;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import javax.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class FeedingUpgradeWrapper extends UpgradeWrapperBase<FeedingUpgradeWrapper, FeedingUpgradeItem> implements ITickableUpgrade, IFilteredUpgrade {
	private static final int COOLDOWN = 100;
	private static final int STILL_HUNGRY_COOLDOWN = 10;
	private static final int FEEDING_RANGE = 3;
	private final FilterLogic filterLogic;

	public FeedingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(), ItemStack::isEdible);
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (isInCooldown(level) || (entity != null && !(entity instanceof Player))) {
			return;
		}

		boolean hungryPlayer = false;
		if (entity == null) {
			AtomicBoolean stillHungryPlayer = new AtomicBoolean(false);
			level.getEntities(EntityType.PLAYER, new AABB(pos).inflate(FEEDING_RANGE), p -> true)
					.forEach(p -> stillHungryPlayer.set(stillHungryPlayer.get() || feedPlayerAndGetHungry(p, level)));
			hungryPlayer = stillHungryPlayer.get();
		} else {
			if (feedPlayerAndGetHungry((Player) entity, level)) {
				hungryPlayer = true;
			}
		}
		if (hungryPlayer) {
			setCooldown(level, STILL_HUNGRY_COOLDOWN);
			return;
		}

		setCooldown(level, COOLDOWN);
	}

	private boolean feedPlayerAndGetHungry(Player player, Level level) {
		int hungerLevel = 20 - player.getFoodData().getFoodLevel();
		if (hungerLevel == 0) {
			return false;
		}
		return tryFeedingFoodFromStorage(level, hungerLevel, player) && player.getFoodData().getFoodLevel() < 20;
	}

	private boolean tryFeedingFoodFromStorage(Level level, int hungerLevel, Player player) {
		ITrackedContentsItemHandler inventory = storageWrapper.getInventoryForUpgradeProcessing();
		return InventoryHelper.iterate(inventory, (slot, stack) -> tryFeedingStack(level, hungerLevel, player, slot, stack, inventory), () -> false,
				ret -> ret);
	}

	private boolean tryFeedingStack(Level level, int hungerLevel, Player player, Integer slot, ItemStack stack, ITrackedContentsItemHandler inventory) {
		boolean isHurt = player.getHealth() < player.getMaxHealth() - 0.1F;
		if (isEdible(stack, player) && filterLogic.matchesFilter(stack)
				&& (isHungryEnoughForFood(hungerLevel, stack, player) || shouldFeedImmediatelyWhenHurt() && hungerLevel > 0 && isHurt)) {
			ItemStack mainHandItem = player.getMainHandItem();
			player.getInventory().items.set(player.getInventory().selected, stack);

			ItemStack singleItemCopy = stack.copy();
			singleItemCopy.setCount(1);

			if (singleItemCopy.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME) {
				stack.shrink(1);
				inventory.setStackInSlot(slot, stack);

				ItemStack resultItem = ForgeEventFactory.onItemUseFinish(player, singleItemCopy.copy(), 0,
						singleItemCopy.getItem().finishUsingItem(singleItemCopy, level, player));
				if (!resultItem.isEmpty()) {
					ItemStack insertResult = inventory.insertItem(resultItem, false);
					if (!insertResult.isEmpty()) {
						player.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
								.ifPresent(playerInventory -> InventoryHelper.insertOrDropItem(player, insertResult, playerInventory));
					}
				}

				player.getInventory().items.set(player.getInventory().selected, mainHandItem);
				return true;
			}
			player.getInventory().items.set(player.getInventory().selected, mainHandItem);
		}
		return false;
	}

	private static boolean isEdible(ItemStack stack, LivingEntity player) {
		if (!stack.isEdible()) {
			return false;
		}
		FoodProperties foodProperties = stack.getItem().getFoodProperties(stack, player);
		return foodProperties != null && foodProperties.getNutrition() >= 1;
	}

	private boolean isHungryEnoughForFood(int hungerLevel, ItemStack stack, Player player) {
		FoodProperties foodProperties = stack.getItem().getFoodProperties(stack, player);
		if (foodProperties == null) {
			return false;
		}

		HungerLevel feedAtHungerLevel = getFeedAtHungerLevel();
		if (feedAtHungerLevel == HungerLevel.ANY) {
			return true;
		}

		int nutrition = foodProperties.getNutrition();
		return (feedAtHungerLevel == HungerLevel.HALF ? (nutrition / 2) : nutrition) <= hungerLevel;
	}

	@Override
	public FilterLogic getFilterLogic() {
		return filterLogic;
	}

	public HungerLevel getFeedAtHungerLevel() {
		return NBTHelper.getEnumConstant(upgrade, "feedAtHungerLevel", HungerLevel::fromName).orElse(HungerLevel.HALF);
	}

	public void setFeedAtHungerLevel(HungerLevel hungerLevel) {
		NBTHelper.setEnumConstant(upgrade, "feedAtHungerLevel", hungerLevel);
		save();
	}

	public boolean shouldFeedImmediatelyWhenHurt() {
		return NBTHelper.getBoolean(upgrade, "feedImmediatelyWhenHurt").orElse(true);
	}

	public void setFeedImmediatelyWhenHurt(boolean feedImmediatelyWhenHurt) {
		NBTHelper.setBoolean(upgrade, "feedImmediatelyWhenHurt", feedImmediatelyWhenHurt);
		save();
	}
}
