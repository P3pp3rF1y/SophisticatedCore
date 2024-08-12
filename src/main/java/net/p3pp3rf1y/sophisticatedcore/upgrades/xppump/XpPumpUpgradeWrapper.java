package net.p3pp3rf1y.sophisticatedcore.upgrades.xppump;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.XpHelper;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class XpPumpUpgradeWrapper extends UpgradeWrapperBase<XpPumpUpgradeWrapper, XpPumpUpgradeItem> implements ITickableUpgrade {
	private static final int DEFAULT_LEVEL = 10;
	private static final int COOLDOWN = 5;
	private static final int ALL_LEVELS = 10000;
	private static final int PLAYER_SEARCH_RANGE = 3;

	private final XpPumpUpgradeConfig xpPumpUpgradeConfig;

	protected XpPumpUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		xpPumpUpgradeConfig = upgradeItem.getXpPumpUpgradeConfig();
	}

	@Override
	public void tick(@Nullable LivingEntity entity, Level level, BlockPos pos) {
		if ((entity != null && !(entity instanceof Player)) || isInCooldown(level)) {
			return;
		}

		if (entity == null) {
			AABB searchBox = new AABB(pos).inflate(PLAYER_SEARCH_RANGE);
			for (Player player : level.players()) {
				if (searchBox.contains(player.getX(), player.getY(), player.getZ())) {
					interactWithPlayer(player);
					mendItems(player);
				}
			}
		} else {
			Player player = (Player) entity;
			interactWithPlayer(player);
			mendItems(player);
		}

		setCooldown(level, COOLDOWN);
	}

	private void mendItems(Player player) {
		if (Boolean.FALSE.equals(xpPumpUpgradeConfig.mendingOn.get()) || !shouldMendItems()) {
			return;
		}

		EnchantmentHelper.getRandomItemWith(EnchantmentEffectComponents.REPAIR_WITH_XP, player, ItemStack::isDamaged)
				.ifPresent(item -> {
					ItemStack itemStack = item.itemStack();
					if (!itemStack.isEmpty() && itemStack.isDamaged() && itemStack.getXpRepairRatio() > 0) {
						float xpToTryDrain = Math.min(xpPumpUpgradeConfig.maxXpPointsPerMending.get(), itemStack.getDamageValue() / itemStack.getXpRepairRatio());
						if (xpToTryDrain > 0) {
							storageWrapper.getFluidHandler().ifPresent(fluidHandler -> {
								FluidStack drained = fluidHandler.drain(ModFluids.EXPERIENCE_TAG, XpHelper.experienceToLiquid(xpToTryDrain), IFluidHandler.FluidAction.EXECUTE, false);
								float xpDrained = XpHelper.liquidToExperience(drained.getAmount());
								int durationToRepair = (int) (xpDrained * itemStack.getXpRepairRatio());
								itemStack.setDamageValue(itemStack.getDamageValue() - durationToRepair);
							});
						}
					}
				});
	}

	private void interactWithPlayer(Player player) {
		storageWrapper.getFluidHandler().ifPresent(fluidHandler -> {
			int level = getLevel();
			AutomationDirection direction = getDirection();
			if (direction == AutomationDirection.OFF) {
				return;
			}

			if (direction == AutomationDirection.INPUT) {
				if (level < player.experienceLevel || (level == player.experienceLevel && player.experienceProgress > 0)) {
					tryFillTankWithPlayerExperience(player, fluidHandler, level, false);
				}
			} else if (direction == AutomationDirection.OUTPUT && level > player.experienceLevel) {
				tryGivePlayerExperienceFromTank(player, fluidHandler, level, false);
			}
		});
	}

	private void tryGivePlayerExperienceFromTank(Player player, IStorageFluidHandler fluidHandler, int stopAtLevel) {
		tryGivePlayerExperienceFromTank(player, fluidHandler, stopAtLevel, true);
	}

	private void tryGivePlayerExperienceFromTank(Player player, IStorageFluidHandler fluidHandler, int stopAtLevel, boolean ignoreInOutLimit) {
		int maxXpPointsToGive = XpHelper.getExperienceForLevel(stopAtLevel) - XpHelper.getPlayerTotalExperience(player);
		FluidStack drained = fluidHandler.drain(ModFluids.EXPERIENCE_TAG, XpHelper.experienceToLiquid(maxXpPointsToGive), IFluidHandler.FluidAction.EXECUTE, ignoreInOutLimit);

		if (!drained.isEmpty()) {
			player.giveExperiencePoints((int) XpHelper.liquidToExperience(drained.getAmount()));
		}
	}

	private void tryFillTankWithPlayerExperience(Player player, IStorageFluidHandler fluidHandler, int stopAtLevel) {
		tryFillTankWithPlayerExperience(player, fluidHandler, stopAtLevel, true);
	}

	private void tryFillTankWithPlayerExperience(Player player, IStorageFluidHandler fluidHandler, int stopAtLevel, boolean ignoreInOutLimit) {
		int maxXpPointsToTake = XpHelper.getPlayerTotalExperience(player) - XpHelper.getExperienceForLevel(stopAtLevel);
		int filled = fluidHandler.fill(ModFluids.EXPERIENCE_TAG, XpHelper.experienceToLiquid(maxXpPointsToTake), ModFluids.XP_STILL.get(), IFluidHandler.FluidAction.EXECUTE, ignoreInOutLimit);

		if (filled > 0) {
			player.giveExperiencePoints((int) -XpHelper.liquidToExperience(filled));
		}
	}

	public void takeLevelsFromPlayer(Player player) {
		storageWrapper.getFluidHandler().ifPresent(fluidHandler -> tryFillTankWithPlayerExperience(player, fluidHandler, Math.max(player.experienceLevel - getLevelsToStore(), 0)));
	}

	public void takeAllExperienceFromPlayer(Player player) {
		storageWrapper.getFluidHandler().ifPresent(fluidHandler -> tryFillTankWithPlayerExperience(player, fluidHandler, 0));
	}

	public void giveLevelsToPlayer(Player player) {
		storageWrapper.getFluidHandler().ifPresent(fluidHandler -> tryGivePlayerExperienceFromTank(player, fluidHandler, player.experienceLevel + getLevelsToTake()));
	}

	public void giveAllExperienceToPlayer(Player player) {
		storageWrapper.getFluidHandler().ifPresent(fluidHandler -> tryGivePlayerExperienceFromTank(player, fluidHandler, ALL_LEVELS));
	}

	public AutomationDirection getDirection() {
		return upgrade.getOrDefault(ModCoreDataComponents.AUTOMATION_DIRECTION, AutomationDirection.INPUT);
	}

	public void setDirection(AutomationDirection direction) {
		upgrade.set(ModCoreDataComponents.AUTOMATION_DIRECTION, direction);
		save();
	}

	public void setLevel(int level) {
		upgrade.set(ModCoreDataComponents.LEVEL, level);
		save();
	}

	public int getLevel() {
		return upgrade.getOrDefault(ModCoreDataComponents.LEVEL, DEFAULT_LEVEL);
	}

	public void setLevelsToStore(int levelsToStore) {
		upgrade.set(ModCoreDataComponents.LEVELS_TO_STORE, levelsToStore);
		save();
	}

	public int getLevelsToStore() {
		return upgrade.getOrDefault(ModCoreDataComponents.LEVELS_TO_STORE, 1);
	}

	public void setLevelsToTake(int levelsToTake) {
		upgrade.set(ModCoreDataComponents.LEVELS_TO_TAKE, levelsToTake);
		save();
	}

	public int getLevelsToTake() {
		return upgrade.getOrDefault(ModCoreDataComponents.LEVELS_TO_TAKE, 1);
	}

	public boolean shouldMendItems() {
		return upgrade.getOrDefault(ModCoreDataComponents.MEND_ITEMS, true);
	}

	public void setMendItems(boolean mendItems) {
		upgrade.set(ModCoreDataComponents.MEND_ITEMS, mendItems);
		save();
	}
}
