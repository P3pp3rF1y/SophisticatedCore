package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public abstract class BlockBase extends Block {
	public BlockBase(Properties properties) {
		super(properties);
	}

	public void addCreativeTabItems(Consumer<ItemStack> itemConsumer) {
		itemConsumer.accept(new ItemStack(this));
	}
}
