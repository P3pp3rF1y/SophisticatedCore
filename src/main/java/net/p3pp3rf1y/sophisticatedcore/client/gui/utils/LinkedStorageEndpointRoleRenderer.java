package net.p3pp3rf1y.sophisticatedcore.client.gui.utils;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointRole;

public class LinkedStorageEndpointRoleRenderer {
	private static final TextureBlitData PRIMARY_ICON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(112, 112), Dimension.SQUARE_16);
	private static final TextureBlitData SECONDARY_ICON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(128, 112), Dimension.SQUARE_16);

	private LinkedStorageEndpointRoleRenderer() {
	}

	public static void renderIcon(GuiGraphicsExtractor guiGraphics, int x, int y, LinkedStorageEndpointRole role) {
		GuiHelper.blit(guiGraphics, x, y, role == LinkedStorageEndpointRole.PRIMARY ? PRIMARY_ICON : SECONDARY_ICON);
	}

	public static Component getDescription(LinkedStorageEndpointRole role) {
		return TranslationHelper.INSTANCE.translTooltip(role == LinkedStorageEndpointRole.PRIMARY ? "linked_storage.primary" : "linked_storage.secondary");
	}
}
