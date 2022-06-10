package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.client.gui.components.AbstractButton;

public interface IButtonFactory {
	AbstractButton instantiateButton(StorageScreenBase<?> storageScreenBase);
}
