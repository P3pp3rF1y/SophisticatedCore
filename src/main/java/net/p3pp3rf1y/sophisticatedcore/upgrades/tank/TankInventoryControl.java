package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeInventoryControlBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import net.p3pp3rf1y.sophisticatedcore.util.FluidTooltipHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
	public void extract(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		renderTank(guiGraphics, getTankLeft(), pos.y(), height, container.getContents(), container.getTankCapacity());
	}

	public static void renderTank(GuiGraphicsExtractor guiGraphics, int x, int y, int height, FluidStack contents, int capacity) {
		GuiHelper.blit(guiGraphics, x, y, GuiHelper.BAR_BACKGROUND_TOP, 18, height < 36 ? height / 2 : 18);
		int yOffset = 18;
		for (int remaining = height - 36; remaining > 0; remaining -= 18) {
			int sectionHeight = Math.min(18, remaining);
			GuiHelper.blit(guiGraphics, x, y + yOffset, GuiHelper.BAR_BACKGROUND_MIDDLE, 18, sectionHeight);
			yOffset += sectionHeight;
		}
		GuiHelper.blit(guiGraphics, x, y + (height < 36 ? height / 2 : yOffset), GuiHelper.BAR_BACKGROUND_BOTTOM, 18, height < 36 ? height / 2 : 18);

		extractFluid(guiGraphics, x + 1, y + 1, height - 2, contents, capacity);

		for (yOffset = 0; yOffset < height; yOffset += 18) {
			GuiHelper.blit(guiGraphics, x + 1, y + yOffset, OVERLAY, 16, Math.min(18, height - yOffset));
		}
	}

	private int getTankLeft() {
		return pos.x() + 9;
	}

	@Override
	public boolean handleMouseReleased(MouseButtonEvent event) {
		if (event.x() < screen.getGuiLeft() + getTankLeft() || event.x() >= screen.getGuiLeft() + getTankLeft() + 18 || event.y() < screen.getGuiTop() + pos.y()
				|| event.y() >= screen.getGuiTop() + pos.y() + height) {
			return false;
		}

		ItemStack cursorStack = screen.getMenu().getCarried();
		if (cursorStack.getCount() > 1) {
			return false;
		}

		ClientPacketDistributor.sendToServer(new TankClickPayload(upgradeSlot));

		return true;
	}

	@Override
	public void extractErrorOverlay(GuiGraphicsExtractor guiGraphics) {
		screen.extractOverlay(guiGraphics, StorageScreenBase.ERROR_SLOT_COLOR, getTankLeft() + 1, pos.y() + 1, 16, height - 2);
	}

	@Override
	public void extractTooltip(StorageScreenBase<?> screen, GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
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
				tooltip.add(contents.getHoverName());
			}
			tooltip.add(FluidTooltipHelper.getContentsTooltip(contents, capacity));
			guiGraphics.setTooltipForNextFrame(screen.getFont(), tooltip, Optional.empty(), mouseX, mouseY);
		}
	}

	private static void extractFluid(GuiGraphicsExtractor guiGraphics, int x, int y, int height, FluidStack contents, int capacity) {
		if (contents.isEmpty()) {
			return;
		}

		Fluid fluid = contents.getFluid();
		int fill = contents.getAmount();
		int displayLevel = (int) (height * ((float) fill / capacity));
		FluidState fluidState = fluid.defaultFluidState();
		FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
		TextureAtlasSprite still = fluidModel.stillMaterial().sprite();
		int color = fluidModel.tintSource() instanceof FluidTintSource fluidTintSource
				? fluidTintSource.colorAsStack(contents)
				: fluidModel.tintSource() != null ? fluidModel.tintSource().color(fluidState.createLegacyBlock()) : -1;
		int fluidY = fluid.getFluidType().isLighterThanAir() ? y : y + height - displayLevel;
		GuiHelper.renderTiledSprite(guiGraphics, still, color, x, fluidY, displayLevel);
	}
}
