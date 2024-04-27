package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;

import javax.annotation.Nullable;
import java.util.List;

public abstract class UpgradeItemBase<T extends IUpgradeWrapper> extends ItemBase implements IUpgradeItem<T> {
	private final IUpgradeCountLimitConfig upgradeTypeLimitConfig;

	protected UpgradeItemBase(IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(new Properties());
		this.upgradeTypeLimitConfig = upgradeTypeLimitConfig;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
		tooltip.addAll(TranslationHelper.INSTANCE.getTranslatedLines(stack.getItem().getDescriptionId() + TranslationHelper.TOOLTIP_SUFFIX, null, ChatFormatting.DARK_GRAY));
	}

	@Override
	public int getUpgradesPerStorage(String storageType) {
		return upgradeTypeLimitConfig.getMaxUpgradesPerStorage(storageType, BuiltInRegistries.ITEM.getKey(this));
	}

	@Override
	public int getUpgradesInGroupPerStorage(String storageType) {
		if (getUpgradeGroup().isSolo()) {
			return Integer.MAX_VALUE;
		}

		return upgradeTypeLimitConfig.getMaxUpgradesInGroupPerStorage(storageType, getUpgradeGroup());
	}

	@Override
	public Component getName() {
		return Component.translatable(getDescriptionId());
	}
}
