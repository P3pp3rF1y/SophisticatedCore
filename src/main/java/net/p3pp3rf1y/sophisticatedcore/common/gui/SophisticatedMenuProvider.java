package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;

public class SophisticatedMenuProvider implements MenuProvider {
    private final Component title;
    private final MenuConstructor menuConstructor;
    private final boolean triggerClientSideContainerClosingOnOpen;

    public SophisticatedMenuProvider(MenuConstructor menuConstructor, Component title) {
        this(menuConstructor, title, true);
    }

    public SophisticatedMenuProvider(MenuConstructor menuConstructor, Component title, boolean triggerClientSideContainerClosingOnOpen) {
        this.menuConstructor = menuConstructor;
        this.title = title;
        this.triggerClientSideContainerClosingOnOpen = triggerClientSideContainerClosingOnOpen;
    }

    @Override
    public Component getDisplayName() {
        return this.title;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return this.menuConstructor.createMenu(containerId, playerInventory, player);
    }

    @Override
    public boolean shouldTriggerClientSideContainerClosingOnOpen() {
        return triggerClientSideContainerClosingOnOpen;
    }
}
