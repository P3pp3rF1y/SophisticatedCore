package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FluidFilterControl extends WidgetBase {
	private final FluidFilterContainer container;
	private final List<Position> slotTopLeftPositions = new ArrayList<>();

	protected FluidFilterControl(Position position, FluidFilterContainer container) {
		super(position, new Dimension(container.getNumberOfFluidFilters() * 18, 18));
		this.container = container;
		for (int i = 0; i < container.getNumberOfFluidFilters(); i++) {
			slotTopLeftPositions.add(new Position(x + i * 18 + 1, y + 1));
		}
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		GuiHelper.renderSlotsBackground(guiGraphics, x, y, container.getNumberOfFluidFilters(), 1);
	}

	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		for (int i = 0; i < container.getNumberOfFluidFilters(); i++) {
			FluidStack fluid = container.getFluid(i);
			if (!fluid.isEmpty()) {
				IClientFluidTypeExtensions renderProperties = IClientFluidTypeExtensions.of(fluid.getFluid());
				ResourceLocation texture = renderProperties.getStillTexture(fluid);
				TextureAtlasSprite still = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
				GuiHelper.renderTiledFluidTextureAtlas(guiGraphics, still, renderProperties.getTintColor(fluid), x + i * 18 + 1, y + 1, 16);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isMouseOver(mouseX, mouseY)) {
			return false;
		}

		getSlotClicked(mouseX, mouseY).ifPresent(container::slotClick);

		return true;
	}

	@Override
	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		getSlotClicked(mouseX, mouseY).ifPresent(slot -> {
			FluidStack fluid = container.getFluid(slot);
			if (!fluid.isEmpty()) {
				GuiHelper.renderTooltip(screen, guiGraphics, List.of(fluid.getHoverName()), mouseX, mouseY);
			}
		});
	}

	public List<Position> getSlotTopLeftPositions() {
		return slotTopLeftPositions;
	}

	private Optional<Integer> getSlotClicked(double mouseX, double mouseY) {
		if (mouseY < y + 1 || mouseY >= y + 17) {
			return Optional.empty();
		}
		int index = (int) ((mouseX - x) / 18);
		if (index < 0 || index >= container.getNumberOfFluidFilters()) {
			return Optional.empty();
		}
		return Optional.of(index);
	}

	@Override
	public void updateNarration(NarrationElementOutput narrationElementOutput) {
		//TODO narration
	}

	public void setFluid(int index, FluidStack fluid) {
		container.setFluid(index, fluid);
	}
}
