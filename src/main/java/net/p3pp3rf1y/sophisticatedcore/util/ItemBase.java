package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.Config;

import java.util.function.Consumer;

public class ItemBase extends Item implements ICreativeTabSupplier {
	public ItemBase(Properties properties) {
		super(properties);
	}

	@Override
	public void addCreativeTabItems(Consumer<ItemStack> itemConsumer) {
		if (Config.COMMON.enabledItems.isItemEnabled(this)) {
			itemConsumer.accept(new ItemStack(this));
		}
	}
}
