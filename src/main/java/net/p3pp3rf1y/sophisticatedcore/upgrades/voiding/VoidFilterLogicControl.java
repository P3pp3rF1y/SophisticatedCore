package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicControl;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.FluidFilterContainer;

import java.util.List;

import static net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicControlBase.MatchButton.*;

public class VoidFilterLogicControl extends FilterLogicControl<FilterLogic, FilterLogicContainer<FilterLogic>> {
	private final FluidFilterContainer fluidFilterContainer;

	private VoidFilterLogicControl(StorageScreenBase<?> screen, Position position, FilterLogicContainer<FilterLogic> filterLogicContainer,
			FluidFilterContainer fluidFilterContainer, int slotsPerRow, MatchButton... showMatchButtons) {
		super(screen, position, filterLogicContainer, slotsPerRow, showMatchButtons);
		this.fluidFilterContainer = fluidFilterContainer;
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
		if (container.getPrimaryMatch() == PrimaryMatch.TAGS) {
			return;
		}

		for (int slot = 0; slot < fluidFilterContainer.getNumberOfFluidFilters(); slot++) {
			FluidStack fluid = fluidFilterContainer.getFluid(slot);
			if (!fluid.isEmpty()) {
				IClientFluidTypeExtensions renderProperties = IClientFluidTypeExtensions.of(fluid.getFluid());
				ResourceLocation texture = renderProperties.getStillTexture(fluid);
				TextureAtlasSprite still = minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
				int slotX = x + slot % slotsPerRow * 18 + 1;
				int slotY = y + slotsTopYOffset + slot / slotsPerRow * 18 + 1;
				GuiHelper.renderTiledFluidTextureAtlas(guiGraphics, RenderType::guiTextured, still, renderProperties.getTintColor(fluid), slotX, slotY, 16);
			}
		}
	}

	@Override
	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
		int slot = getFilterSlot(mouseX, mouseY);
		if (slot >= 0) {
			FluidStack fluid = fluidFilterContainer.getFluid(slot);
			if (!fluid.isEmpty()) {
				GuiHelper.renderTooltip(screen, guiGraphics, List.of(fluid.getHoverName()), mouseX, mouseY);
			}
		}
	}

	private int getFilterSlot(double mouseX, double mouseY) {
		if (container.getPrimaryMatch() == PrimaryMatch.TAGS || mouseX < x || mouseY < y + slotsTopYOffset) {
			return -1;
		}
		int column = (int) ((mouseX - x) / 18);
		int row = (int) ((mouseY - y - slotsTopYOffset) / 18);
		int slot = row * slotsPerRow + column;
		return column < 0 || column >= slotsPerRow || slot < 0 || slot >= fluidFilterContainer.getNumberOfFluidFilters() ? -1 : slot;
	}

	public static class Basic extends VoidFilterLogicControl {
		public Basic(StorageScreenBase<?> screen, Position position, FilterLogicContainer<FilterLogic> filterLogicContainer,
				FluidFilterContainer fluidFilterContainer, int slotsPerRow) {
			super(screen, position, filterLogicContainer, fluidFilterContainer, slotsPerRow, ALLOW_LIST);
		}
	}

	public static class Advanced extends VoidFilterLogicControl {
		public Advanced(StorageScreenBase<?> screen, Position position, FilterLogicContainer<FilterLogic> filterLogicContainer,
				FluidFilterContainer fluidFilterContainer, int slotsPerRow) {
			super(screen, position, filterLogicContainer, fluidFilterContainer, slotsPerRow, ALLOW_LIST, PRIMARY_MATCH, DURABILITY, NBT);
		}
	}
}
