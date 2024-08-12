package net.p3pp3rf1y.sophisticatedcore.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterAttributes;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.HungerLevel;
import net.p3pp3rf1y.sophisticatedcore.upgrades.filter.Direction;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.AutomationDirection;
import net.p3pp3rf1y.sophisticatedcore.util.SimpleItemContent;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ModCoreDataComponents {
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, SophisticatedCore.MOD_ID);
    public static final Supplier<DataComponentType<Integer>> NUMBER_OF_INVENTORY_SLOTS = DATA_COMPONENT_TYPES.register("number_of_inventory_slots",
            () -> new DataComponentType.Builder<Integer>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());
    public static final Supplier<DataComponentType<Integer>> NUMBER_OF_UPGRADE_SLOTS = DATA_COMPONENT_TYPES.register("number_of_upgrade_slots",
            () -> new DataComponentType.Builder<Integer>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());

    public static final Supplier<DataComponentType<Integer>> MAIN_COLOR = DATA_COMPONENT_TYPES.register("main_color",
            () -> new DataComponentType.Builder<Integer>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());
    public static final Supplier<DataComponentType<Integer>> ACCENT_COLOR = DATA_COMPONENT_TYPES.register("accent_color",
            () -> new DataComponentType.Builder<Integer>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());

    public static final Supplier<DataComponentType<UUID>> STORAGE_UUID = DATA_COMPONENT_TYPES.register("storage_uuid",
            () -> new DataComponentType.Builder<UUID>().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());

    public static final Supplier<DataComponentType<Integer>> OPEN_TAB_ID = DATA_COMPONENT_TYPES.register("open_tab_id",
            () -> new DataComponentType.Builder<Integer>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());

    public static final Supplier<DataComponentType<SortBy>> SORT_BY = DATA_COMPONENT_TYPES.register("sort_by",
            () -> new DataComponentType.Builder<SortBy>().persistent(SortBy.CODEC).networkSynchronized(SortBy.STREAM_CODEC).build());

    public static final Supplier<DataComponentType<CompoundTag>> RENDER_INFO_TAG = DATA_COMPONENT_TYPES.register("render_info_tag",
            () -> new DataComponentType.Builder<CompoundTag>().persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG).build());

    public static final Supplier<DataComponentType<Boolean>> SHIFT_CLICK_INTO_STORAGE = DATA_COMPONENT_TYPES.register("shift_click_into_storage",
            () -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final Supplier<DataComponentType<SimpleItemContent>> INPUT_ITEM = DATA_COMPONENT_TYPES.register("input_item",
            () -> new DataComponentType.Builder<SimpleItemContent>().persistent(SimpleItemContent.CODEC).networkSynchronized(SimpleItemContent.STREAM_CODEC).build());

    public static final Supplier<DataComponentType<SimpleItemContent>> RESULT_ITEM = DATA_COMPONENT_TYPES.register("result_item",
            () -> new DataComponentType.Builder<SimpleItemContent>().persistent(SimpleItemContent.CODEC).networkSynchronized(SimpleItemContent.STREAM_CODEC).build());

    public static final Supplier<DataComponentType<Integer>> ENERGY_STORED = DATA_COMPONENT_TYPES.register("energy_stored",
            () -> new DataComponentType.Builder<Integer>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());

    public static final Supplier<DataComponentType<Boolean>> COMPACT_NON_UNCRAFTABLE = DATA_COMPONENT_TYPES.register("compact_non_uncraftable",
            () -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final Supplier<DataComponentType<Boolean>> SHOULD_WORK_IN_GUI = DATA_COMPONENT_TYPES.register("should_work_in_gui",
            () -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final Supplier<DataComponentType<ItemContainerContents>> COOKING_INVENTORY = DATA_COMPONENT_TYPES.register("cooking_inventory",
             () -> new DataComponentType.Builder<ItemContainerContents>().persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding().build()
    );

    public static final Supplier<DataComponentType<Long>> BURN_TIME_FINISH = DATA_COMPONENT_TYPES.register("burn_time_finish",
            () -> new DataComponentType.Builder<Long>().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

    public static final Supplier<DataComponentType<Integer>> BURN_TIME_TOTAL = DATA_COMPONENT_TYPES.register("burn_time_total",
            () -> new DataComponentType.Builder<Integer>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());

    public static final Supplier<DataComponentType<Long>> COOK_TIME_FINISH = DATA_COMPONENT_TYPES.register("cook_time_finish",
            () -> new DataComponentType.Builder<Long>().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

    public static  final Supplier<DataComponentType<Integer>> COOK_TIME_TOTAL = DATA_COMPONENT_TYPES.register("cook_time_total",
            () -> new DataComponentType.Builder<Integer>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());

    public static final Supplier<DataComponentType<Boolean>> IS_COOKING = DATA_COMPONENT_TYPES.register("is_cooking",
            () -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final Supplier<DataComponentType<HungerLevel>> FEED_AT_HUNGER_LEVEL = DATA_COMPONENT_TYPES.register("feed_at_hunger_level",
            () -> new DataComponentType.Builder<HungerLevel>().persistent(HungerLevel.CODEC).networkSynchronized(HungerLevel.STREAM_CODEC).build());

    public static final Supplier<DataComponentType<Boolean>> FEED_IMMEDIATELY_WHEN_HURT = DATA_COMPONENT_TYPES.register("feed_immediately_when_hurt",
            () -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final Supplier<DataComponentType<Direction>> DIRECTION = DATA_COMPONENT_TYPES.register("direction",
            () -> new DataComponentType.Builder<Direction>().persistent(Direction.CODEC).networkSynchronized(Direction.STREAM_CODEC).build());

    public static final Supplier<DataComponentType<Boolean>> IS_PLAYING = DATA_COMPONENT_TYPES.register("is_playing",
            () -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final Supplier<DataComponentType<Boolean>> PICKUP_ITEMS = DATA_COMPONENT_TYPES.register("pickup_items",
            () -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final Supplier<DataComponentType<Boolean>> PICKUP_XP = DATA_COMPONENT_TYPES.register("pickup_xp",
            () -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final Supplier<DataComponentType<List<SimpleFluidContent>>> FLUID_FILTERS = DATA_COMPONENT_TYPES.register("fluid_filters",
            () -> new DataComponentType.Builder<List<SimpleFluidContent>>().persistent(Codec.list(SimpleFluidContent.CODEC))
                    .networkSynchronized(StreamCodecHelper.ofCollection(SimpleFluidContent.STREAM_CODEC, ArrayList::new)).build());

    public static final Supplier<DataComponentType<Boolean>> IS_INPUT = DATA_COMPONENT_TYPES.register("is_input",
            () -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final Supplier<DataComponentType<Boolean>> INTERACT_WITH_HAND = DATA_COMPONENT_TYPES.register("interact_with_hand",
            () -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final Supplier<DataComponentType<Boolean>> INTERACT_WITH_WORLD = DATA_COMPONENT_TYPES.register("interact_with_world",
            () -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final Supplier<DataComponentType<ResourceLocation>> RECIPE_ID = DATA_COMPONENT_TYPES.register("recipe_id",
            () -> new DataComponentType.Builder<ResourceLocation>().persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC).build());

    public static final Supplier<DataComponentType<SimpleFluidContent>> FLUID_CONTENTS = DATA_COMPONENT_TYPES.register("fluid_contents",
            () -> new DataComponentType.Builder<SimpleFluidContent>().persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC).build());

    public static final Supplier<DataComponentType<Boolean>> SHOULD_VOID_OVERFLOW = DATA_COMPONENT_TYPES.register("should_void_overflow",
            () -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final Supplier<DataComponentType<AutomationDirection>> AUTOMATION_DIRECTION = DATA_COMPONENT_TYPES.register("automation_direction",
            () -> new DataComponentType.Builder<AutomationDirection>().persistent(AutomationDirection.CODEC).networkSynchronized(AutomationDirection.STREAM_CODEC).build());

    public static final Supplier<DataComponentType<Integer>> LEVEL = DATA_COMPONENT_TYPES.register("level",
            () -> new DataComponentType.Builder<Integer>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());

    public static final Supplier<DataComponentType<Integer>> LEVELS_TO_STORE = DATA_COMPONENT_TYPES.register("levels_to_store",
            () -> new DataComponentType.Builder<Integer>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());

    public static final Supplier<DataComponentType<Integer>> LEVELS_TO_TAKE = DATA_COMPONENT_TYPES.register("levels_to_take",
            () -> new DataComponentType.Builder<Integer>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());

    public static final Supplier<DataComponentType<Boolean>> MEND_ITEMS = DATA_COMPONENT_TYPES.register("mend_items",
            () -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilterAttributes>> FILTER_ATTRIBUTES = DATA_COMPONENT_TYPES.register("filter_attributes",
            () -> new DataComponentType.Builder<FilterAttributes>().persistent(FilterAttributes.CODEC).networkSynchronized(FilterAttributes.STREAM_CODEC).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilterAttributes>> INPUT_FILTER_ATTRIBUTES = DATA_COMPONENT_TYPES.register("input_filter_attributes",
            () -> new DataComponentType.Builder<FilterAttributes>().persistent(FilterAttributes.CODEC).networkSynchronized(FilterAttributes.STREAM_CODEC).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilterAttributes>> FUEL_FILTER_ATTRIBUTES = DATA_COMPONENT_TYPES.register("fuel_filter_attributes",
            () -> new DataComponentType.Builder<FilterAttributes>().persistent(FilterAttributes.CODEC).networkSynchronized(FilterAttributes.STREAM_CODEC).build());

    public static final Supplier<DataComponentType<Boolean>> ENABLED = DATA_COMPONENT_TYPES.register("enabled",
            () -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static void register(IEventBus modBus) {
        DATA_COMPONENT_TYPES.register(modBus);
    }
}
