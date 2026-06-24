package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;

import java.util.List;

public interface ICraftingUIPart {
	void onCraftingSlotsDisplayed(List<Slot> slots);

	void onCraftingSlotsHidden();

	int getWidth();

	void setStorageScreen(StorageScreenBase<?> screen);

	ICraftingUIPart NOOP = new ICraftingUIPart() {
		@Override
		public void onCraftingSlotsDisplayed(List<Slot> slots) {
			// noop
		}

		@Override
		public void onCraftingSlotsHidden() {
			// noop
		}

		@Override
		public int getWidth() {
			return 0;
		}

		@Override
		public void setStorageScreen(StorageScreenBase<?> screen) {
			// noop
		}
	};
}
