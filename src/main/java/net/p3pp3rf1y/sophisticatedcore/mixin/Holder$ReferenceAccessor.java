package net.p3pp3rf1y.sophisticatedcore.mixin;

import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(Holder.Reference.class)
public interface Holder$ReferenceAccessor {
	// Provides direct access to the underlying Set in order to avoid Stream overhead. Do not mutate this set.
	@Accessor
	Set<TagKey<Item>> getTags();
}
