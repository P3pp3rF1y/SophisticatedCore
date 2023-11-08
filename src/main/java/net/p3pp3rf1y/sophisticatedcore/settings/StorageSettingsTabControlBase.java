package net.p3pp3rf1y.sophisticatedcore.settings;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsTabControl;
import net.p3pp3rf1y.sophisticatedcore.client.gui.Tab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

import java.util.ArrayList;
import java.util.List;

public abstract class StorageSettingsTabControlBase extends SettingsTabControl<SettingsScreen, SettingsTab<?>> {
	private final List<SettingsTab<?>> settingsTabs = new ArrayList<>();
	protected final SettingsScreen screen;

	protected static <C extends SettingsContainerBase<?>, T extends SettingsTab<C>> void addFactory(
			ImmutableMap.Builder<String, ISettingsTabFactory<?, ?>> builder, String categoryName, ISettingsTabFactory<C, T> factory) {
		builder.put(categoryName, factory);
	}

	protected StorageSettingsTabControlBase(SettingsScreen screen, Position position) {
		super(position);
		this.screen = screen;
		addChild(instantiateReturnBackTab());
		screen.getMenu().forEachSettingsContainer((categoryName, settingsContainer) -> {
			if (isSettingsCategoryDisabled(categoryName)) {
				return;
			}
			settingsTabs.add(addSettingsTab(() -> {}, () -> {},
					instantiateContainer(categoryName, settingsContainer, new Position(x, getTopY()), screen)));
		});
	}

	@SuppressWarnings("unused") //categoryName used in the overrides
	protected boolean isSettingsCategoryDisabled(String categoryName) {
		return false;
	}

	protected abstract Tab instantiateReturnBackTab();

	public void renderSlotOverlays(PoseStack matrixStack, Slot slot, ISlotOverlayRenderer overlayRenderer, boolean templateLoadHovered) {
		List<Integer> colors = new ArrayList<>();
		settingsTabs.forEach(tab -> tab.getSlotOverlayColor(slot.index, templateLoadHovered).ifPresent(colors::add));
		if (colors.isEmpty()) {
			return;
		}

		int stripeHeight = 16 / colors.size();
		int i = 0;
		for (int color : colors) {
			int yOffset = i * stripeHeight;
			overlayRenderer.renderSlotOverlay(matrixStack, slot.x, slot.y + yOffset, i == colors.size() - 1 ? 16 - yOffset : stripeHeight, color);
			i++;
		}
	}

	public ItemStack getSlotStackDisplayOverride(int slotNumber, boolean isTemplateLoadHovered) {
		for (SettingsTab<?> settingsTab : settingsTabs) {
			ItemStack stack = settingsTab.getItemDisplayOverride(slotNumber, isTemplateLoadHovered);
			if (!stack.isEmpty()) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	public void renderSlotExtra(PoseStack poseStack, Slot slot) {
		settingsTabs.forEach(tab -> tab.renderExtra(poseStack, slot));
	}

	public void handleSlotClick(Slot slot, int mouseButton) {
		getOpenTab().ifPresent(tab -> tab.handleSlotClick(slot, mouseButton));
	}

	public boolean renderGuiItem(ItemRenderer itemRenderer, ItemStack itemstack, Slot slot, boolean templateLoadHovered) {
		for (SettingsTab<?> tab : settingsTabs) {
			int rotation = tab.getItemRotation(slot.index, templateLoadHovered);
			if (rotation != 0) {
				GuiHelper.tryRenderGuiItem(itemRenderer, minecraft.getTextureManager(), minecraft.player, itemstack, slot.x, slot.y, rotation);
				return true;
			}
		}
		if (!itemstack.isEmpty()) {
			itemRenderer.renderAndDecorateItem(itemstack, slot.x, slot.y);
			return true;
		}
		return false;
	}

	public void drawSlotStackOverlay(PoseStack poseStack, Slot slot, boolean templateLoadHovered) {
		for (SettingsTab<?> tab : settingsTabs) {
			tab.drawSlotStackOverlay(poseStack, slot, templateLoadHovered);
		}
	}

	public interface ISlotOverlayRenderer {
		void renderSlotOverlay(PoseStack matrixStack, int xPos, int yPos, int height, int slotColor);
	}

	public interface ISettingsTabFactory<C extends SettingsContainerBase<?>, T extends SettingsTab<C>> {
		T create(C container, Position position, SettingsScreen screen);
	}

	private <C extends SettingsContainerBase<?>> SettingsTab<C> instantiateContainer(String categoryName, C container, Position position, SettingsScreen screen) {
		//noinspection unchecked
		return (SettingsTab<C>) getSettingsTabFactory(categoryName).create(container, position, screen);
	}

	protected abstract <C extends SettingsContainerBase<?>, T extends SettingsTab<C>> ISettingsTabFactory<C, T> getSettingsTabFactory(String name);
}
