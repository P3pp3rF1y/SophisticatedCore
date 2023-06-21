package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.Config;

import java.util.function.Consumer;

public class BlockItemBase extends BlockItem {
	public BlockItemBase(Block pBlock, Properties properties) {
		super(pBlock, properties);
	}

	public void addCreativeTabItems(Consumer<ItemStack> itemConsumer) {
		if (Config.SERVER.enabledItems.isItemEnabled(this) && getBlock() instanceof BlockBase blockBase) {
			blockBase.addCreativeTabItems(itemConsumer);
		}
	}
}
