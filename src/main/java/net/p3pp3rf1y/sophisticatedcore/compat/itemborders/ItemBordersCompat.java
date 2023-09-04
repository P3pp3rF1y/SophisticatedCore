package net.p3pp3rf1y.sophisticatedcore.compat.itemborders;

import com.anthonyhilyard.itemborders.ItemBorders;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class ItemBordersCompat implements ICompat {
	@Override
	public void setup() {
		StorageScreenBase.setSlotDecorationRenderer(ItemBordersCompat::renderItemBorder);
	}

	private static void renderItemBorder(GuiGraphics guiGraphics, Slot slot) {
		ItemBorders.renderBorder(guiGraphics.pose(), slot);
	}
}
