package net.p3pp3rf1y.sophisticatedcore.compat.craftingtweaks;

import net.blay09.mods.craftingtweaks.CraftingTweaksProviderManager;
import net.blay09.mods.craftingtweaks.api.CraftingTweaksClientAPI;
import net.blay09.mods.craftingtweaks.api.TweakType;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.ICraftingUIPart;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CraftingUpgradeTweakUIPart implements ICraftingUIPart {
	private StorageScreenBase<?> storageScreen;

	private static final Method ADD_RENDERABLE_WIDGET = ObfuscationReflectionHelper.findMethod(Screen.class, "addRenderableWidget", GuiEventListener.class);

	private final List<AbstractWidget> buttons = new ArrayList<>();

	public static void register() {
		StorageScreenBase.setCraftingUIPart(new CraftingUpgradeTweakUIPart());
	}

	private void addButton(AbstractWidget button) {
		buttons.add(button);
		try {
			ADD_RENDERABLE_WIDGET.invoke(storageScreen, button);
		} catch (IllegalAccessException | InvocationTargetException e) {
			SophisticatedCore.LOGGER.error("Error calling addButton in Screen class", e);
		}
	}

	@Override
	public void onCraftingSlotsHidden() {
		if (buttons.isEmpty() || storageScreen == null) {
			return;
		}

		buttons.forEach(storageScreen.children()::remove);
		buttons.forEach(storageScreen.renderables::remove);
		buttons.clear();
	}

	@Override
	public int getWidth() {
		return 18;
	}

	@Override
	public void setStorageScreen(StorageScreenBase<?> screen) {
		storageScreen = screen;
	}

	@Override
	public void onCraftingSlotsDisplayed(List<Slot> slots) {
		if (slots.isEmpty() || storageScreen == null) {
			return;
		}
		Slot firstSlot = slots.getFirst();
		CraftingTweaksProviderManager.getDefaultCraftingGrid(storageScreen.getMenu()).ifPresent(craftingGrid -> {
			addButton(CraftingTweaksClientAPI.createTweakButtonRelative(craftingGrid, storageScreen, getButtonX(firstSlot), getButtonY(firstSlot, 0),
					TweakType.Rotate));
			addButton(CraftingTweaksClientAPI.createTweakButtonRelative(craftingGrid, storageScreen, getButtonX(firstSlot), getButtonY(firstSlot, 1),
					TweakType.Balance));
			addButton(CraftingTweaksClientAPI.createTweakButtonRelative(craftingGrid, storageScreen, getButtonX(firstSlot), getButtonY(firstSlot, 2),
					TweakType.Clear));
		});
	}

	private int getButtonX(Slot firstSlot) {
		return firstSlot.x - 19;
	}

	private int getButtonY(Slot firstSlot, int index) {
		return firstSlot.y + 18 * index;
	}
}
