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
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStashStorageItem;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.init.ModParticles;
import net.p3pp3rf1y.sophisticatedcore.client.render.BlockHighlightRenderer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StorageSoundHandler;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static net.neoforged.neoforge.client.settings.KeyConflictContext.GUI;

public class ClientEventHandler {
	private ClientEventHandler() {
	}

	private static final int MIDDLE_BUTTON = 2;
	private static final String KEYBIND_SOPHISTICATEDCORE_CATEGORY = "key.category.sophisticatedcore.main";
	public static final KeyMapping SORT_KEYBIND = new KeyMapping(TranslationHelper.INSTANCE.translKeybind("sort"),
			SophisticatedScreenKeyConflictContext.INSTANCE, InputConstants.Type.MOUSE.getOrCreate(MIDDLE_BUTTON), KEYBIND_SOPHISTICATEDCORE_CATEGORY);

	public static void registerHandlers(IEventBus modBus) {
		modBus.addListener(ModParticles::registerFactories);
		modBus.addListener(ClientEventHandler::registerFluidClientExtension);
		modBus.addListener(ClientEventHandler::registerKeyMappings);
		IEventBus eventBus = NeoForge.EVENT_BUS;
		eventBus.addListener(StorageSoundHandler::tick);
		eventBus.addListener(StorageSoundHandler::onWorldUnload);
		eventBus.addListener(ClientEventHandler::onDrawScreen);
		eventBus.addListener(ClientEventHandler::handleGuiKeyPress);
		eventBus.addListener(ClientEventHandler::handleGuiMouseKeyPress);
		eventBus.addListener(ClientEventHandler::renderLevelStage);
		eventBus.addListener(ClientEventHandler::onTickEnd);
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
	}

	public static void handleGuiKeyPress(ScreenEvent.KeyPressed.Pre event) {
		InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
		if (SORT_KEYBIND.isActiveAndMatches(key) && tryCallSort(event.getScreen())) {
			event.setCanceled(true);
		}
	}

	public static void handleGuiMouseKeyPress(ScreenEvent.MouseButtonPressed.Pre event) {
		InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
		if (SORT_KEYBIND.isActiveAndMatches(input) && tryCallSort(event.getScreen())) {
			event.setCanceled(true);
		}
	}

	private static boolean tryCallSort(Screen gui) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.containerMenu instanceof StorageContainerMenuBase<?> container && gui instanceof StorageScreenBase<?> screen) {
			MouseHandler mh = mc.mouseHandler;
			double mouseX = mh.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
			double mouseY = mh.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
			Slot selectedSlot = screen.findSlot(mouseX, mouseY);
			if (selectedSlot == null || container.isNotPlayersInventorySlot(selectedSlot.index)) {
				container.sort();
				return true;
			}
		}
		return false;
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
			Slot under = containerGui.getSlotUnderMouse();

			List<Slot> slots = menu instanceof StorageContainerMenuBase<?> storageMenu ? storageMenu.realInventorySlots : menu.slots;

			for (Slot s : slots) {
				ItemStack stack = s.getItem();
				if (!s.mayPickup(mc.player) || stack.isEmpty()) {
					continue;
				}
				Optional<StashResultAndTooltip> stashResultAndTooltip = getStashResultAndTooltip(stack, held);
				if (stashResultAndTooltip.isEmpty()) {
					continue;
				}

				if (s == under) {
					renderSpecialTooltip(event, mc, event.getGuiGraphics(), stashResultAndTooltip.get());
				} else {
					renderStashSign(mc, containerGui, event.getGuiGraphics(), s, stack, stashResultAndTooltip.get().stashResult());
				}
			}
		}
	}

	private static void renderStashSign(Minecraft mc, AbstractContainerScreen<?> containerGui, GuiGraphics guiGraphics, Slot s, ItemStack stack, IStashStorageItem.StashResult stashResult) {
		int x = containerGui.getGuiLeft() + s.x;
		int y = containerGui.getGuiTop() + s.y;

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

	private static void renderSpecialTooltip(ScreenEvent.Render.Post event, Minecraft mc, GuiGraphics guiGraphics, StashResultAndTooltip stashResultAndTooltip) {
		int x = event.getMouseX();
		int y = event.getMouseY();
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0, 0, 100);
		guiGraphics.renderTooltip(mc.font, Collections.singletonList(Component.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".right_click_to_add_to_storage")), stashResultAndTooltip.tooltip(), x, y);
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

	private static Optional<StashResultAndTooltip> getStashResultAndTooltip(ItemStack potentialStashStorage, ItemStack potentiallyStashable, IStashStorageItem stashStorageItem) {
		IStashStorageItem.StashResult stashResult = stashStorageItem.getItemStashable(Minecraft.getInstance().level.registryAccess(), potentialStashStorage, potentiallyStashable);
		if (stashResult == IStashStorageItem.StashResult.NO_SPACE) {
			return Optional.empty();
		}
		return Optional.of(new StashResultAndTooltip(stashResult, stashStorageItem.getInventoryTooltip(potentialStashStorage)));
	}

	private record StashResultAndTooltip(IStashStorageItem.StashResult stashResult,
										 Optional<TooltipComponent> tooltip) {
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
}
