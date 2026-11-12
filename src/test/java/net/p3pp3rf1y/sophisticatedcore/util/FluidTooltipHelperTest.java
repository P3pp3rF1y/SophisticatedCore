package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FluidTooltipHelperTest {
	private Locale previousLocale;

	@BeforeEach
	void useEnglishFormatting() {
		previousLocale = Locale.getDefault();
		Locale.setDefault(Locale.US);
	}

	@AfterEach
	void restoreLocale() {
		Locale.setDefault(previousLocale);
	}

	@Test
	void getContentsTooltipGroupsFluidAmountAndCapacity() {
		FluidStack fluid = fluid(1_234_567, false);

		Component result = FluidTooltipHelper.getContentsTooltip(fluid, 2_000_000);

		TranslatableContents gauge = contents(result);
		assertEquals("gui.sophisticatedcore.upgrades.tank.contents_tooltip", gauge.getKey());
		assertArrayEquals(new Object[]{"1,234,567", "2,000,000"}, gauge.getArgs());
	}

	@Test
	void getFluidTooltipUsesCustomTranslationWithAbbreviatedAmountAndColors() {
		FluidStack fluid = fluid(1_234_567, false);

		Component result = FluidTooltipHelper.getFluidTooltip(fluid, "custom.fluid");

		TranslatableContents item = contents(result);
		assertEquals("custom.fluid", item.getKey());
		Component amount = assertInstanceOf(Component.class, item.getArgs()[0]);
		assertEquals("1.23m", amount.getString());
		assertEquals(Component.literal("").withStyle(ChatFormatting.GRAY).getStyle().getColor(), amount.getStyle().getColor());
		Component name = assertInstanceOf(Component.class, item.getArgs()[1]);
		assertEquals("fluid.test.contents", contents(name).getKey());
		assertEquals(Component.literal("").withStyle(ChatFormatting.BLUE).getStyle().getColor(), name.getStyle().getColor());
	}

	@Test
	void getFluidTooltipGroupsFluidAmountAndCapacity() {
		FluidStack fluid = fluid(1_234_567, false);

		Component result = FluidTooltipHelper.getFluidTooltip(fluid, 2_000_000);

		TranslatableContents withCapacity = contents(result);
		assertEquals("item.sophisticatedcore.storage.tooltip.fluid_with_capacity", withCapacity.getKey());
		assertEquals("1,234,567", withCapacity.getArgs()[0]);
		assertEquals("2,000,000", withCapacity.getArgs()[1]);
		Component name = assertInstanceOf(Component.class, withCapacity.getArgs()[2]);
		assertEquals("fluid.test.contents", contents(name).getKey());
		assertEquals(Component.literal("").withStyle(ChatFormatting.BLUE).getStyle().getColor(), name.getStyle().getColor());
	}

	@Test
	void getContentsTooltipUsesExperienceLevelsForAmountAndCapacity() {
		FluidStack fluid = fluid(6_300, true);

		Component result = FluidTooltipHelper.getContentsTooltip(fluid, 27_900);

		TranslatableContents gauge = contents(result);
		assertEquals("gui.sophisticatedcore.upgrades.tank.xp_contents_tooltip", gauge.getKey());
		assertArrayEquals(new Object[]{"15.0", "30.0"}, gauge.getArgs());
	}

	@Test
	void getFluidTooltipUsesExperienceLevelsInsteadOfCustomMillibucketTranslation() {
		FluidStack fluid = fluid(6_300, true);

		Component result = FluidTooltipHelper.getFluidTooltip(fluid, "custom.fluid");

		TranslatableContents item = contents(result);
		assertEquals("item.sophisticatedcore.storage.tooltip.fluid_xp", item.getKey());
		assertEquals("15.0", assertInstanceOf(Component.class, item.getArgs()[0]).getString());
	}

	@Test
	void getFluidTooltipUsesExperienceLevelsForAmountAndCapacity() {
		FluidStack fluid = fluid(6_300, true);

		Component result = FluidTooltipHelper.getFluidTooltip(fluid, 27_900);

		TranslatableContents withCapacity = contents(result);
		assertEquals("item.sophisticatedcore.storage.tooltip.fluid_xp_with_capacity", withCapacity.getKey());
		assertEquals("15.0", withCapacity.getArgs()[0]);
		assertEquals("30.0", withCapacity.getArgs()[1]);
	}

	@Test
	void getContentsTooltipFormatsEmptyFluidInMillibuckets() {
		int capacity = 27_900;

		Component result = FluidTooltipHelper.getContentsTooltip(FluidStack.EMPTY, capacity);

		assertArrayEquals(new Object[]{"0", "27,900"}, contents(result).getArgs());
	}

	@SuppressWarnings("deprecation")
	private static FluidStack fluid(int amount, boolean experience) {
		FluidStack stack = mock(FluidStack.class);
		Fluid fluid = mock(Fluid.class);
		FluidType type = mock(FluidType.class);
		when(stack.getAmount()).thenReturn(amount);
		when(stack.getFluid()).thenReturn(fluid);
		when(fluid.is(ModFluids.EXPERIENCE_TAG)).thenReturn(experience);
		when(stack.getFluidType()).thenReturn(type);
		when(type.getDescriptionId(stack)).thenReturn("fluid.test.contents");
		return stack;
	}

	private static TranslatableContents contents(Component component) {
		return assertInstanceOf(TranslatableContents.class, component.getContents());
	}
}
