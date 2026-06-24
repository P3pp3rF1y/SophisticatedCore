package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.common.plugins.REIServerPlugin;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessorRegistry;
import me.shedaniel.rei.forge.REIPluginCommon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

@SuppressWarnings("unused")
@REIPluginCommon
public class CoreReiCommonPlugin implements REIServerPlugin {
	@Override
	public void registerSlotAccessors(SlotAccessorRegistry registry) {
		registry.register(new ResourceLocation(SophisticatedCore.MOD_ID, "storage"), slotAccessor -> slotAccessor instanceof ReiSlotAccessor,
				new SlotAccessorRegistry.Serializer() {
					@Override
					public SlotAccessor read(AbstractContainerMenu menu, Player player, CompoundTag tag) {
						int slot = tag.getInt("Slot");
						return new ReiSlotAccessor(menu.getSlot(slot));
					}

					@Override
					public CompoundTag save(AbstractContainerMenu menu, Player player, SlotAccessor accessor) {
						if (!(accessor instanceof ReiSlotAccessor reiSlotAccessor)) {
							throw new IllegalArgumentException("Cannot save non-sophisticated slot accessor!");
						}
						CompoundTag tag = new CompoundTag();
						tag.putInt("Slot", reiSlotAccessor.getIndex());
						return tag;
					}
				});
	}
}
