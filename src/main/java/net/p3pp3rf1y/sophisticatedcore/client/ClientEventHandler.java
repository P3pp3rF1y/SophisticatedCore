package net.p3pp3rf1y.sophisticatedcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
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
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStashStorageItem;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiSoundHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.init.ModParticles;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderHelper;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderer;
import net.p3pp3rf1y.sophisticatedcore.client.render.ItemDisplayPreviewRenderState;
import net.p3pp3rf1y.sophisticatedcore.client.render.ItemDisplayPreviewRenderer;
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
	private static final KeyMapping.Category SOPHISTICATEDCORE_CATEGORY = new KeyMapping.Category(SophisticatedCore.getIdentifier("main"));
	public static final KeyMapping SORT_KEYBIND = new KeyMapping(TranslationHelper.INSTANCE.translKeybind("sort"),
			SophisticatedScreenKeyConflictContext.INSTANCE, InputConstants.Type.MOUSE.getOrCreate(MIDDLE_BUTTON), SOPHISTICATEDCORE_CATEGORY);
	public static final KeyMapping TRANSFER_TO_STORAGE_KEYBIND = new KeyMapping(TranslationHelper.INSTANCE.translKeybind("transfer_to_storage"),
			ContainerScreenKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_BRACKET), SOPHISTICATEDCORE_CATEGORY);
	public static final KeyMapping TRANSFER_TO_INVENTORY_KEYBIND = new KeyMapping(TranslationHelper.INSTANCE.translKeybind("transfer_to_inventory"),
			ContainerScreenKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_BRACKET), SOPHISTICATEDCORE_CATEGORY);

	public static void registerHandlers(IEventBus modBus) {
		modBus.addListener(ModParticles::registerFactories);
		modBus.addListener(ClientEventHandler::registerFluidClientExtension);
		modBus.addListener(ClientEventHandler::registerFluidModels);
		modBus.addListener(ClientEventHandler::registerKeyMappings);
		modBus.addListener(ClientEventHandler::registerRenderPipelines);
		modBus.addListener(ClientEventHandler::registerPictureInPictureRenderers);
		IEventBus eventBus = NeoForge.EVENT_BUS;
		eventBus.addListener(StorageSoundHandler::tick);
		eventBus.addListener(StorageSoundHandler::onWorldUnload);
		eventBus.addListener(ClientEventHandler::onDrawScreen);
		eventBus.addListener(ClientEventHandler::onContainerScreenForeground);
		eventBus.addListener(ClientEventHandler::recipesReceived);
		eventBus.addListener(ClientEventHandler::handleGuiKeyPress);
		eventBus.addListener(ClientEventHandler::handleGuiMouseKeyPress);
		eventBus.addListener(ClientEventHandler::submitCustomGeometry);
		eventBus.addListener(ClientEventHandler::onTickEnd);
	}

	private static void onTickEnd(ClientTickEvent.Post event) {
		ItemStackKey.clearCache();
	}

	private static void submitCustomGeometry(SubmitCustomGeometryEvent event) {
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		BlockHighlightRenderer.submit(event.getSubmitNodeCollector(), event.getPoseStack(), partialTick, event.getLevelRenderState().cameraRenderState.pos);
	}

	private static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
		event.registerPipeline(BlockHighlightRenderHelper.THICK_HIGHLIGHT_PIPELINE);
	}

	private static void registerPictureInPictureRenderers(RegisterPictureInPictureRenderersEvent event) {
		event.register(ItemDisplayPreviewRenderState.class, ItemDisplayPreviewRenderer::new);
	}

	private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.registerCategory(SOPHISTICATEDCORE_CATEGORY);
		event.register(SORT_KEYBIND);
		event.register(TRANSFER_TO_STORAGE_KEYBIND);
		event.register(TRANSFER_TO_INVENTORY_KEYBIND);
	}

	public static void handleGuiKeyPress(ScreenEvent.KeyPressed.Pre event) {
		InputConstants.Key key = InputConstants.getKey(event.getKeyEvent());
		if (SORT_KEYBIND.isActiveAndMatches(key) && tryCallSort(event.getScreen())) {
			GuiSoundHelper.playButtonClickSound();
			event.setCanceled(true);
		} else if (isActiveAndMatchesIgnoringShift(TRANSFER_TO_STORAGE_KEYBIND, key) && tryCallTransferToStorage(event.getScreen())) {
			GuiSoundHelper.playButtonClickSound();
			event.setCanceled(true);
		} else if (isActiveAndMatchesIgnoringShift(TRANSFER_TO_INVENTORY_KEYBIND, key) && tryCallTransferToInventory(event.getScreen())) {
			GuiSoundHelper.playButtonClickSound();
			event.setCanceled(true);
		}
	}

	public static void handleGuiMouseKeyPress(ScreenEvent.MouseButtonPressed.Pre event) {
		InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
		if (SORT_KEYBIND.isActiveAndMatches(input) && tryCallSort(event.getScreen())) {
			GuiSoundHelper.playButtonClickSound();
			event.setCanceled(true);
		} else if (isActiveAndMatchesIgnoringShift(TRANSFER_TO_STORAGE_KEYBIND, input) && tryCallTransferToStorage(event.getScreen())) {
			GuiSoundHelper.playButtonClickSound();
			event.setCanceled(true);
		} else if (isActiveAndMatchesIgnoringShift(TRANSFER_TO_INVENTORY_KEYBIND, input) && tryCallTransferToInventory(event.getScreen())) {
			GuiSoundHelper.playButtonClickSound();
			event.setCanceled(true);
		}
	}

	public static boolean isActiveAndMatchesIgnoringShift(KeyMapping keyMapping, InputConstants.Key key) {
		Minecraft mc = Minecraft.getInstance();
		return keyMapping.isActiveAndMatches(key)
				|| key != InputConstants.UNKNOWN && key.equals(keyMapping.getKey()) && keyMapping.getKeyConflictContext().isActive()
						&& keyMapping.getKeyModifier() == KeyModifier.NONE && mc.hasShiftDown() && !mc.hasControlDown() && !mc.hasAltDown();
	}

	private static boolean tryCallTransferToStorage(Screen gui) {
		if (gui instanceof StorageScreenBase<?> screen) {
			screen.getMenu().transferItemsToStorage(!Minecraft.getInstance().hasShiftDown());
			return true;
		}
		return false;
	}

	private static boolean tryCallTransferToInventory(Screen gui) {
		if (gui instanceof StorageScreenBase<?> screen) {
			screen.getMenu().transferItemsToPlayerInventory(!Minecraft.getInstance().hasShiftDown());
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
		Screen gui = mc.gui.screen();
		if (!(gui instanceof AbstractContainerScreen<?> containerGui) || gui instanceof CreativeModeInventoryScreen || mc.player == null) {
			return;
		}
		ItemStack held = containerGui.getMenu().getCarried();
		if (!held.isEmpty()) {
			renderStashTooltip(event, containerGui, held);
		}
	}

	private static void onContainerScreenForeground(ContainerScreenEvent.Render.Foreground event) {
		Minecraft mc = Minecraft.getInstance();
		AbstractContainerScreen<?> containerGui = event.getContainerScreen();
		if (containerGui instanceof CreativeModeInventoryScreen || mc.player == null) {
			return;
		}
		AbstractContainerMenu menu = containerGui.getMenu();
		ItemStack held = menu.getCarried();
		if (held.isEmpty()) {
			return;
		}

		Slot under = containerGui.getSlotUnderMouse();
		for (Slot s : menu.slots) {
			ItemStack stack = s.getItem();
			if (s == under || !s.mayPickup(mc.player) || stack.isEmpty()) {
				continue;
			}
			getStashResult(stack, held).ifPresent(stashResult -> renderStashSign(mc, event.getGuiGraphics(), s, stack, stashResult));
		}
	}

	private static void renderStashTooltip(ScreenEvent.Render.Post event, AbstractContainerScreen<?> containerGui, ItemStack held) {
		Slot under = containerGui.getSlotUnderMouse();
		if (under != null) {
			ItemStack inInventory = under.getItem();
			if (inInventory.getCount() == 1 && inInventory.getItem() instanceof IStashStorageItem stashStorageItem) {
				renderSpecialTooltip(event, event.getGuiGraphics(), held, getStashTooltip(inInventory, held, stashStorageItem));
			} else if (held.getItem() instanceof IStashStorageItem stashStorageItem) {
				renderSpecialTooltip(event, event.getGuiGraphics(), inInventory, getStashTooltip(held, inInventory, stashStorageItem));
			}
		}
	}

	private static void renderStashSign(Minecraft mc, GuiGraphicsExtractor guiGraphics, Slot s, ItemStack stack, IStashStorageItem.StashResult stashResult) {
		int x = s.x;
		int y = s.y;

		int color = ARGB.opaque(stashResult == IStashStorageItem.StashResult.MATCH_AND_SPACE ? TextColor.GREEN.getValue() : 0xFFFF00);
		if (stack.getItem() instanceof IStashStorageItem) {
			guiGraphics.text(mc.font, "+", x + 10, y + 8, color);
		} else {
			guiGraphics.text(mc.font, "-", x + 1, y, color);
		}
	}

	private static void renderSpecialTooltip(ScreenEvent.Render.Post event, GuiGraphicsExtractor guiGraphics, ItemStack stack,
			Optional<TooltipComponent> tooltipComponent) {
		int x = event.getMouseX();
		int y = event.getMouseY();
		GuiHelper.extractTooltip(event.getScreen(), guiGraphics, stack,
				Collections.singletonList(Component.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".right_click_to_add_to_storage")),
				tooltipComponent, x, y);
	}

	private static Optional<IStashStorageItem.StashResult> getStashResult(ItemStack inInventory, ItemStack held) {
		if (inInventory.getCount() == 1 && inInventory.getItem() instanceof IStashStorageItem stashStorageItem) {
			return getStashResult(inInventory, held, stashStorageItem);
		}

		if (held.getItem() instanceof IStashStorageItem stashStorageItem) {
			return getStashResult(held, inInventory, stashStorageItem);
		}
		return Optional.empty();
	}

	private static Optional<IStashStorageItem.StashResult> getStashResult(ItemStack potentialStashStorage, ItemStack potentiallyStashable,
			IStashStorageItem stashStorageItem) {
		IStashStorageItem.StashResult stashResult = stashStorageItem.getItemStashable(Minecraft.getInstance().level.registryAccess(), potentialStashStorage,
				potentiallyStashable);
		if (stashResult == IStashStorageItem.StashResult.NO_SPACE) {
			return Optional.empty();
		}
		return Optional.of(stashResult);
	}

	private static Optional<TooltipComponent> getStashTooltip(ItemStack potentialStashStorage, ItemStack potentiallyStashable,
			IStashStorageItem stashStorageItem) {
		if (stashStorageItem.getItemStashable(Minecraft.getInstance().level.registryAccess(), potentialStashStorage,
				potentiallyStashable) == IStashStorageItem.StashResult.NO_SPACE) {
			return Optional.empty();
		}
		return stashStorageItem.getInventoryTooltip(potentialStashStorage);
	}

	private static void registerFluidClientExtension(RegisterClientExtensionsEvent event) {
		event.registerFluidType(new IClientFluidTypeExtensions() {
			private static final Identifier XP_STILL_TEXTURE = Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "block/xp_still");
			private static final Identifier XP_FLOWING_TEXTURE = Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "block/xp_flowing");

			public Identifier getStillTexture() {
				return XP_STILL_TEXTURE;
			}

			public Identifier getFlowingTexture() {
				return XP_FLOWING_TEXTURE;
			}
		}, ModFluids.XP_FLUID_TYPE.get());
	}

	private static void registerFluidModels(RegisterFluidModelsEvent event) {
		event.register(new FluidModel.Unbaked(
				new net.minecraft.client.resources.model.sprite.Material(Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "block/xp_still")),
				new net.minecraft.client.resources.model.sprite.Material(Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "block/xp_flowing")), null,
				null), ModFluids.XP_STILL.get(), ModFluids.XP_FLOWING.get());
	}

	private static class SophisticatedScreenKeyConflictContext implements IKeyConflictContext {
		public static final SophisticatedScreenKeyConflictContext INSTANCE = new SophisticatedScreenKeyConflictContext();

		@Override
		public boolean isActive() {
			return GUI.isActive() && Minecraft.getInstance().gui.screen() instanceof StorageScreenBase<?>;
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
			return GUI.isActive() && Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?>;
		}

		@Override
		public boolean conflicts(IKeyConflictContext other) {
			return this == other;
		}
	}
}
