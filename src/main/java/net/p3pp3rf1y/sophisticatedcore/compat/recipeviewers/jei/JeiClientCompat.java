package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticatedcore.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticatedcore.client.render.ItemInStorageHighlightRenderer;

import javax.annotation.Nullable;
import java.util.Optional;

public class JeiClientCompat {
	@Nullable
	private static IJeiRuntime runtime = null;

	public static void setRuntime(@Nullable IJeiRuntime runtime) {
		JeiClientCompat.runtime = runtime;
	}

	public static void init() {
		IEventBus eventBus = NeoForge.EVENT_BUS;
		eventBus.addListener(JeiClientCompat::handleGuiKeyPress);
		eventBus.addListener(JeiClientCompat::handleGuiMouseKeyPress);

		ClientEventHandler.registerHoveredStackSupplier(() -> getStack().orElse(ItemStack.EMPTY));
	}

	private static Optional<ItemStack> getStack() {
		return runtime == null ? Optional.empty() : runtime.getIngredientListOverlay().getIngredientUnderMouse()
				.or(() -> runtime.getBookmarkOverlay().getIngredientUnderMouse())
				.flatMap(ITypedIngredient::getItemStack)
				.or(() -> runtime.getRecipesGui().getIngredientUnderMouse(VanillaTypes.ITEM_STACK));
	}

	public static void handleGuiKeyPress(ScreenEvent.KeyPressed.Pre event) {
		if (runtime == null) {
			return;
		}
		InputConstants.Key key = InputConstants.getKey(event.getKeyEvent());
		if (ClientEventHandler.ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(key) && getStack().map(JeiClientCompat::tryHighlightItem).orElse(false)) {
			event.getScreen().onClose();
			event.setCanceled(true);
		}
	}

	public static void handleGuiMouseKeyPress(ScreenEvent.MouseButtonPressed.Pre event) {
		if (runtime == null) {
			return;
		}
		InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
		if (ClientEventHandler.ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(input) && getStack().map(JeiClientCompat::tryHighlightItem).orElse(false)) {
			event.getScreen().onClose();
			event.setCanceled(true);
		}
	}

	private static boolean tryHighlightItem(ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			return false;
		}

		ItemInStorageHighlightRenderer.highlightItem(player, stack);

		return true;
	}
}
