package net.p3pp3rf1y.sophisticatedcore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ICraftingContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.network.RequestLinkerCraftingDiagnosticsPayload;
import net.p3pp3rf1y.sophisticatedcore.network.SyncLinkerCraftingDiagnosticsPayload;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LinkerCraftingDiagnostics {
	private static int lastRequestedContainerId = -1;
	private static int lastRequestedSignature;
	@Nullable
	private static AbstractContainerMenu lastRequestedMenu;
	private static final Map<Integer, String> diagnostics = new HashMap<>();

	private LinkerCraftingDiagnostics() {
	}

	public static void requestIfChanged(AbstractContainerMenu menu) {
		if (menu != lastRequestedMenu) {
			lastRequestedMenu = menu;
			lastRequestedContainerId = -1;
			diagnostics.clear();
		}
		int signature = 1;
		boolean hasCraftingGrid = false;
		for (Slot slot : menu.slots) {
			if (slot.container instanceof CraftingContainer) {
				signature = 31 * signature + 31 * slot.index + slot.getItem().hashCode();
				hasCraftingGrid = true;
			}
		}
		if (menu instanceof StorageContainerMenuBase<?> storageContainer) {
			for (UpgradeContainerBase<?, ?> upgradeContainer : storageContainer.getUpgradeContainers().values()) {
				if (upgradeContainer instanceof ICraftingContainer craftingContainer) {
					for (int slot = 0; slot < craftingContainer.getCraftMatrix().getContainerSize(); slot++) {
						signature = 31 * signature + craftingContainer.getCraftMatrix().getItem(slot).hashCode();
					}
					hasCraftingGrid = true;
				}
			}
		}
		if (!hasCraftingGrid) {
			diagnostics.clear();
			lastRequestedMenu = null;
			lastRequestedContainerId = -1;
			return;
		}
		if (menu.containerId == lastRequestedContainerId && signature == lastRequestedSignature) {
			return;
		}
		lastRequestedContainerId = menu.containerId;
		lastRequestedSignature = signature;
		diagnostics.clear();
		PacketDistributor.sendToServer(new RequestLinkerCraftingDiagnosticsPayload(menu.containerId));
	}

	public static void update(int containerId, List<SyncLinkerCraftingDiagnosticsPayload.Diagnostic> updatedDiagnostics) {
		if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen) || screen.getMenu().containerId != containerId) {
			return;
		}
		diagnostics.clear();
		updatedDiagnostics.forEach(diagnostic -> diagnostics.put(diagnostic.slot(), diagnostic.statusMessageKey()));
	}

	@Nullable
	public static String getStatusMessage(int slot) {
		return diagnostics.get(slot);
	}
}
