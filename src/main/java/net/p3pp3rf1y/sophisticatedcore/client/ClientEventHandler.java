package net.p3pp3rf1y.sophisticatedcore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStashStorageItem;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.init.ModParticles;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StorageSoundHandler;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import java.util.Collections;
import java.util.Optional;

public class ClientEventHandler {
	private ClientEventHandler() {}

	public static void registerHandlers() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(ModParticles::registerFactories);
		IEventBus eventBus = MinecraftForge.EVENT_BUS;
		eventBus.addListener(ClientEventHandler::onPlayerJoinServer);
		eventBus.addListener(StorageSoundHandler::tick);
		eventBus.addListener(StorageSoundHandler::onWorldUnload);
		eventBus.addListener(ClientEventHandler::onDrawScreen);
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

			for (Slot s : menu.slots) {
				ItemStack stack = s.getItem();
				if (!s.mayPickup(mc.player) || stack.isEmpty()) {
					continue;
				}
				Optional<TooltipComponent> tooltip = getStashableAndTooltip(stack, held);
				if (tooltip.isEmpty()) {
					continue;
				}

				if (s == under) {
					renderSpecialTooltip(event, mc, containerGui, event.getGuiGraphics(), tooltip);
				} else {
					renderStashSign(mc, containerGui, event.getGuiGraphics(), s, stack);
				}
			}
		}
	}

	private static void renderStashSign(Minecraft mc, AbstractContainerScreen<?> containerGui, GuiGraphics guiGraphics, Slot s, ItemStack stack) {
		int x = containerGui.getGuiLeft() + s.x;
		int y = containerGui.getGuiTop() + s.y;

		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0, 0, 300);

		if (stack.getItem() instanceof IStashStorageItem) {
			guiGraphics.drawString(mc.font, "+", x + 10, y + 8, 0xFFFF00);
		} else {
			guiGraphics.drawString(mc.font, "-", x + 1, y, 0xFFFF00);
		}
		poseStack.popPose();
	}

	private static void renderSpecialTooltip(ScreenEvent.Render.Post event, Minecraft mc, AbstractContainerScreen<?> containerGui, GuiGraphics guiGraphics, Optional<TooltipComponent> tooltip) {
		int x = event.getMouseX();
		int y = event.getMouseY();
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0, 0, 100);
		guiGraphics.renderTooltip(containerGui.font, Collections.singletonList(Component.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".right_click_to_add_to_storage")), tooltip, x, y);
		poseStack.popPose();
	}

	private static Optional<TooltipComponent> getStashableAndTooltip(ItemStack inInventory, ItemStack held) {
		if (inInventory.getCount() == 1 && inInventory.getItem() instanceof IStashStorageItem stashStorageItem && stashStorageItem.isItemStashable(inInventory, held)) {
			return stashStorageItem.getInventoryTooltip(inInventory);
		}

		if (held.getItem() instanceof IStashStorageItem stashStorageItem && stashStorageItem.isItemStashable(held, inInventory)) {
			return stashStorageItem.getInventoryTooltip(held);
		}
		return Optional.empty();
	}

	private static void onPlayerJoinServer(ClientPlayerNetworkEvent.LoggingIn evt) {
		//noinspection ConstantConditions - by the time player is joining the world is not null
		RecipeHelper.setWorld(Minecraft.getInstance().level);
	}
}
