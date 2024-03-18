package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

import java.util.Map;

public class UpgradeSettingsTabControl extends SettingsTabControl<StorageScreenBase<?>, UpgradeSettingsTab<?>> {
	public UpgradeSettingsTabControl(Position position, StorageScreenBase<?> screen, String storageSettingsTabTooltip) {
		super(position);
		addChild(new StorageSettingsTab(new Position(x, getTopY()), screen, storageSettingsTabTooltip));
		for (Map.Entry<Integer, UpgradeContainerBase<?, ?>> entry : screen.getMenu().getUpgradeContainers().entrySet()) {
			addSettingsTab(() -> screen.getMenu().setOpenTabId(entry.getKey()), () -> screen.getMenu().removeOpenTabId(),
					UpgradeGuiManager.getTab(entry.getValue(), new Position(x, getTopY()), screen))
					.onAfterInit();
		}
	}

	public boolean slotIsNotCoveredAt(Slot slot, double mouseX, double mouseY) {
		for (Tab tab : children) {
			if (tab instanceof UpgradeSettingsTab<?> upgradeSettingsTab && !upgradeSettingsTab.slotIsNotCoveredAt(slot, mouseX, mouseY)) {
				return false;
			}
		}

		return true;
	}
}
