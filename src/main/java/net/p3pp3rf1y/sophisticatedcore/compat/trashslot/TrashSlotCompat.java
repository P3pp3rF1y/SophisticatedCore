package net.p3pp3rf1y.sophisticatedcore.compat.trashslot;

import net.blay09.mods.trashslot.api.event.RegisterTrashSlotContainerLayoutsEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.world.inventory.MenuType;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

import java.util.HashSet;
import java.util.Set;

public class TrashSlotCompat implements ICompat {
	private static final Set<MenuType<?>> MENU_TYPES = new HashSet<>();

	public static void registerMenuType(MenuType<?> menuType) {
		MENU_TYPES.add(menuType);
	}

	@Override
	public void init(IEventBus modBus) {
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			RegisterTrashSlotContainerLayoutsEvent.EVENT.register(this::onRegisterLayouts);
		}
	}

	@Override
	public void setup() {
		//noop
	}

	private void onRegisterLayouts(RegisterTrashSlotContainerLayoutsEvent event) {
		MENU_TYPES.forEach(menuType -> event.registerLayout(menuType, SophisticatedContainerLayout.INSTANCE));
	}
}
