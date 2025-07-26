package net.p3pp3rf1y.sophisticatedcore.upgrades.feeding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.EventHooks;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

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
		filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(), this::isEdible, ModCoreDataComponents.FILTER_ATTRIBUTES);
	}

	private boolean isEdible(ItemStack s) {
		FoodProperties foodProperties = s.get(DataComponents.FOOD);
		return foodProperties != null && foodProperties.nutrition() >= 1;
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (isInCooldown(level)) {
			return;
		}

		boolean hungryPlayer = false;
		if (!(entity instanceof Player)) {
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
		ITrackedContentsItemHandler inventory = storageWrapper.getInventoryForUpgradeProcessing();
		return InventoryHelper.iterate(inventory, (slot, stack) -> tryFeedingStack(level, hungerLevel, player, slot, stack, inventory), () -> false, ret -> ret);
	}

	private boolean tryFeedingStack(Level level, int hungerLevel, Player player, Integer slot, ItemStack stack, ITrackedContentsItemHandler inventory) {
		boolean isHurt = player.getHealth() < player.getMaxHealth() - 0.1F;
		if (isEdible(stack) && filterLogic.matchesFilter(stack) && (isHungryEnoughForFood(hungerLevel, stack) || shouldFeedImmediatelyWhenHurt() && hungerLevel > 0 && isHurt)) {
			ItemStack mainHandItem = player.getMainHandItem();
			player.getInventory().getNonEquipmentItems().set(player.getInventory().getSelectedSlot(), stack);

			ItemStack singleItemCopy = stack.copy();
			singleItemCopy.setCount(1);

			if (singleItemCopy.use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME) {
				stack.shrink(1);
				inventory.setStackInSlot(slot, stack);

				ItemStack resultItem = EventHooks.onItemUseFinish(player, singleItemCopy.copy(), 0, singleItemCopy.getItem().finishUsingItem(singleItemCopy, level, player));
				if (!resultItem.isEmpty()) {
					ItemStack insertResult = inventory.insertItem(resultItem, false);
					if (!insertResult.isEmpty()) {
						CapabilityHelper.runOnCapability(player, Capabilities.ItemHandler.ENTITY, null, playerInventory ->
								InventoryHelper.insertOrDropItem(player, insertResult, playerInventory));
					}
				}

				player.getInventory().getNonEquipmentItems().set(player.getInventory().getSelectedSlot(), mainHandItem);
				return true;
			}
			player.getInventory().getNonEquipmentItems().set(player.getInventory().getSelectedSlot(), mainHandItem);
		}
		return false;
	}

	private boolean isHungryEnoughForFood(int hungerLevel, ItemStack stack) {
		if (stack.getItem() == Items.OMINOUS_BOTTLE) {
			return false; // Don't eat ominous bottle at all
		}

		FoodProperties foodProperties = stack.get(DataComponents.FOOD);
		if (foodProperties == null) {
			return false;
		}

		HungerLevel feedAtHungerLevel = getFeedAtHungerLevel();
		if (feedAtHungerLevel == HungerLevel.ANY) {
			return true;
		}

		int nutrition = foodProperties.nutrition();
		return (feedAtHungerLevel == HungerLevel.HALF ? (nutrition / 2) : nutrition) <= hungerLevel;
	}

	@Override
	public FilterLogic getFilterLogic() {
		return filterLogic;
	}

	public HungerLevel getFeedAtHungerLevel() {
		return upgrade.getOrDefault(ModCoreDataComponents.FEED_AT_HUNGER_LEVEL, HungerLevel.HALF);
	}

	public void setFeedAtHungerLevel(HungerLevel hungerLevel) {
		upgrade.set(ModCoreDataComponents.FEED_AT_HUNGER_LEVEL, hungerLevel);
		save();
	}

	public boolean shouldFeedImmediatelyWhenHurt() {
		return upgrade.getOrDefault(ModCoreDataComponents.FEED_IMMEDIATELY_WHEN_HURT, true);
	}

	public void setFeedImmediatelyWhenHurt(boolean feedImmediatelyWhenHurt) {
		upgrade.set(ModCoreDataComponents.FEED_IMMEDIATELY_WHEN_HURT, feedImmediatelyWhenHurt);
		save();
	}
}
