package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.emi;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.CommonMessages;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

public class EmiCompat implements ICompat {
	@Override
	public void init() {
		SettingsContainerMenu.setConstructCallback(menu -> menu.addInternalCompatibilitySlot(new EmiAnchorSlot()));
	}

	@Override
	public void setup() {
		PacketHandler.INSTANCE.registerMessage(EmiTransferRecipeMessage.class, EmiTransferRecipeMessage::encode, EmiTransferRecipeMessage::decode, EmiTransferRecipeMessage::onMessage);
		CommonMessages.registerMessages();
	}

	private static class EmiAnchorSlot extends Slot {
		public EmiAnchorSlot() {
			super(new SimpleContainer(1), 0, -10000, -10000);
		}

		@Override
		public boolean isActive() {
			return false;
		}
	}
}
