package net.p3pp3rf1y.sophisticatedcore.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public abstract class ClientStorageContentsTooltipBase implements ClientTooltipComponent {
	private static final int REFRESH_INTERVAL = 20;
	private static final String STORAGE_ITEM = "storage";
	protected static long lastRequestTime = 0;
	@Nullable
	private static UUID storageUuid = null;
	private static List<IUpgradeWrapper> upgrades = new ArrayList<>();
	private static List<ItemStack> sortedContents = new ArrayList<>();
	private static final List<Component> tooltipLines = new ArrayList<>();
	private static int height = 0;
	private static int width = 0;
	private static boolean shouldRefreshContents = true;

	public static void refreshContents() {
		shouldRefreshContents = true;
	}

	private void initContents(LocalPlayer player, IStorageWrapper wrapper) {
		UUID newUuid = wrapper.getContentsUuid().orElse(null);
		if (storageUuid == null && newUuid != null || storageUuid != null && !storageUuid.equals(newUuid)) {
			setLastRequestTime(0);
			storageUuid = newUuid;
			setShouldRefreshContents(true);
		}
		if (storageUuid != null) {
			requestContents(player, wrapper);
		}
		refreshContents(wrapper);
	}

	@SuppressWarnings("java:S2696")
	protected void setLastRequestTime(long lastRequestTime) {
		ClientStorageContentsTooltipBase.lastRequestTime = lastRequestTime;
	}

	protected long getLastRequestTime() {
		return ClientStorageContentsTooltipBase.lastRequestTime;
	}

	private void requestContents(LocalPlayer player, IStorageWrapper wrapper) {
		if (getLastRequestTime() + REFRESH_INTERVAL < player.level().getGameTime()) {
			setLastRequestTime(player.level().getGameTime());
			wrapper.getContentsUuid().ifPresent(this::sendInventorySyncRequest);
		}
	}

	protected abstract void sendInventorySyncRequest(UUID uuid);

	private void refreshContents(IStorageWrapper wrapper) {
		if (shouldRefreshContents()) {
			setShouldRefreshContents(false);
			sortedContents.clear();
			upgrades.clear();
			tooltipLines.clear();
			if (storageUuid != null) {
				wrapper.onContentsNbtUpdated();
				sortedContents = InventoryHelper.getCompactedStacksSortedByCount(wrapper.getInventoryHandler());
				upgrades = new ArrayList<>(wrapper.getUpgradeHandler().getSlotWrappers().values());
				addMultiplierTooltip(wrapper);
				addFluidTooltip(wrapper);
				addEnergyTooltip(wrapper);
			}
			if (upgrades.isEmpty() && sortedContents.isEmpty()) {
				tooltipLines.add(Component.translatable(TranslationHelper.INSTANCE.translItemTooltip(STORAGE_ITEM) + ".empty").withStyle(ChatFormatting.YELLOW));
			}

			calculateHeight();
			calculateWidth();
		}
	}

	@SuppressWarnings("java:S2696")
	protected void setShouldRefreshContents(boolean shouldRefreshContents) {
		ClientStorageContentsTooltipBase.shouldRefreshContents = shouldRefreshContents;
	}

	protected boolean shouldRefreshContents() {
		return ClientStorageContentsTooltipBase.shouldRefreshContents;
	}

	private void calculateWidth() {
		int upgradesWidth = calculateUpgradesWidth();
		int contentsWidth = calculateContentsWidth();
		int tooltipContentsWidth = calculateTooltipLinesWidth();
		int stacksWidth = Math.max(upgradesWidth, contentsWidth);
		width = Math.max(stacksWidth, tooltipContentsWidth);
	}

	private int calculateTooltipLinesWidth() {
		return tooltipLines.stream().map(this::getTooltipWidth).max(Comparator.naturalOrder()).orElse(0);
	}

	private int calculateUpgradesWidth() {
		int upgradesWidth = 0;
		for (IUpgradeWrapper upgradeWrapper : upgrades) {
			upgradesWidth += (upgradeWrapper.canBeDisabled() ? 4 : 0) + DEFAULT_STACK_WIDTH;
		}
		return upgradesWidth;
	}

	private int calculateContentsWidth() {
		Font fontRenderer = Minecraft.getInstance().font;
		int contentsWidth = 0;
		for (int i = 0; i < sortedContents.size() && i < MAX_STACKS_ON_LINE; i++) {
			int countWidth = getStackCountWidth(fontRenderer, sortedContents.get(i));
			contentsWidth += Math.max(countWidth, DEFAULT_STACK_WIDTH);
		}

		return contentsWidth;
	}

	private int getStackCountWidth(Font fontRenderer, ItemStack stack) {
		return fontRenderer.width(CountAbbreviator.abbreviate(stack.getCount())) + COUNT_PADDING;
	}

	private int getTooltipWidth(Component component) {
		return Minecraft.getInstance().font.width(component.getVisualOrderText());
	}

	private void addMultiplierTooltip(IStorageWrapper wrapper) {
		double multiplier = wrapper.getInventoryHandler().getStackSizeMultiplier();
		if (multiplier > 1) {
			DecimalFormat df = new DecimalFormat("0.###");

			tooltipLines.add(Component.translatable(TranslationHelper.INSTANCE.translItemTooltip(STORAGE_ITEM) + ".stack_multiplier",
					Component.literal(df.format(multiplier)).withStyle(ChatFormatting.WHITE)
			).withStyle(ChatFormatting.GREEN));
		}
	}

	private void addEnergyTooltip(IStorageWrapper wrapper) {
		wrapper.getEnergyStorage().ifPresent(energyStorage -> tooltipLines.add(Component.translatable(getEnergyTooltipTranslation(),
				Component.literal(CountAbbreviator.abbreviate(energyStorage.getEnergyStored())).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.RED)
		));
	}

	protected String getEnergyTooltipTranslation() {
		return TranslationHelper.INSTANCE.translItemTooltip(STORAGE_ITEM) + ".energy";
	}

	private void addFluidTooltip(IStorageWrapper wrapper) {
		wrapper.getFluidHandler().ifPresent(fluidHandler -> {
			for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
				FluidStack fluid = fluidHandler.getFluidInTank(tank);
				if (fluid.isEmpty()) {
					tooltipLines.add(Component.translatable(getEmptyFluidTooltipTranslation()).withStyle(ChatFormatting.BLUE));
				} else {
					tooltipLines.add(Component.translatable(getFluidTooltipTranslation(),
							Component.literal(CountAbbreviator.abbreviate(fluid.getAmount())).withStyle(ChatFormatting.WHITE),
							Component.translatable(fluid.getTranslationKey()).withStyle(ChatFormatting.BLUE)

					));
				}
			}
		});
	}

	protected String getFluidTooltipTranslation() {
		return TranslationHelper.INSTANCE.translItemTooltip(STORAGE_ITEM) + ".fluid";
	}

	protected String getEmptyFluidTooltipTranslation() {
		return TranslationHelper.INSTANCE.translItemTooltip(STORAGE_ITEM) + ".fluid_empty";
	}

	private void calculateHeight() {
		int upgradesHeight = upgrades.isEmpty() ? 0 : 32;
		int inventoryHeight = sortedContents.isEmpty() ? 0 : 12 + (1 + (sortedContents.size() - 1) / MAX_STACKS_ON_LINE) * 20;
		int totalHeight = upgradesHeight + inventoryHeight + tooltipLines.size() * 10;
		height = totalHeight > 0 ? totalHeight : 12;
	}

	private static final TextureBlitData UPGRADE_ON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(4, 128), Dimension.RECTANGLE_4_10);
	private static final TextureBlitData UPGRADE_OFF = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(0, 128), Dimension.RECTANGLE_4_10);
	private static final int MAX_STACKS_ON_LINE = 9;
	private static final int DEFAULT_STACK_WIDTH = 18;
	private static final int COUNT_PADDING = 2;

	@Override
	public int getWidth(Font font) {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	protected void renderTooltip(IStorageWrapper wrapper, Font font, int leftX, int topY, GuiGraphics guiGraphics) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}

		initContents(player, wrapper);
		renderComponent(font, leftX, topY, guiGraphics, minecraft);
	}

	private void renderComponent(Font font, int leftX, int topY, GuiGraphics guiGraphics, Minecraft minecraft) {
		for (Component tooltipLine : tooltipLines) {
			topY = renderTooltipLine(guiGraphics, leftX, topY, font, tooltipLine);
		}
		renderContentsTooltip(minecraft, font, leftX, topY, guiGraphics);
	}

	private void renderContentsTooltip(Minecraft minecraft, Font font, int leftX, int topY, GuiGraphics guiGraphics) {
		if (!upgrades.isEmpty()) {
			topY = renderTooltipLine(guiGraphics, leftX, topY, font, Component.translatable(TranslationHelper.INSTANCE.translItemTooltip(STORAGE_ITEM) + ".upgrades").withStyle(ChatFormatting.YELLOW));
			topY = renderUpgrades(guiGraphics, leftX, topY);
		}
		if (!sortedContents.isEmpty()) {
			topY = renderTooltipLine(guiGraphics, leftX, topY, font, Component.translatable(TranslationHelper.INSTANCE.translItemTooltip(STORAGE_ITEM) + ".inventory").withStyle(ChatFormatting.YELLOW));
			renderContents(minecraft, leftX, topY, guiGraphics, font);
		}
	}

	private int renderTooltipLine(GuiGraphics guiGraphics, int leftX, int topY, Font font, Component tooltip) {
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0.0D, 0.0D, 200.0F);
		MultiBufferSource.BufferSource renderTypeBuffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		font.drawInBatch(tooltip, leftX, topY, 16777215, true, poseStack.last().pose(), renderTypeBuffer, Font.DisplayMode.NORMAL, 0, 15728880);
		renderTypeBuffer.endBatch();
		poseStack.translate(0.0D, 0.0D, -200.0F);
		poseStack.popPose();
		return topY + 10;
	}

	private int renderUpgrades(GuiGraphics guiGraphics, int leftX, int topY) {
		int x = leftX;
		for (IUpgradeWrapper upgradeWrapper : upgrades) {
			if (upgradeWrapper.canBeDisabled()) {
				RenderSystem.disableDepthTest();
				GuiHelper.blit(guiGraphics, x, topY + 3, upgradeWrapper.isEnabled() ? UPGRADE_ON : UPGRADE_OFF);
				x += 4;
			}
			guiGraphics.renderItem(upgradeWrapper.getUpgradeStack(), x, topY);
			x += DEFAULT_STACK_WIDTH;
		}
		topY += 20;
		return topY;
	}

	private void renderContents(Minecraft minecraft, int leftX, int topY, GuiGraphics guiGraphics, Font font) {
		int x = leftX;
		for (int i = 0; i < sortedContents.size(); i++) {
			int y = topY + i / MAX_STACKS_ON_LINE * 20;
			if (i % MAX_STACKS_ON_LINE == 0) {
				x = leftX;
			}
			ItemStack stack = sortedContents.get(i);
			int stackWidth = Math.max(getStackCountWidth(minecraft.font, stack), DEFAULT_STACK_WIDTH);
			int xOffset = stackWidth - DEFAULT_STACK_WIDTH;
			guiGraphics.renderItem(stack, x + xOffset, y);
			guiGraphics.renderItemDecorations(font, stack, x + xOffset, y, CountAbbreviator.abbreviate(stack.getCount()));
			x += stackWidth;
		}
	}
}
