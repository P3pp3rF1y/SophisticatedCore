package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeInventoryControlBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.util.XpHelper;

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
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		GuiHelper.blit(guiGraphics, getTankLeft(), pos.y(), GuiHelper.BAR_BACKGROUND_TOP, 18, height < 36 ? height / 2 : 18);
		int yOffset = 18;
		for (int i = 0; i < (height - 36) / 18; i++) {
			GuiHelper.blit(guiGraphics, getTankLeft(), pos.y() + yOffset, GuiHelper.BAR_BACKGROUND_MIDDLE);
			yOffset += 18;
		}
		GuiHelper.blit(guiGraphics, getTankLeft(), pos.y() + (height < 36 ? height / 2 : yOffset), GuiHelper.BAR_BACKGROUND_BOTTOM, 18,
				height < 36 ? height / 2 : 18);

		renderFluid(guiGraphics);

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
	public boolean handleMouseReleased(double mouseX, double mouseY, int button) {
		if (mouseX < screen.getGuiLeft() + getTankLeft() || mouseX >= screen.getGuiLeft() + getTankLeft() + 18 || mouseY < screen.getGuiTop() + pos.y()
				|| mouseY >= screen.getGuiTop() + pos.y() + height) {
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
				tooltip.add(contents.getHoverName());
			}
			tooltip.add(getContentsTooltip(contents, capacity));
			guiGraphics.setTooltipForNextFrame(screen.getFont(), tooltip, Optional.empty(), mouseX, mouseY);
		}
	}

	private MutableComponent getContentsTooltip(FluidStack contents, int capacity) {
		// noinspection deprecation
		if (contents.getFluid().is(ModFluids.EXPERIENCE_TAG)) {
			double contentsLevels = XpHelper.getLevelsForExperience((int) XpHelper.liquidToExperience(contents.getAmount()));
			double tankCapacityLevels = XpHelper.getLevelsForExperience((int) XpHelper.liquidToExperience(capacity));

			return Component.translatable(TranslationHelper.INSTANCE.translUpgradeKey("tank.xp_contents_tooltip"), String.format("%.1f", contentsLevels),
					String.format("%.1f", tankCapacityLevels));
		}
		return Component.translatable(TranslationHelper.INSTANCE.translUpgradeKey("tank.contents_tooltip"), String.format("%,d", contents.getAmount()),
				String.format("%,d", capacity));
	}

	private void renderFluid(GuiGraphics guiGraphics) {
		FluidStack contents = container.getContents();
		int capacity = container.getTankCapacity();
		if (contents.isEmpty()) {
			return;
		}

		Fluid fluid = contents.getFluid();
		int fill = contents.getAmount();
		int displayLevel = (int) ((height - 2) * ((float) fill / capacity));
		IClientFluidTypeExtensions renderProperties = IClientFluidTypeExtensions.of(fluid);
		ResourceLocation texture = renderProperties.getStillTexture(contents);
		TextureAtlasSprite still = FluidSpriteCache.getSprite(texture);
		int fluidY = fluid.getFluidType().isLighterThanAir() ? pos.y() + 1 : pos.y() + 1 + height - 2 - displayLevel;
		GuiHelper.renderTiledSprite(guiGraphics, still, renderProperties.getTintColor(contents), pos.x() + 10, fluidY, displayLevel);
	}
}
