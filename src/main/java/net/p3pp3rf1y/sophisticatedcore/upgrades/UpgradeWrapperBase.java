package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

import java.util.function.Consumer;

public abstract class UpgradeWrapperBase<W extends IUpgradeWrapper, T extends UpgradeItemBase<W>> implements IUpgradeWrapper {
	protected final IStorageWrapper storageWrapper;
	protected final Consumer<ItemStack> upgradeSaveHandler;
	protected final ItemStack upgrade;
	protected final T upgradeItem;

	private long cooldown = 0;

	protected UpgradeWrapperBase(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		this.storageWrapper = storageWrapper;
		this.upgrade = upgrade;
		//noinspection unchecked
		upgradeItem = (T) upgrade.getItem();
		this.upgradeSaveHandler = upgradeSaveHandler;
	}

	@Override
	public ItemStack getUpgradeStack() {
		return upgrade;
	}

	protected void save() {
		upgradeSaveHandler.accept(upgrade);
	}

	protected void setCooldown(Level level, int time) {
		cooldown = level.getGameTime() + time;
	}

	public long getCooldownTime() {
		return cooldown;
	}

	public boolean isInCooldown(Level level) {
		return getCooldownTime() > level.getGameTime();
	}

	@Override
	public boolean isEnabled() {
		return upgrade.getOrDefault(ModCoreDataComponents.ENABLED, true);
	}

	@Override
	public void setEnabled(boolean enabled) {
		upgrade.set(ModCoreDataComponents.ENABLED, enabled);
		save();
		storageWrapper.getUpgradeHandler().refreshWrappersThatImplementAndTypeWrappers();
	}
}
