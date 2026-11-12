package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeInventoryControlBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.util.FluidTooltipHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("PMD.UnnecessaryImport")
public class TankInventoryControl extends UpgradeInventoryControlBase {
	private static final TextureBlitData OVERLAY = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(47, 30), new Dimension(16, 18));
	private final int upgradeSlot;
	private final TankUpgradeContainer container;
	private final Position pos;
	private final int height;
	private final StorageScreenBase<?> screen;

	public TankInventoryControl(int upgradeSlot, TankUpgradeContainer container, Position pos, int height, StorageScreenBase<?> screen) {
		this.upgradeSlot = upgradeSlot;
		this.container = container;
		this.pos = pos;
		this.height = height;
		this.screen = screen;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		renderTank(guiGraphics, getTankLeft(), pos.y(), height, container.getContents(), container.getTankCapacity());
	}

	public static void renderTank(GuiGraphics guiGraphics, int x, int y, int height, FluidStack contents, int capacity) {
		GuiHelper.blit(guiGraphics, x, y, GuiHelper.BAR_BACKGROUND_TOP, 18, height < 36 ? height / 2 : 18);
		int yOffset = 18;
		for (int remaining = height - 36; remaining > 0; remaining -= 18) {
			int sectionHeight = Math.min(18, remaining);
			GuiHelper.blit(guiGraphics, x, y + yOffset, GuiHelper.BAR_BACKGROUND_MIDDLE, 18, sectionHeight);
			yOffset += sectionHeight;
		}
		GuiHelper.blit(guiGraphics, x, y + (height < 36 ? height / 2 : yOffset), GuiHelper.BAR_BACKGROUND_BOTTOM, 18, height < 36 ? height / 2 : 18);

		renderFluid(guiGraphics, x + 1, y + 1, height - 2, contents, capacity);

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(0, 0, 100);
		for (yOffset = 0; yOffset < height; yOffset += 18) {
			guiGraphics.blit(OVERLAY.getTextureName(), x + 1, y + yOffset, OVERLAY.getU(), OVERLAY.getV(), OVERLAY.getWidth(), Math.min(18, height - yOffset),
					OVERLAY.getTextureWidth(), OVERLAY.getTextureHeight());
		}
		guiGraphics.pose().popPose();
	}

	private int getTankLeft() {
		return pos.x() + 9;
	}

	@Override
	public boolean handleMouseReleased(double mouseX, double mouseY, int button) {
		if (mouseX < screen.getGuiLeft() + getTankLeft() || mouseX >= screen.getGuiLeft() + getTankLeft() + 18 || mouseY < screen.getGuiTop() + pos.y()
				|| mouseY >= screen.getGuiTop() + pos.y() + height) {
			return false;
		}

		ItemStack cursorStack = screen.getMenu().getCarried();
		if (cursorStack.getCount() > 1) {
			return false;
		}

		PacketHandler.INSTANCE.sendToServer(new TankClickMessage(upgradeSlot));

		return true;
	}

	@Override
	public void renderErrorOverlay(GuiGraphics guiGraphics) {
		screen.renderOverlay(guiGraphics, StorageScreenBase.ERROR_SLOT_COLOR, getTankLeft() + 1, pos.y() + 1, 16, height - 2);
	}

	@Override
	public void renderTooltip(StorageScreenBase<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		FluidStack contents = container.getContents();
		int capacity = container.getTankCapacity();
		if (contents.isEmpty()) {
			contents = FluidStack.EMPTY;
		}

		int screenX = screen.getGuiLeft() + pos.x() + 10;
		int screenY = screen.getGuiTop() + pos.y() + 1;
		if (mouseX >= screenX && mouseX < screenX + 16 && mouseY >= screenY && mouseY < screenY + height - 2) {
			List<Component> tooltip = new ArrayList<>();
			if (!contents.isEmpty()) {
				tooltip.add(contents.getDisplayName());
			}
			tooltip.add(FluidTooltipHelper.getContentsTooltip(contents, capacity));
			guiGraphics.renderTooltip(screen.font, tooltip, Optional.empty(), mouseX, mouseY);
		}
	}

	private static void renderFluid(GuiGraphics guiGraphics, int x, int y, int height, FluidStack contents, int capacity) {
		if (contents.isEmpty()) {
			return;
		}

		Fluid fluid = contents.getFluid();
		int fill = contents.getAmount();
		int displayLevel = (int) (height * ((float) fill / capacity));
		IClientFluidTypeExtensions renderProperties = IClientFluidTypeExtensions.of(fluid);
		ResourceLocation texture = renderProperties.getStillTexture(contents);
		TextureAtlasSprite still = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
		int fluidY = fluid.getFluidType().isLighterThanAir() ? y : y + height - displayLevel;
		GuiHelper.renderTiledFluidTextureAtlas(guiGraphics, still, renderProperties.getTintColor(contents), x, fluidY, displayLevel);
	}
}
