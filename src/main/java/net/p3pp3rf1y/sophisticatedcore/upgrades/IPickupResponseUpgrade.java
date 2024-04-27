package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IPickupResponseUpgrade {
	ItemStack pickup(Level level, ItemStack stack, boolean simulate);
}
