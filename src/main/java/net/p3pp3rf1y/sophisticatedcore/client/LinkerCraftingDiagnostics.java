package net.p3pp3rf1y.sophisticatedcore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.network.RequestLinkerCraftingDiagnosticsMessage;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class LinkerCraftingDiagnostics {
	private static int lastRequestedContainerId = -1;
	private static int lastRequestedSignature;
	@Nullable
	private static AbstractContainerMenu lastRequestedMenu;
	private static final Map<Integer, String> DIAGNOSTICS = new HashMap<>();

	private LinkerCraftingDiagnostics() {
	}

	public static void requestIfChanged(AbstractContainerMenu menu) {
		if (menu != lastRequestedMenu) {
			lastRequestedMenu = menu;
			lastRequestedContainerId = -1;
			DIAGNOSTICS.clear();
		}
		int signature = 1;
		boolean hasCraftingGrid = false;
		for (Slot slot : menu.slots) {
			if (slot.container instanceof CraftingContainer) {
				signature = 31 * signature + 31 * slot.index + slot.getItem().hashCode();
				hasCraftingGrid = true;
			}
		}
		if (!hasCraftingGrid) {
			DIAGNOSTICS.clear();
			return;
		}
		if (menu.containerId == lastRequestedContainerId && signature == lastRequestedSignature) {
			return;
		}
		lastRequestedContainerId = menu.containerId;
		lastRequestedSignature = signature;
		DIAGNOSTICS.clear();
		PacketHandler.INSTANCE.sendToServer(new RequestLinkerCraftingDiagnosticsMessage(menu.containerId));
	}

	public static void update(int containerId, Map<Integer, String> updatedDiagnostics) {
		if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen) || screen.getMenu().containerId != containerId) {
			return;
		}
		DIAGNOSTICS.clear();
		DIAGNOSTICS.putAll(updatedDiagnostics);
	}

	@Nullable
	public static String getStatusMessage(int slot) {
		return DIAGNOSTICS.get(slot);
	}
}
