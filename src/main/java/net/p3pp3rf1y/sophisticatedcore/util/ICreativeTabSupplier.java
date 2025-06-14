package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public interface ICreativeTabSupplier {
	void addCreativeTabItems(Consumer<ItemStack> itemConsumer);
}
