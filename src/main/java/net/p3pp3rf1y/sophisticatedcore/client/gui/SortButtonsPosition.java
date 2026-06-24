package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;

import java.util.Locale;

public enum SortButtonsPosition implements TranslatableEnum {
	TITLE_LINE_RIGHT, BELOW_UPGRADES, BELOW_UPGRADE_TABS, HIDDEN;

	@Override
	public Component getTranslatedName() {
		return Component.translatable(TranslationHelper.INSTANCE.translConfig("sortButtonsPosition." + name().toLowerCase(Locale.ROOT)));
	}
}
