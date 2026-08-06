package net.p3pp3rf1y.sophisticatedcore.upgrades.stack;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;

import javax.annotation.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class StackUpgradeConversionItem extends ItemBase {
	private final Supplier<? extends Item> sourceUpgrade;
	private final Supplier<? extends Item> targetUpgrade;

	public StackUpgradeConversionItem(Supplier<? extends Item> sourceUpgrade, Supplier<? extends Item> targetUpgrade) {
		super(new Properties());
		this.sourceUpgrade = sourceUpgrade;
		this.targetUpgrade = targetUpgrade;
	}

	public boolean canConvert(ItemStack upgradeStack) {
		return upgradeStack.is(sourceUpgrade.get());
	}

	public ItemStack getTargetUpgradeStack() {
		return new ItemStack(targetUpgrade.get());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
		tooltip.addAll(
				TranslationHelper.INSTANCE.getTranslatedLines("item.sophisticatedcore.stack_upgrade_conversion.tooltip", null, ChatFormatting.DARK_GRAY));
	}
}
