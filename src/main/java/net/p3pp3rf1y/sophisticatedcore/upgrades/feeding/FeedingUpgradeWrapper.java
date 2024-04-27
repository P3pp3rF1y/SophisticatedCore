package net.p3pp3rf1y.sophisticatedcore.upgrades.feeding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
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
	public void tick(@Nullable LivingEntity entity, Level level, BlockPos pos) {
		if (isInCooldown(level) || (entity != null && !(entity instanceof Player))) {
			return;
		}

		boolean hungryPlayer = false;
		if (entity == null) {
			AtomicBoolean stillHungryPlayer = new AtomicBoolean(false);
			level.getEntities(EntityType.PLAYER, new AABB(pos).inflate(FEEDING_RANGE), p -> true).forEach(p -> stillHungryPlayer.set(stillHungryPlayer.get() || feedPlayerAndGetHungry(p, level)));
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
		boolean isHurt = player.getHealth() < player.getMaxHealth() - 0.1F;
		IItemHandlerModifiable inventory = storageWrapper.getInventoryForUpgradeProcessing();
		AtomicBoolean fedPlayer = new AtomicBoolean(false);
		InventoryHelper.iterate(inventory, (slot, stack) -> {
			if (isEdible(stack, player) && filterLogic.matchesFilter(stack) && (isHungryEnoughForFood(hungerLevel, stack, player) || shouldFeedImmediatelyWhenHurt() && hungerLevel > 0 && isHurt)) {
				ItemStack mainHandItem = player.getMainHandItem();
				player.getInventory().items.set(player.getInventory().selected, stack);
				if (stack.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME) {
					player.getInventory().items.set(player.getInventory().selected, mainHandItem);
					ItemStack containerItem = EventHooks.onItemUseFinish(player, stack.copy(), 0, stack.getItem().finishUsingItem(stack, level, player));
					inventory.setStackInSlot(slot, stack);
					if (!ItemStack.matches(containerItem, stack)) {
						//not handling the case where player doesn't have item handler cap as the player should always have it. if that changes in the future well I guess I fix it
						CapabilityHelper.runOnCapability(player, Capabilities.ItemHandler.ENTITY, null, playerInventory -> InventoryHelper.insertOrDropItem(player, containerItem, inventory, playerInventory));
					}
					fedPlayer.set(true);
					return true;
				}
				player.getInventory().items.set(player.getInventory().selected, mainHandItem);
			}
			return false;
		}, () -> false, ret -> ret);
		return fedPlayer.get();
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
