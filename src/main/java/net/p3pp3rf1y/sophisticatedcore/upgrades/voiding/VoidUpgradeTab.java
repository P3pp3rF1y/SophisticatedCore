package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;

import java.util.Map;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.getButtonStateData;

public class VoidUpgradeTab extends UpgradeSettingsTab<VoidUpgradeContainer> {
	private static final MutableComponent VOID_SLOT_OVERFLOW_TOOLTIP = Component
			.translatable(TranslationHelper.INSTANCE.translUpgradeButton("void_slot_overflow"));
	private static final MutableComponent VOID_SLOT_OVERFLOW_TOOLTIP_DETAIL = Component
			.translatable(TranslationHelper.INSTANCE.translUpgradeButton("void_slot_overflow.detail")).withStyle(ChatFormatting.GRAY);

	private static final MutableComponent VOID_STORAGE_OVERFLOW_TOOLTIP = Component
			.translatable(TranslationHelper.INSTANCE.translUpgradeButton("void_storage_overflow"));
	private static final MutableComponent VOID_STORAGE_OVERFLOW_TOOLTIP_DETAIL = Component
			.translatable(TranslationHelper.INSTANCE.translUpgradeButton("void_storage_overflow.detail")).withStyle(ChatFormatting.GRAY);

	private static final MutableComponent VOID_ALWAYS_DISABLED_TOOLTIP = Component
			.translatable(TranslationHelper.INSTANCE.translUpgradeButton("void_always_disabled")).withStyle(ChatFormatting.RED);

	private static final ButtonDefinition.Toggle<VoidType> VOID_TYPE = ButtonDefinitions.createToggleButtonDefinition(Map.of(VoidType.ALWAYS,
			getButtonStateData(new UV(208, 16), TranslationHelper.INSTANCE.translUpgradeButton("void_always"), Dimension.SQUARE_16, new Position(1, 1)),
			VoidType.SLOT_OVERFLOW,
			getButtonStateData(new UV(224, 16), Dimension.SQUARE_16, new Position(1, 1), VOID_SLOT_OVERFLOW_TOOLTIP, VOID_SLOT_OVERFLOW_TOOLTIP_DETAIL),
			VoidType.STORAGE_OVERFLOW,
			getButtonStateData(new UV(112, 96), Dimension.SQUARE_16, new Position(1, 1), VOID_STORAGE_OVERFLOW_TOOLTIP, VOID_STORAGE_OVERFLOW_TOOLTIP_DETAIL)));
	private static final ButtonDefinition.Toggle<VoidType> VOID_TYPE_OVERFLOW_ONLY = ButtonDefinitions
			.createToggleButtonDefinition(Map.of(VoidType.SLOT_OVERFLOW,
					getButtonStateData(new UV(224, 16), Dimension.SQUARE_16, new Position(1, 1), VOID_SLOT_OVERFLOW_TOOLTIP, VOID_SLOT_OVERFLOW_TOOLTIP_DETAIL,
							VOID_ALWAYS_DISABLED_TOOLTIP),
					VoidType.STORAGE_OVERFLOW, getButtonStateData(new UV(112, 96), Dimension.SQUARE_16, new Position(1, 1), VOID_STORAGE_OVERFLOW_TOOLTIP,
							VOID_STORAGE_OVERFLOW_TOOLTIP_DETAIL, VOID_ALWAYS_DISABLED_TOOLTIP)));

	protected VoidFilterLogicControl filterLogicControl;

	protected VoidUpgradeTab(VoidUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, Component tabLabel,
			Component closedTooltip) {
		super(upgradeContainer, position, screen, tabLabel, closedTooltip);
		addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), ButtonDefinitions.WORK_IN_GUI,
				button -> getContainer().setShouldWorkdInGUI(!getContainer().shouldWorkInGUI()), getContainer()::shouldWorkInGUI));
		addHideableChild(
				new ToggleButton<>(new Position(x + 21, y + 24), getContainer().getUpgradeWrapper().isVoidAlwaysEnabled() ? VOID_TYPE : VOID_TYPE_OVERFLOW_ONLY,
						button -> getContainer().setVoidType(getContainer().getVoidType().next()), getContainer()::getVoidType));
	}

	@Override
	protected void moveSlotsToTab() {
		filterLogicControl.moveSlotsToView();
	}

	public void setFluidFilter(int slot, FluidStack fluid) {
		getContainer().getFluidFilterContainer().setFluid(slot, fluid);
	}

	public static class Basic extends VoidUpgradeTab {
		public Basic(VoidUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsPerRow) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("void"),
					TranslationHelper.INSTANCE.translUpgradeTooltip("void"));
			filterLogicControl = addHideableChild(new VoidFilterLogicControl.Basic(screen, new Position(x + 3, y + 44),
					getContainer().getFilterLogicContainer(), getContainer().getFluidFilterContainer(), slotsPerRow));
		}
	}

	public static class Advanced extends VoidUpgradeTab {
		public Advanced(VoidUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, int slotsPerRow) {
			super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("advanced_void"),
					TranslationHelper.INSTANCE.translUpgradeTooltip("advanced_void"));
			filterLogicControl = addHideableChild(new VoidFilterLogicControl.Advanced(screen, new Position(x + 3, y + 44),
					getContainer().getFilterLogicContainer(), getContainer().getFluidFilterContainer(), slotsPerRow));
		}
	}
}
