package net.p3pp3rf1y.sophisticatedcore.compat.itemborders;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;

public class ItemBordersCompatClient {
	private ItemBordersCompatClient() {
	}

	public static void registerBorderDecorationRenderer() {
		StorageScreenBase.setSlotDecorationRenderer(ItemBordersCompatClient::renderItemBorder);
	}

	private static void renderItemBorder(GuiGraphicsExtractor guiGraphics, Slot slot) {
		// ItemBorders.renderBorder(guiGraphics.pose(), slot); //TODO readd this when ItemBorders ports to current version
	}
}
