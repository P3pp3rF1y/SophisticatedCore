package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("java:S4144")
// this is noop wrapper and thus identical implementation isn't an issue especially when it means just returning same field
public class NoopStorageWrapper implements IStorageWrapper {
	public static final NoopStorageWrapper INSTANCE = new NoopStorageWrapper();

	@Nullable
	private UpgradeHandler upgradeHandler;
	@Nullable
	private InventoryHandler inventoryHandler;
	@Nullable
	private RenderDataHandler renderDataHandler;
	@Nullable
	private SettingsHandler settingsHandler;

	protected NoopStorageWrapper() {
	}

	@Override
	public void setContentsChangeHandler(Runnable contentsChangeHandler) {
		// noop
	}

	@Override
	public ITrackedContentsItemResourceHandler getInventoryForUpgradeProcessing() {
		return getInventoryHandler();
	}

	@Override
	public InventoryHandler getInventoryHandler() {
		if (inventoryHandler == null) {
			inventoryHandler = new InventoryHandler(0, this, new ContainerContents(), () -> {
			}, 64, new StackUpgradeConfig(new ModConfigSpec.Builder())) {
				@Override
				protected boolean isAllowed(ItemResource resource) {
					return true;
				}
			};
		}
		return inventoryHandler;
	}

	@Override
	public ITrackedContentsItemResourceHandler getInventoryForInputOutput() {
		return getInventoryHandler();
	}

	@Override
	public SettingsHandler getSettingsHandler() {
		if (settingsHandler == null) {
			settingsHandler = new SettingsHandler(new ContainerContents.SettingsData(), () -> {
			}, this::getInventoryHandler, this::getRenderDataHandler, "") {
				@Override
				protected void addItemDisplayCategory(Supplier<InventoryHandler> inventoryHandlerSupplier,
						Supplier<RenderDataHandler> renderDataHandlerSupplier, ContainerContents.SettingsData settingsData) {
					// noop
				}
			};
		}
		return settingsHandler;
	}

	@Override
	public UpgradeHandler getUpgradeHandler() {
		if (upgradeHandler == null) {
			upgradeHandler = new UpgradeHandler(0, this, new ContainerContents(), () -> {
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
		// noop
	}

	@Override
	public void removeOpenTabId() {
		// noop
	}

	@Override
	public void setColors(int mainColor, int accentColor) {
		// noop
	}

	@Override
	public void setSortBy(SortBy sortBy) {
		// noop
	}

	@Override
	public SortBy getSortBy() {
		return SortBy.NAME;
	}

	@Override
	public void sort() {
		// noop
	}

	@Override
	public void onContentsUpdated() {
		// noop
	}

	@Override
	public void refreshInventoryForUpgradeProcessing() {
		// noop
	}

	@Override
	public void refreshInventoryForInputOutput() {
		// noop
	}

	@Override
	public void setPersistent(boolean persistent) {
		// noop
	}

	@Override
	public void fillWithLoot(Player playerEntity) {
		// noop
	}

	@Override
	public RenderDataHandler getRenderDataHandler() {
		if (renderDataHandler == null) {
			renderDataHandler = new RenderDataHandler(new RenderData(), renderData -> {
			}) {
			};
		}
		return renderDataHandler;
	}

	@Override
	public void setColumnsTaken(int columnsTaken, boolean hasChanged) {
		// noop
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
		return Component.empty();
	}
}
