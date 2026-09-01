package net.p3pp3rf1y.sophisticatedcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStashStorageItem;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiSoundHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.init.ModParticles;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StorageSoundHandler;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.Optional;

import static net.neoforged.neoforge.client.settings.KeyConflictContext.GUI;

public class ClientEventHandler {
	private ClientEventHandler() {
	}

	private static final int MIDDLE_BUTTON = 2;
	private static final String KEYBIND_SOPHISTICATEDCORE_CATEGORY = "key.category.sophisticatedcore.main";
	public static final KeyMapping SORT_KEYBIND = new KeyMapping(TranslationHelper.INSTANCE.translKeybind("sort"),
			SophisticatedScreenKeyConflictContext.INSTANCE, InputConstants.Type.MOUSE.getOrCreate(MIDDLE_BUTTON), KEYBIND_SOPHISTICATEDCORE_CATEGORY);
	public static final KeyMapping TRANSFER_TO_STORAGE_KEYBIND = new KeyMapping(TranslationHelper.INSTANCE.translKeybind("transfer_to_storage"),
			ContainerScreenKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_BRACKET), KEYBIND_SOPHISTICATEDCORE_CATEGORY);
	public static final KeyMapping TRANSFER_TO_INVENTORY_KEYBIND = new KeyMapping(TranslationHelper.INSTANCE.translKeybind("transfer_to_inventory"),
			ContainerScreenKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_BRACKET),
			KEYBIND_SOPHISTICATEDCORE_CATEGORY);

	public static void registerHandlers(IEventBus modBus) {
		modBus.addListener(ModParticles::registerFactories);
		modBus.addListener(ClientEventHandler::registerConditionalItemModelProperties);
		modBus.addListener(ClientEventHandler::registerFluidClientExtension);
		modBus.addListener(ClientEventHandler::registerKeyMappings);
		IEventBus eventBus = NeoForge.EVENT_BUS;
		eventBus.addListener(StorageSoundHandler::tick);
		eventBus.addListener(StorageSoundHandler::onWorldUnload);
		eventBus.addListener(ClientEventHandler::onDrawScreen);
		eventBus.addListener(ClientEventHandler::onContainerScreenForeground);
		eventBus.addListener(ClientEventHandler::onItemTooltip);
		eventBus.addListener(ClientEventHandler::recipesReceived);
		eventBus.addListener(ClientEventHandler::handleGuiKeyPress);
		eventBus.addListener(ClientEventHandler::handleGuiMouseKeyPress);
		eventBus.addListener(ClientEventHandler::renderLevelStage);
		eventBus.addListener(ClientEventHandler::onTickEnd);
	}

	private static void registerConditionalItemModelProperties(RegisterConditionalItemModelPropertyEvent event) {
		event.register(SophisticatedCore.getRL("bound"), EnderLinkerBound.MAP_CODEC);
	}

	private static void onTickEnd(ClientTickEvent.Post event) {
		ItemStackKey.clearCache();
	}

	private static void renderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
			return;
		}
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		BlockHighlightRenderer.render(event.getPoseStack(), partialTick, event.getCamera().getPosition());
	}

	private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(SORT_KEYBIND);
		event.register(TRANSFER_TO_STORAGE_KEYBIND);
		event.register(TRANSFER_TO_INVENTORY_KEYBIND);
	}

	public static void handleGuiKeyPress(ScreenEvent.KeyPressed.Pre event) {
		InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
		boolean shiftDown = Screen.hasShiftDown() || (event.getModifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
		if (SORT_KEYBIND.isActiveAndMatches(key) && tryCallSort(event.getScreen())) {
			GuiSoundHelper.playButtonClickSound();
			event.setCanceled(true);
		} else if (isActiveAndMatchesIgnoringShift(TRANSFER_TO_STORAGE_KEYBIND, key) && tryCallTransferToStorage(event.getScreen(), shiftDown)) {
			GuiSoundHelper.playButtonClickSound();
			event.setCanceled(true);
		} else if (isActiveAndMatchesIgnoringShift(TRANSFER_TO_INVENTORY_KEYBIND, key) && tryCallTransferToInventory(event.getScreen(), shiftDown)) {
			GuiSoundHelper.playButtonClickSound();
			event.setCanceled(true);
		}
	}

	public static void handleGuiMouseKeyPress(ScreenEvent.MouseButtonPressed.Pre event) {
		InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
		if (SORT_KEYBIND.isActiveAndMatches(input) && tryCallSort(event.getScreen())) {
			GuiSoundHelper.playButtonClickSound();
			event.setCanceled(true);
		} else if (isActiveAndMatchesIgnoringShift(TRANSFER_TO_STORAGE_KEYBIND, input) && tryCallTransferToStorage(event.getScreen(), Screen.hasShiftDown())) {
			GuiSoundHelper.playButtonClickSound();
			event.setCanceled(true);
		} else if (isActiveAndMatchesIgnoringShift(TRANSFER_TO_INVENTORY_KEYBIND, input)
				&& tryCallTransferToInventory(event.getScreen(), Screen.hasShiftDown())) {
			GuiSoundHelper.playButtonClickSound();
			event.setCanceled(true);
		}
	}

	public static boolean isActiveAndMatchesIgnoringShift(KeyMapping keyMapping, InputConstants.Key key) {
		return keyMapping.isActiveAndMatches(key)
				|| key != InputConstants.UNKNOWN && key.equals(keyMapping.getKey()) && keyMapping.getKeyConflictContext().isActive()
						&& keyMapping.getKeyModifier() == KeyModifier.NONE && Screen.hasShiftDown() && !Screen.hasControlDown() && !Screen.hasAltDown();
	}

	private static boolean tryCallTransferToStorage(Screen gui, boolean shiftDown) {
		if (gui instanceof StorageScreenBase<?> screen) {
			screen.getMenu().transferItemsToStorage(!shiftDown);
			return true;
		}
		return false;
	}

	private static boolean tryCallTransferToInventory(Screen gui, boolean shiftDown) {
		if (gui instanceof StorageScreenBase<?> screen) {
			screen.getMenu().transferItemsToPlayerInventory(!shiftDown);
			return true;
		}
		return false;
	}

	private static boolean tryCallSort(Screen gui) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.containerMenu instanceof StorageContainerMenuBase<?> container && gui instanceof StorageScreenBase<?> screen) {
			MouseHandler mh = mc.mouseHandler;
			double mouseX = mh.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
			double mouseY = mh.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
			Slot selectedSlot = screen.getHoveredSlot(mouseX, mouseY);
			if (selectedSlot == null || container.isNotPlayersInventorySlot(selectedSlot.index)) {
				container.sort();
				return true;
			}
		}
		return false;
	}

	private static void recipesReceived(RecipesReceivedEvent event) {
		RecipeHelper.setRecipes(event.getRecipeMap());
	}

	private static void onDrawScreen(ScreenEvent.Render.Post event) {
		Minecraft mc = Minecraft.getInstance();
		Screen gui = mc.screen;
		if (!(gui instanceof AbstractContainerScreen<?> containerGui) || gui instanceof CreativeModeInventoryScreen || mc.player == null) {
			return;
		}
		ItemStack held = containerGui.getMenu().getCarried();
		Slot under = containerGui.getSlotUnderMouse();
		if (!held.isEmpty() && under != null && !under.getItem().isEmpty()) {
			getStashResultAndTooltip(under.getItem(), held).filter(stashResultAndTooltip -> under.mayPickup(mc.player))
					.ifPresent(stashResultAndTooltip -> renderSpecialTooltip(event, mc, event.getGuiGraphics(), stashResultAndTooltip));
		}
	}

	private static void onContainerScreenForeground(ContainerScreenEvent.Render.Foreground event) {
		Minecraft mc = Minecraft.getInstance();
		AbstractContainerScreen<?> containerGui = event.getContainerScreen();
		if (containerGui instanceof CreativeModeInventoryScreen || mc.player == null) {
			return;
		}
		AbstractContainerMenu menu = containerGui.getMenu();
		LinkerCraftingDiagnostics.requestIfChanged(menu);
		renderLinkerCraftingDiagnostics(event.getGuiGraphics(), menu);
		ItemStack held = menu.getCarried();
		if (held.isEmpty()) {
			return;
		}

		Slot under = containerGui.getSlotUnderMouse();
		for (Slot s : menu.slots) {
			ItemStack stack = s.getItem();
			if (s == under || stack.isEmpty()) {
				continue;
			}
			getStashResultAndTooltip(stack, held).filter(stashResultAndTooltip -> s.mayPickup(mc.player))
					.ifPresent(stashResultAndTooltip -> renderStashSign(mc, event.getGuiGraphics(), s, stack, stashResultAndTooltip.stashResult()));
		}
	}

	private static void renderLinkerCraftingDiagnostics(GuiGraphics guiGraphics, AbstractContainerMenu menu) {
		for (Slot slot : menu.slots) {
			renderLinkerCraftingDiagnostic(guiGraphics, slot);
		}
		if (menu instanceof StorageContainerMenuBase<?> storageContainer) {
			storageContainer.upgradeSlots.forEach(slot -> renderLinkerCraftingDiagnostic(guiGraphics, slot));
		}
	}

	private static void renderLinkerCraftingDiagnostic(GuiGraphics guiGraphics, Slot slot) {
		if (LinkerCraftingDiagnostics.getStatusMessage(slot.index) != null) {
			guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x80FF0000);
		}
	}

	private static void onItemTooltip(ItemTooltipEvent event) {
		if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) {
			return;
		}
		Slot slot = screen.getSlotUnderMouse();
		if (slot == null || slot.getItem() != event.getItemStack()) {
			return;
		}
		String statusMessage = LinkerCraftingDiagnostics.getStatusMessage(slot.index);
		if (statusMessage != null) {
			event.getToolTip().add(TranslationHelper.INSTANCE.translStatusMessage(statusMessage).withStyle(ChatFormatting.RED));
		}
	}

	private static void renderStashSign(Minecraft mc, GuiGraphics guiGraphics, Slot s, ItemStack stack, IStashStorageItem.StashResult stashResult) {
		int x = s.x;
		int y = s.y;

		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0, 0, 300);

		int color = stashResult == IStashStorageItem.StashResult.MATCH_AND_SPACE ? ChatFormatting.GREEN.getColor() : 0xFFFF00;
		if (stack.getItem() instanceof IStashStorageItem) {
			guiGraphics.drawString(mc.font, "+", x + 10, y + 8, color);
		} else {
			guiGraphics.drawString(mc.font, "-", x + 1, y, color);
		}
		poseStack.popPose();
	}

	private static void renderSpecialTooltip(ScreenEvent.Render.Post event, Minecraft mc, GuiGraphics guiGraphics,
			StashResultAndTooltip stashResultAndTooltip) {
		int x = event.getMouseX();
		int y = event.getMouseY();
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0, 0, 100);
		guiGraphics.renderTooltip(mc.font,
				Collections.singletonList(Component.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".right_click_to_add_to_storage")),
				stashResultAndTooltip.tooltip(), x, y);
		poseStack.popPose();
	}

	private static Optional<StashResultAndTooltip> getStashResultAndTooltip(ItemStack inInventory, ItemStack held) {
		if (inInventory.getCount() == 1 && inInventory.getItem() instanceof IStashStorageItem stashStorageItem) {
			return getStashResultAndTooltip(inInventory, held, stashStorageItem);
		}

		if (held.getItem() instanceof IStashStorageItem stashStorageItem) {
			return getStashResultAndTooltip(held, inInventory, stashStorageItem);
		}
		return Optional.empty();
	}

	private static Optional<StashResultAndTooltip> getStashResultAndTooltip(ItemStack potentialStashStorage, ItemStack potentiallyStashable,
			IStashStorageItem stashStorageItem) {
		IStashStorageItem.StashResult stashResult = stashStorageItem.getItemStashable(Minecraft.getInstance().level.registryAccess(), potentialStashStorage,
				potentiallyStashable);
		if (stashResult == IStashStorageItem.StashResult.NO_SPACE) {
			return Optional.empty();
		}
		return Optional.of(new StashResultAndTooltip(stashResult, stashStorageItem.getInventoryTooltip(potentialStashStorage)));
	}

	private record StashResultAndTooltip(IStashStorageItem.StashResult stashResult, Optional<TooltipComponent> tooltip) {
	}

	private static void registerFluidClientExtension(RegisterClientExtensionsEvent event) {
		event.registerFluidType(new IClientFluidTypeExtensions() {
			private static final ResourceLocation XP_STILL_TEXTURE = ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "block/xp_still");
			private static final ResourceLocation XP_FLOWING_TEXTURE = ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "block/xp_flowing");

			@Override
			public ResourceLocation getStillTexture() {
				return XP_STILL_TEXTURE;
			}

			@Override
			public ResourceLocation getFlowingTexture() {
				return XP_FLOWING_TEXTURE;
			}
		}, ModFluids.XP_FLUID_TYPE.get());
	}

	private static class SophisticatedScreenKeyConflictContext implements IKeyConflictContext {
		public static final SophisticatedScreenKeyConflictContext INSTANCE = new SophisticatedScreenKeyConflictContext();

		@Override
		public boolean isActive() {
			return GUI.isActive() && Minecraft.getInstance().screen instanceof StorageScreenBase<?>;
		}

		@Override
		public boolean conflicts(IKeyConflictContext other) {
			return this == other;
		}
	}

	private static class ContainerScreenKeyConflictContext implements IKeyConflictContext {
		public static final ContainerScreenKeyConflictContext INSTANCE = new ContainerScreenKeyConflictContext();

		@Override
		public boolean isActive() {
			return GUI.isActive() && Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>;
		}

		@Override
		public boolean conflicts(IKeyConflictContext other) {
			return this == other;
		}
	}
}
