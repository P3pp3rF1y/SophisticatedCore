package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("java:S4144")
//this is noop wrapper and thus identical implementation isn't an issue especially when it means just returning same field
public class NoopStorageWrapper implements IStorageWrapper {
    public static final NoopStorageWrapper INSTANCE = new NoopStorageWrapper();

    @Nullable
    private UpgradeHandler upgradeHandler;
    @Nullable
    private InventoryHandler inventoryHandler;
    @Nullable
    private RenderInfo renderInfo;
    @Nullable
    private SettingsHandler settingsHandler;

    protected NoopStorageWrapper() {
    }

    @Override
    public void setSaveHandler(Runnable saveHandler) {
        //noop
    }

    @Override
    public ITrackedContentsItemHandler getInventoryForUpgradeProcessing() {
        return getInventoryHandler();
    }

    @Override
    public InventoryHandler getInventoryHandler() {
        if (inventoryHandler == null) {
            inventoryHandler = new InventoryHandler(0, this, new CompoundTag(), () -> {
            }, 64, new StackUpgradeConfig(new ForgeConfigSpec.Builder())) {
                @Override
                protected boolean isAllowed(ItemStack stack) {
                    return true;
                }
            };
        }
        return inventoryHandler;
    }

    @Override
    public ITrackedContentsItemHandler getInventoryForInputOutput() {
        return getInventoryHandler();
    }

    @Override
    public SettingsHandler getSettingsHandler() {
        if (settingsHandler == null) {
            settingsHandler = new SettingsHandler(new CompoundTag(), () -> {
            }, this::getInventoryHandler, this::getRenderInfo) {
                @Override
                protected CompoundTag getSettingsNbtFromContentsNbt(CompoundTag contentsNbt) {
                    return contentsNbt;
                }

                @Override
                protected void addItemDisplayCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderInfo> renderInfoSupplier, CompoundTag settingsNbt) {
                    //noop
                }

                @Override
                public String getGlobalSettingsCategoryName() {
                    return "";
                }

                @Override
                public ISettingsCategory<?> instantiateGlobalSettingsCategory(CompoundTag categoryNbt, Consumer<CompoundTag> saveNbt) {
                    return new MainSettingsCategory<>(categoryNbt, saveNbt, "");
                }

                @Override
                protected void saveCategoryNbt(CompoundTag settingsNbt, String categoryName, CompoundTag tag) {
                    //noop
                }
            };
        }
        return settingsHandler;
    }

    @Override
    public UpgradeHandler getUpgradeHandler() {
        if (upgradeHandler == null) {
            upgradeHandler = new UpgradeHandler(0, this, new CompoundTag(), () -> {
            }, () -> {
            });
        }

        return upgradeHandler;
    }

    @Override
    public Optional<UUID> getContentsUuid() {
        return Optional.empty();
    }

    @Override
    public int getMainColor() {
        return -1;
    }

    @Override
    public int getAccentColor() {
        return -1;
    }

    @Override
    public Optional<Integer> getOpenTabId() {
        return Optional.empty();
    }

    @Override
    public void setOpenTabId(int openTabId) {
        //noop
    }

    @Override
    public void removeOpenTabId() {
        //noop
    }

    @Override
    public void setColors(int mainColor, int accentColor) {
        //noop
    }

    @Override
    public void setSortBy(SortBy sortBy) {
        //noop
    }

    @Override
    public SortBy getSortBy() {
        return SortBy.NAME;
    }

    @Override
    public void sort() {
        //noop
    }

    @Override
    public void onContentsNbtUpdated() {
        //noop
    }

    @Override
    public void refreshInventoryForUpgradeProcessing() {
        //noop
    }

    @Override
    public void refreshInventoryForInputOutput() {
        //noop
    }

    @Override
    public void setPersistent(boolean persistent) {
        //noop
    }

    @Override
    public void fillWithLoot(Player playerEntity) {
        //noop
    }

    @Override
    public RenderInfo getRenderInfo() {
        if (renderInfo == null) {
            renderInfo = new RenderInfo(() -> () -> {
            }) {

                @Override
                protected void serializeRenderInfo(CompoundTag renderInfo) {
                    //noop
                }

                @Override
                protected Optional<CompoundTag> getRenderInfoTag() {
                    return Optional.empty();
                }
            };
        }
        return renderInfo;
    }

    @Override
    public void setColumnsTaken(int columnsTaken, boolean hasChanged) {
        //noop
    }

    @Override
    public int getColumnsTaken() {
        return 0;
    }

    @Override
    public String getStorageType() {
        return "";
    }

    @Override
    public Component getDisplayName() {
        return TextComponent.EMPTY;
    }
}
