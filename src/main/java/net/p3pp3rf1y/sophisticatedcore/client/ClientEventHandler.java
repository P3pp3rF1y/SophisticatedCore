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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStashStorageItem;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.init.ModParticles;
import net.p3pp3rf1y.sophisticatedcore.client.render.ItemFlightAnimator;
import net.p3pp3rf1y.sophisticatedcore.client.render.ItemInStorageHighlightRenderer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StorageSoundHandler;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static net.minecraftforge.client.settings.KeyConflictContext.GUI;
import static net.minecraftforge.client.settings.KeyConflictContext.IN_GAME;

public class ClientEventHandler {
	private ClientEventHandler() {}

	private static final int MIDDLE_BUTTON = 2;
	private static final String KEYBIND_SOPHISTICATEDCORE_CATEGORY = "keybind.sophisticatedcore.category";
	public static final KeyMapping ITEM_HIGHLIGHT_KEYBIND = new KeyMapping(TranslationHelper.INSTANCE.translKeybind("item_highlight"),
			ItemHighlightKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_SEMICOLON), KEYBIND_SOPHISTICATEDCORE_CATEGORY);
	public static final KeyMapping ITEM_DEPOSIT_KEYBIND = new KeyMapping(TranslationHelper.INSTANCE.translKeybind("deposit_item"),
			ItemHighlightKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_APOSTROPHE), KEYBIND_SOPHISTICATEDCORE_CATEGORY);
	public static final KeyMapping ITEM_RESTOCK_KEYBIND = new KeyMapping(TranslationHelper.INSTANCE.translKeybind("restock_item"),
			ItemHighlightKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_BACKSLASH), KEYBIND_SOPHISTICATEDCORE_CATEGORY);
	public static final KeyMapping SORT_KEYBIND = new KeyMapping(TranslationHelper.INSTANCE.translKeybind("sort"),
			SophisticatedScreenKeyConflictContext.INSTANCE, InputConstants.Type.MOUSE.getOrCreate(MIDDLE_BUTTON), KEYBIND_SOPHISTICATEDCORE_CATEGORY);

	private static final List<Supplier<ItemStack>> HOVERED_STACK_SUPPLIERS = new ArrayList<>();
	public static void registerHoveredStackSupplier(Supplier<ItemStack> stackSupplier) {
		HOVERED_STACK_SUPPLIERS.add(stackSupplier);
	}

	public static void registerHandlers() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(ModParticles::registerFactories);
		modBus.addListener(ClientEventHandler::registerKeyMappings);
		IEventBus eventBus = MinecraftForge.EVENT_BUS;
		eventBus.addListener(StorageSoundHandler::tick);
		eventBus.addListener(StorageSoundHandler::onWorldUnload);
		eventBus.addListener(ClientEventHandler::onDrawScreen);
		eventBus.addListener(ClientEventHandler::handleGuiKeyPress);
		eventBus.addListener(ClientEventHandler::handleGuiMouseKeyPress);
		eventBus.addListener(ClientEventHandler::handleKeyInput);
		eventBus.addListener(ClientEventHandler::onPostClientTick);
		eventBus.addListener(ClientEventHandler::renderLevelStage);
	}

	private static void renderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
			return;
		}
		float partialTick = event.getPartialTick();
		ItemInStorageHighlightRenderer.render(event.getPoseStack(), partialTick, event.getCamera().getPosition());
		ItemFlightAnimator.render(event.getPoseStack(), partialTick, event.getCamera().getPosition());
	}

	private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(ITEM_HIGHLIGHT_KEYBIND);
		event.register(SORT_KEYBIND);
		event.register(ITEM_DEPOSIT_KEYBIND);
		event.register(ITEM_RESTOCK_KEYBIND);
	}
	public static void onPostClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		if (ITEM_HIGHLIGHT_KEYBIND.consumeClick()) {
			tryHighlightItem();
		}
	}

	public static void handleKeyInput(InputEvent.Key event) {
		if (ITEM_DEPOSIT_KEYBIND.getKey().getValue() == event.getKey() && event.getAction() == GLFW.GLFW_PRESS) {
			tryDepositItem(event);
		} else if (ITEM_RESTOCK_KEYBIND.getKey().getValue() == event.getKey() && event.getAction() == GLFW.GLFW_PRESS) {
			tryRestockItem(event);
		}
	}

	private static ItemStack getHoveredStack() {
		for (Supplier<ItemStack> supplier : HOVERED_STACK_SUPPLIERS) {
			ItemStack stack = supplier.get();
			if (!stack.isEmpty()) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static void tryRestockItem(InputEvent.Key event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		int mods = event.getModifiers();
		boolean mainInventory = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
		boolean hotbar = (mods & GLFW.GLFW_MOD_ALT) != 0;
		boolean fillEmpty = (mods & GLFW.GLFW_MOD_CONTROL) != 0;

		Screen screen = Minecraft.getInstance().screen;
		ItemStack filter = getHoveredStack();
		int slot;
		if (filter.isEmpty()) {
			if (screen instanceof AbstractContainerScreen<?> containerScreen) {
				Slot slotUnderMouse = containerScreen.getSlotUnderMouse();
				if (slotUnderMouse != null) {
					filter = slotUnderMouse.getItem();
					slot = slotUnderMouse.getSlotIndex();
				} else {
					filter = ItemStack.EMPTY;
					slot = player.getInventory().selected;
				}
			} else {
				filter = player.getMainHandItem();
				slot = player.getInventory().selected;
			}
		} else {
			slot = player.getInventory().getFreeSlot();
			if (slot == -1) {
				return;
			}
		}
		if (mainInventory || hotbar) {
			ItemInteractionHandler.restockMultipleItems(player, filter, mainInventory, hotbar, fillEmpty);
		} else {
			ItemInteractionHandler.restockItem(player, filter, slot, fillEmpty);
		}
	}

	private static void tryDepositItem(InputEvent.Key event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		int mods = event.getModifiers();
		boolean mainInventory = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
		boolean onlyMatching = (mods & GLFW.GLFW_MOD_CONTROL) == 0;
		boolean hotbar = (mods & GLFW.GLFW_MOD_ALT) != 0;

		if (mainInventory || hotbar) {
			tryDepositMultipleItems(player, mainInventory, hotbar, onlyMatching);
			return;
		}

		Screen screen = Minecraft.getInstance().screen;
		if (screen != null) {
			if (screen instanceof AbstractContainerScreen<?> containerScreen) {
				tryDepositItem(player, containerScreen.getSlotUnderMouse(), onlyMatching);
			}
		} else {
			tryDepositItem(player, onlyMatching);
		}
	}

	private static void tryDepositMultipleItems(Player player, boolean mainInventory, boolean hotbar, boolean onlyMatching) {
		ItemInteractionHandler.depositMultipleItems(player, mainInventory, hotbar, onlyMatching);
	}

	private static void tryDepositItem(Player player, boolean onlyMatching) {
		ItemStack item = player.getMainHandItem();
		if (!item.isEmpty()) {
			ItemInteractionHandler.depositItem(player, player.getInventory().selected, onlyMatching);
		}
	}

	private static boolean tryDepositItem(Player player, @Nullable Slot slot, boolean onlyMatching) {
		if (slot == null || slot.getItem().isEmpty() || !(slot.container instanceof Inventory)) {
			return false;
		}
		ItemInteractionHandler.depositItem(player, slot.getSlotIndex(), onlyMatching);
		return true;
	}

	public static void handleGuiKeyPress(ScreenEvent.KeyPressed.Pre event) {
		InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
		if (ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(key) && event.getScreen() instanceof AbstractContainerScreen screen && tryHighlightItem(screen.getSlotUnderMouse())) {
			screen.onClose();
			event.setCanceled(true);
		} else if (SORT_KEYBIND.isActiveAndMatches(key) && tryCallSort(event.getScreen())) {
			event.setCanceled(true);
		}
	}

	public static void handleGuiMouseKeyPress(ScreenEvent.MouseButtonPressed.Pre event) {
		InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
		if (ITEM_HIGHLIGHT_KEYBIND.isActiveAndMatches(input) && event.getScreen() instanceof AbstractContainerScreen screen && tryHighlightItem(screen.getSlotUnderMouse())) {
			screen.onClose();
			event.setCanceled(true);
		} else if (SORT_KEYBIND.isActiveAndMatches(input) && tryCallSort(event.getScreen())) {
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
					renderSpecialTooltip(event, mc, containerGui, event.getGuiGraphics(), stashResultAndTooltip.get());
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

	private static void renderSpecialTooltip(ScreenEvent.Render.Post event, Minecraft mc, AbstractContainerScreen<?> containerGui, GuiGraphics guiGraphics, StashResultAndTooltip stashResultAndTooltip) {
		int x = event.getMouseX();
		int y = event.getMouseY();
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0, 0, 100);
		guiGraphics.renderTooltip(containerGui.font, Collections.singletonList(Component.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".right_click_to_add_to_storage")), stashResultAndTooltip.tooltip(), x, y);
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
		IStashStorageItem.StashResult stashResult = stashStorageItem.getItemStashable(potentialStashStorage, potentiallyStashable);
		if (stashResult == IStashStorageItem.StashResult.NO_SPACE) {
			return Optional.empty();
		}
		return Optional.of(new StashResultAndTooltip(stashResult, stashStorageItem.getInventoryTooltip(potentialStashStorage)));
	}

	private record StashResultAndTooltip(IStashStorageItem.StashResult stashResult, Optional<TooltipComponent> tooltip) {}

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
