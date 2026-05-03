package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.CompositeWidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;

public abstract class Tab extends CompositeWidgetBase<WidgetBase> {
	public static final int DEFAULT_HEIGHT = 24;
	protected static final int DEFAULT_WIDTH = 21;

	private int width = DEFAULT_WIDTH;
	private int height = DEFAULT_HEIGHT;
	private final List<Component> tooltip;

	private BooleanSupplier shouldShowTooltip = () -> true;
	protected BooleanSupplier shouldRender = () -> true;

	protected Tab(Position position, List<Component> tooltip, Function<IntConsumer, ButtonBase> getTabButton) {
		super(position, new Dimension(0, 0));
		this.tooltip = tooltip;
		addChild(getTabButton.apply(this::onTabIconClicked));
	}

	protected Tab(Position position, Component tooltip, Function<IntConsumer, ButtonBase> getTabButton) {
		this(position, List.of(tooltip), getTabButton);
	}

	public void setHandlers(BooleanSupplier shouldShowTooltip, BooleanSupplier shouldRender) {
		this.shouldShowTooltip = shouldShowTooltip;
		this.shouldRender = shouldRender;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		if (!shouldRender.getAsBoolean()) {
			return;
		}
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
		if (shouldRender.getAsBoolean() && isClosedTooltipVisible(mouseX, mouseY)) {
			guiGraphics.setTooltipForNextFrame(screen.getFont(), tooltip, Optional.empty(), mouseX, mouseY);
		}
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public Optional<Rect2i> getTabRectangle() {
		return GuiHelper.getPositiveRectangle(x, y, width, height);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		GuiHelper.renderTabBackground(guiGraphics, x, y, width, height);
	}

	protected boolean isClosedTooltipVisible(int mouseX, int mouseY) {
		return shouldShowTooltip.getAsBoolean() && isMouseOver(mouseX, mouseY);
	}

	public int getTopY() {
		return y;
	}

	public int getBottomY() {
		return y + getHeight();
	}

	protected abstract void onTabIconClicked(int button);

	@Override
	public NarrationPriority narrationPriority() {
		return NarrationPriority.NONE;
	}

	@Override
	public void updateNarration(NarrationElementOutput narrationElementOutput) {
		//noop
	}

	public void tick() {
		//noop
	}
}
