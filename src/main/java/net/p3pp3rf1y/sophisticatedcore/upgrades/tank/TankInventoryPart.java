package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeInventoryPartBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.util.XpHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TankInventoryPart extends UpgradeInventoryPartBase<TankUpgradeContainer> {
	private static final TextureBlitData OVERLAY = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(47, 30), new Dimension(16, 18));
	private final Position pos;
	private final int height;
	private final StorageScreenBase<?> screen;

	public TankInventoryPart(int upgradeSlot, TankUpgradeContainer container, Position pos, int height, StorageScreenBase<?> screen) {
		super(upgradeSlot, container);
		this.pos = pos;
		this.height = height;
		this.screen = screen;
	}

	@Override
	public void extract(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		GuiHelper.blit(guiGraphics, getTankLeft(), pos.y(), GuiHelper.BAR_BACKGROUND_TOP, 18, height < 36 ? height / 2 : 18);
		int yOffset = 18;
		for (int i = 0; i < (height - 36) / 18; i++) {
			GuiHelper.blit(guiGraphics, getTankLeft(), pos.y() + yOffset, GuiHelper.BAR_BACKGROUND_MIDDLE);
			yOffset += 18;
		}
		GuiHelper.blit(guiGraphics, getTankLeft(), pos.y() + (height < 36 ? height / 2 : yOffset), GuiHelper.BAR_BACKGROUND_BOTTOM, 18, height < 36 ? height / 2 : 18);

		extractFluid(guiGraphics);

		yOffset = 0;
		for (int i = 0; i < height / 18; i++) {
			GuiHelper.blit(guiGraphics, getTankLeft() + 1, pos.y() + yOffset, OVERLAY);
			yOffset += 18;
		}
	}

	private int getTankLeft() {
		return pos.x() + 9;
	}

	@Override
	public boolean handleMouseReleased(MouseButtonEvent event) {
		if (event.x() < screen.getGuiLeft() + getTankLeft() || event.x() >= screen.getGuiLeft() + getTankLeft() + 18 ||
				event.y() < screen.getGuiTop() + pos.y() || event.y() >= screen.getGuiTop() + pos.y() + height) {
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
			tooltip.add(getContentsTooltip(contents, capacity));
			guiGraphics.setTooltipForNextFrame(screen.getFont(), tooltip, Optional.empty(), mouseX, mouseY);
		}
	}

	private MutableComponent getContentsTooltip(FluidStack contents, int capacity) {
		//noinspection deprecation
		if (contents.getFluid().is(ModFluids.EXPERIENCE_TAG)) {
			double contentsLevels = XpHelper.getLevelsForExperience((int) XpHelper.liquidToExperience(contents.getAmount()));
			double tankCapacityLevels = XpHelper.getLevelsForExperience((int) XpHelper.liquidToExperience(capacity));

			return Component.translatable(TranslationHelper.INSTANCE.translUpgradeKey("tank.xp_contents_tooltip"), String.format("%.1f", contentsLevels), String.format("%.1f", tankCapacityLevels));
		}
		return Component.translatable(TranslationHelper.INSTANCE.translUpgradeKey("tank.contents_tooltip"), String.format("%,d", contents.getAmount()), String.format("%,d", capacity));
	}

	private void extractFluid(GuiGraphicsExtractor guiGraphics) {
		FluidStack contents = container.getContents();
		int capacity = container.getTankCapacity();
		if (contents.isEmpty()) {
			return;
		}

		Fluid fluid = contents.getFluid();
		int fill = contents.getAmount();
		int displayLevel = (int) ((height - 2) * ((float) fill / capacity));
		FluidState fluidState = fluid.defaultFluidState();
		FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
		TextureAtlasSprite still = fluidModel.stillMaterial().sprite();
		int color = fluidModel.tintSource() instanceof FluidTintSource fluidTintSource ? fluidTintSource.colorAsStack(contents) : fluidModel.tintSource() != null ? fluidModel.tintSource().color(fluidState.createLegacyBlock()) : -1;
		GuiHelper.renderTiledSprite(guiGraphics, still, color, pos.x() + 10, pos.y() + 1 + height - 2 - displayLevel, displayLevel);
	}

}
