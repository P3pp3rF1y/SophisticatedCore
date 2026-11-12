package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;

public class FluidTooltipHelper {
	private static final String STORAGE_TOOLTIP_PREFIX = TranslationHelper.INSTANCE.translItemTooltip("storage");

	private FluidTooltipHelper() {
	}

	@SuppressWarnings("deprecation")
	public static boolean isExperience(FluidStack fluid) {
		return !fluid.isEmpty() && fluid.getFluid().is(ModFluids.EXPERIENCE_TAG);
	}

	public static String formatAmount(FluidStack fluid, int amount) {
		return isExperience(fluid)
				? String.format("%.1f", XpHelper.getLevelsForExperience((int) XpHelper.liquidToExperience(amount)))
				: String.format("%,d", amount);
	}

	public static MutableComponent getContentsTooltip(FluidStack fluid, int capacity) {
		return TranslationHelper.INSTANCE.translUpgrade("tank." + (isExperience(fluid) ? "xp_contents_tooltip" : "contents_tooltip"),
				formatAmount(fluid, fluid.getAmount()), formatAmount(fluid, capacity));
	}

	public static MutableComponent getFluidTooltip(FluidStack fluid) {
		return getFluidTooltip(fluid, STORAGE_TOOLTIP_PREFIX + ".fluid");
	}

	public static MutableComponent getFluidTooltip(FluidStack fluid, String fluidTranslation) {
		return Component.translatable(isExperience(fluid) ? STORAGE_TOOLTIP_PREFIX + ".fluid_xp" : fluidTranslation,
				Component.literal(isExperience(fluid) ? formatAmount(fluid, fluid.getAmount()) : CountAbbreviator.abbreviate(fluid.getAmount()))
						.withStyle(ChatFormatting.GRAY),
				Component.translatable(fluid.getFluidType().getDescriptionId(fluid)).withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GRAY);
	}

	public static MutableComponent getFluidTooltip(FluidStack fluid, int capacity) {
		return Component.translatable(STORAGE_TOOLTIP_PREFIX + (isExperience(fluid) ? ".fluid_xp_with_capacity" : ".fluid_with_capacity"),
				formatAmount(fluid, fluid.getAmount()), formatAmount(fluid, capacity),
				Component.translatable(fluid.getFluidType().getDescriptionId(fluid)).withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GRAY);
	}
}
