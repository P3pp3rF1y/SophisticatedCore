package net.p3pp3rf1y.sophisticatedcore.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.function.Supplier;

public class ModFluids {
	private ModFluids() {
	}

	private static BaseFlowingFluid.Properties fluidProperties() {
		return new BaseFlowingFluid.Properties(XP_FLUID_TYPE, XP_STILL, XP_FLOWING).bucket(XP_BUCKET);
	}

	public static final ResourceLocation EXPERIENCE_TAG_NAME = ResourceLocation.fromNamespaceAndPath("c", "experience");

	public static final TagKey<Fluid> EXPERIENCE_TAG = TagKey.create(Registries.FLUID, EXPERIENCE_TAG_NAME);
	public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, SophisticatedCore.MOD_ID);

	public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, SophisticatedCore.MOD_ID);
	public static final Supplier<FlowingFluid> XP_STILL = FLUIDS.register("xp_still", () -> new BaseFlowingFluid.Source(fluidProperties()));

	public static final Supplier<FlowingFluid> XP_FLOWING = FLUIDS.register("xp_flowing", () -> new BaseFlowingFluid.Flowing(fluidProperties()));
	public static final Supplier<FluidType> XP_FLUID_TYPE = FLUID_TYPES.register("experience", () -> new FluidType(FluidType.Properties.create().lightLevel(10).density(800).viscosity(1500)));

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, SophisticatedCore.MOD_ID);
	public static final Supplier<Item> XP_BUCKET = ITEMS.register("xp_bucket", () -> new BucketItem(XP_STILL.get(), new Item.Properties().stacksTo(1)));

	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB.location(), SophisticatedCore.MOD_ID);
	public static final Supplier<CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register("main", () ->
			CreativeModeTab.builder().icon(() -> new ItemStack(XP_BUCKET.get()))
					.title(Component.translatable("itemGroup.sophisticatedbackpacks"))
					.displayItems((featureFlags, output) -> output.accept(new ItemStack(XP_BUCKET.get())))
					.build());

	public static void registerHandlers(IEventBus modBus) {
		FLUIDS.register(modBus);
		FLUID_TYPES.register(modBus);
		ITEMS.register(modBus);
		CREATIVE_MODE_TABS.register(modBus);
	}
}
