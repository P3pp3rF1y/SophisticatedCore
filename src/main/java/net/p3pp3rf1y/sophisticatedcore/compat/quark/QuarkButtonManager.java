package net.p3pp3rf1y.sophisticatedcore.compat.quark;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import vazkii.quark.base.network.QuarkNetwork;
import vazkii.quark.base.network.message.SortInventoryMessage;
import vazkii.quark.content.management.client.screen.widgets.MiniInventoryButton;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static vazkii.quark.content.management.module.EasyTransferingModule.enableShiftLock;
import static vazkii.quark.content.management.module.EasyTransferingModule.shiftLocked;
import static vazkii.quark.content.management.module.InventorySortingModule.enablePlayerInventoryInChests;
import static vazkii.quark.content.management.module.InventorySortingModule.satisfyingClick;

public class QuarkButtonManager {
	private static final String KEY_MAPPING_PREFIX = "quark.keybind.";

	private static final Map<KeyMapping, Runnable> mappingsToCheck = new HashMap<>();

	public static void addButtons() {
		int currentXOffset = -12;
		if (enablePlayerInventoryInChests) {
			int finalCurrentXOffset = currentXOffset;
			StorageScreenBase.addButtonFactory(storageScreenBase -> {
				Position rightTopAbovePlayersInventory = storageScreenBase.getRightTopAbovePlayersInventory();
				return new MiniInventoryButton(storageScreenBase, 0, rightTopAbovePlayersInventory.x() + finalCurrentXOffset, rightTopAbovePlayersInventory.y() - 1, "quark.gui.button.sort_inventory",
						b -> QuarkNetwork.sendToServer(new SortInventoryMessage(true)));
			});
			currentXOffset -= 12;
		}

		int finalCurrentXOffset1 = currentXOffset;
		StorageScreenBase.addButtonFactory(storageScreenBase -> instantiateButton(storageScreenBase, 1, "insert", false, finalCurrentXOffset1));
		currentXOffset -= 12;

		int finalCurrentXOffset2 = currentXOffset;
		StorageScreenBase.addButtonFactory(storageScreenBase -> instantiateButton(storageScreenBase, 2, "extract", true, finalCurrentXOffset2));
		currentXOffset -= 12;

		if (enableShiftLock) {
			int finalCurrentXOffset3 = currentXOffset;
			StorageScreenBase.addButtonFactory(storageScreenBase -> {
				Position rightTopAbovePlayersInventory = storageScreenBase.getRightTopAbovePlayersInventory();
				return new MiniInventoryButton(storageScreenBase, 4, rightTopAbovePlayersInventory.x() + finalCurrentXOffset3, rightTopAbovePlayersInventory.y() - 1, "quark.gui.button.shift_lock",
						b -> shiftLocked = !shiftLocked).setTextureShift(() -> shiftLocked);
			});
		}

		MinecraftForge.EVENT_BUS.addListener(QuarkButtonManager::mouseInputEvent);
		MinecraftForge.EVENT_BUS.addListener(QuarkButtonManager::keyboardInputEvent);
	}

	private static Optional<KeyMapping> getKeyMapping(String mappingName) {
		for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
			if (keyMapping.getName().equals(KEY_MAPPING_PREFIX + mappingName)) {
				return Optional.of(keyMapping);
			}
		}
		return Optional.empty();
	}

	private static void mouseInputEvent(ScreenEvent.MouseClickedEvent.Pre pressed) {
		if (pressed.getScreen() instanceof StorageScreenBase<?>) {
			initMappings();
			mappingsToCheck.forEach((mapping, toRun) -> {
				if (mapping.matchesMouse(pressed.getButton()) &&
						(mapping.getKeyModifier() == KeyModifier.NONE || mapping.getKeyModifier().isActive(KeyConflictContext.GUI))) {
					toRun.run();
					pressed.setCanceled(true);
				}
			});
		}
	}

	private static boolean mappingsInitialized = false;

	private static void initMappings() {
		if (mappingsInitialized) {
			return;
		}
		mappingsInitialized = true;
		if (enablePlayerInventoryInChests) {
			getKeyMapping("sort_player").ifPresent(mapping -> mappingsToCheck.put(mapping, () -> {
				if (satisfyingClick) {
					Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				}
				QuarkNetwork.sendToServer(new SortInventoryMessage(true));
			}));
		}

		getKeyMapping("transfer_insert").ifPresent(mapping -> mappingsToCheck.put(mapping, () -> SophisticatedCore.PACKET_HANDLER.sendToServer(new TransferMessage(false, Screen.hasShiftDown()))));

		getKeyMapping("transfer_extract").ifPresent(mapping -> mappingsToCheck.put(mapping, () -> SophisticatedCore.PACKET_HANDLER.sendToServer(new TransferMessage(true, Screen.hasShiftDown()))));

		if (enableShiftLock) {
			getKeyMapping("shift_lock").ifPresent(mapping -> mappingsToCheck.put(mapping, () ->  shiftLocked = !shiftLocked));
		}
	}

	private static void keyboardInputEvent(ScreenEvent.KeyboardKeyPressedEvent.Post pressed) {
		if (pressed.getScreen() instanceof StorageScreenBase<?>) {
			initMappings();
			mappingsToCheck.forEach((mapping, toRun) -> {
				if (mapping.matches(pressed.getKeyCode(), pressed.getScanCode()) &&
						(mapping.getKeyModifier() == KeyModifier.NONE || mapping.getKeyModifier().isActive(KeyConflictContext.GUI))) {
					toRun.run();
					pressed.setCanceled(true);
				}
			});
		}
	}

	private static AbstractButton instantiateButton(StorageScreenBase<?> screen, int priority, String name, boolean restock, int xOffset) {
		Position rightTopAbovePlayersInventory = screen.getRightTopAbovePlayersInventory();
		return new MiniInventoryButton(screen, priority, rightTopAbovePlayersInventory.x() + xOffset, rightTopAbovePlayersInventory.y() - 1, t -> t.add(I18n.get("quark.gui.button." + name + (Screen.hasShiftDown() ? "_filtered" : ""))),
				b -> SophisticatedCore.PACKET_HANDLER.sendToServer(new TransferMessage(restock, Screen.hasShiftDown()))).setTextureShift(Screen::hasShiftDown);
	}
}