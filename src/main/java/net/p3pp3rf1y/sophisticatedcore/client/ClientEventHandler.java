package net.p3pp3rf1y.sophisticatedcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStashStorageItem;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.init.ModParticles;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderHelper;
import net.p3pp3rf1y.sophisticatedcore.client.render.ItemInStorageHighlightRenderer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StorageSoundHandler;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static net.neoforged.neoforge.client.settings.KeyConflictContext.GUI;
import static net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME;

public class ClientEventHandler {
	private ClientEventHandler() {
	}

	private static final String KEYBIND_SOPHISTICATEDCORE_CATEGORY = "keybind.sophisticatedcore.category";
	public static final KeyMapping ITEM_HIGHLIGHT_KEYBIND = new KeyMapping(TranslationHelper.INSTANCE.translKeybind("item_highlight"),
			ItemHighlightKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_SEMICOLON), KEYBIND_SOPHISTICATEDCORE_CATEGORY);

	public static void registerHandlers(IEventBus modBus) {
		modBus.addListener(ModParticles::registerFactories);
		modBus.addListener(ClientEventHandler::registerFluidClientExtension);
		modBus.addListener(ClientEventHandler::registerKeyMappings);
		modBus.addListener(ClientEventHandler::registerRenderPipelines);
		IEventBus eventBus = NeoForge.EVENT_BUS;
		eventBus.addListener(StorageSoundHandler::tick);
		eventBus.addListener(StorageSoundHandler::onWorldUnload);
		eventBus.addListener(ClientEventHandler::onDrawScreen);
		eventBus.addListener(ClientEventHandler::recipesReceived);
		eventBus.addListener(ClientEventHandler::handleGuiKeyPress);
		eventBus.addListener(ClientEventHandler::handleGuiMouseKeyPress);
		eventBus.addListener(ClientEventHandler::handleKeyInput);
		eventBus.addListener(ClientEventHandler::renderLevelStage);
	}

	private static void renderLevelStage(RenderLevelStageEvent.AfterBlockEntities event) {
		ItemInStorageHighlightRenderer.render(event.getPoseStack(), event.getPartialTick().getGameTimeDeltaPartialTick(false), event.getCamera().getPosition());
	}

	private static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
		event.registerPipeline(BlockHighlightRenderHelper.THICK_HIGHLIGHT_PIPELINE);
	}

	private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(ITEM_HIGHLIGHT_KEYBIND);
	}
	public static void handleKeyInput(ClientTickEvent.Post event) {
		if (ITEM_HIGHLIGHT_KEYBIND.consumeClick()) {
			tryHighlightItem();
		}
	}

	public static void handleGuiKeyPress(ScreenEvent.KeyPressed.Pre event) {
		InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
		if (ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(key) && event.getScreen() instanceof AbstractContainerScreen screen && tryHighlightItem(screen.getSlotUnderMouse())) {
			screen.onClose();
			event.setCanceled(true);
		}
	}

	public static void handleGuiMouseKeyPress(ScreenEvent.MouseButtonPressed.Pre event) {
		InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
		if (ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(input) && event.getScreen() instanceof AbstractContainerScreen screen && tryHighlightItem(screen.getSlotUnderMouse())) {
			screen.onClose();
			event.setCanceled(true);
		}
	}

	private static boolean tryHighlightItem(@Nullable Slot slot) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (slot == null || player == null || slot.getItem().isEmpty()) {
			return false;
		}

		ItemInStorageHighlightRenderer.highlightItem(player, slot.getItem());

		return true;
	}

	private static void tryHighlightItem() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || player.getMainHandItem().isEmpty()) {
			return;
		}

		ItemInStorageHighlightRenderer.highlightItem(player, player.getMainHandItem());
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
		AbstractContainerMenu menu = containerGui.getMenu();
		ItemStack held = menu.getCarried();
		if (!held.isEmpty()) {
			List<Slot> slots = menu instanceof StorageContainerMenuBase<?> storageMenu ? storageMenu.realInventorySlots : menu.slots;

			for (Slot s : slots) {
				ItemStack stack = s.getItem();
				if (!s.mayPickup(mc.player) || stack.isEmpty()) {
					continue;
				}
				Optional<IStashStorageItem.StashResult> stashResult = getStashResult(stack, held);
				if (stashResult.isEmpty()) {
					continue;
				}

				if (s != containerGui.getSlotUnderMouse()) {
					renderStashSign(mc, containerGui, event.getGuiGraphics(), s, stack, stashResult.get());
				}
			}
			renderStashTooltip(event, containerGui, held);
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

	private static void renderStashSign(Minecraft mc, AbstractContainerScreen<?> containerGui, GuiGraphics guiGraphics, Slot s, ItemStack stack, IStashStorageItem.StashResult stashResult) {
		int x = containerGui.getGuiLeft() + s.x;
		int y = containerGui.getGuiTop() + s.y;

		int color = ARGB.opaque(stashResult == IStashStorageItem.StashResult.MATCH_AND_SPACE ? ChatFormatting.GREEN.getColor() : 0xFFFF00);
		if (stack.getItem() instanceof IStashStorageItem) {
			guiGraphics.drawString(mc.font, "+", x + 10, y + 8, color);
		} else {
			guiGraphics.drawString(mc.font, "-", x + 1, y, color);
		}
	}

	private static void renderSpecialTooltip(ScreenEvent.Render.Post event, GuiGraphics guiGraphics, ItemStack stack, Optional<TooltipComponent> tooltipComponent) {
		int x = event.getMouseX();
		int y = event.getMouseY();
		GuiHelper.renderTooltip(event.getScreen(), guiGraphics, stack, Collections.singletonList(Component.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".right_click_to_add_to_storage")), tooltipComponent, x, y);
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

	private static Optional<IStashStorageItem.StashResult> getStashResult(ItemStack potentialStashStorage, ItemStack potentiallyStashable, IStashStorageItem stashStorageItem) {
		IStashStorageItem.StashResult stashResult = stashStorageItem.getItemStashable(Minecraft.getInstance().level.registryAccess(), potentialStashStorage, potentiallyStashable);
		if (stashResult == IStashStorageItem.StashResult.NO_SPACE) {
			return Optional.empty();
		}
		return Optional.of(stashResult);
	}

	private static Optional<TooltipComponent> getStashTooltip(ItemStack potentialStashStorage, ItemStack potentiallyStashable, IStashStorageItem stashStorageItem) {
		if (stashStorageItem.getItemStashable(Minecraft.getInstance().level.registryAccess(), potentialStashStorage, potentiallyStashable) == IStashStorageItem.StashResult.NO_SPACE) {
			return Optional.empty();
		}
		return stashStorageItem.getInventoryTooltip(potentialStashStorage);
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

	private static class ItemHighlightKeyConflictContext implements IKeyConflictContext {
		public static final ItemHighlightKeyConflictContext INSTANCE = new ItemHighlightKeyConflictContext();

		@Override
		public boolean isActive() {
			return (IN_GAME.isActive() && !Minecraft.getInstance().player.getMainHandItem().isEmpty()) || GUI.isActive();
		}

		@Override
		public boolean conflicts(IKeyConflictContext other) {
			return this == other;
		}
	}
}
