package net.p3pp3rf1y.sophisticatedcore.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

public class ModTags {
    private ModTags() {
    }

    public static final TagKey<Item> CAN_PLAY = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "can_play"));

    public static void registerTags() {
    }
}
