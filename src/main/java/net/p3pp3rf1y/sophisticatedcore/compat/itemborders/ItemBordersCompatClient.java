package net.p3pp3rf1y.sophisticatedcore.compat.itemborders;

import com.anthonyhilyard.itemborders.ItemBorders;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;

public class ItemBordersCompatClient {
	private ItemBordersCompatClient() {}

	public static void registerBorderDecorationRenderer() {
		StorageScreenBase.setSlotDecorationRenderer(ItemBordersCompatClient::renderItemBorder);
	}

	private static void renderItemBorder(GuiGraphics guiGraphics, Slot slot) {
		ItemBorders.renderBorder(guiGraphics.pose(), slot);
	}
}
