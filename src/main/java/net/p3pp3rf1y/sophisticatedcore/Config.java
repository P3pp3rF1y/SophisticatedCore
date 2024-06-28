package net.p3pp3rf1y.sophisticatedcore;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SortButtonsPosition;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Config {

	private Config() {}

	public static final Client CLIENT;
	public static final ModConfigSpec CLIENT_SPEC;
	public static final Common COMMON;
	public static final ModConfigSpec COMMON_SPEC;

	static {
		final Pair<Client, ModConfigSpec> clientSpec = new ModConfigSpec.Builder().configure(Client::new);
		CLIENT_SPEC = clientSpec.getRight();
		CLIENT = clientSpec.getLeft();

		final Pair<Common, ModConfigSpec> commonSpec = new ModConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = commonSpec.getRight();
		COMMON = commonSpec.getLeft();
	}

	public static class Client {
		public final ModConfigSpec.EnumValue<SortButtonsPosition> sortButtonsPosition;
		public final ModConfigSpec.BooleanValue playButtonSound;

		Client(ModConfigSpec.Builder builder) {
			builder.comment("Client Settings").push("client");
			sortButtonsPosition = builder.comment("Positions where sort buttons can display to help with conflicts with controls from other mods").defineEnum("sortButtonsPosition", SortButtonsPosition.TITLE_LINE_RIGHT);
			playButtonSound = builder.comment("Whether click sound should play when custom buttons are clicked in gui").define("playButtonSound", true);
			builder.pop();
		}
	}

	public static class Common {
		public final EnabledItems enabledItems;

		public void initListeners(IEventBus modBus) {
			modBus.addListener(this::onConfigReload);
		}

		@SuppressWarnings("unused") //need the Event parameter for forge reflection to understand what event this listens to
		public void onConfigReload(ModConfigEvent.Reloading event) {
			enabledItems.enabledMap.clear();
		}

		Common(ModConfigSpec.Builder builder) {
			builder.comment("Common Settings").push("common");

			enabledItems = new EnabledItems(builder);
		}

		public static class EnabledItems {
			private final ModConfigSpec.ConfigValue<List<String>> itemsEnableList;
			private final Map<ResourceLocation, Boolean> enabledMap = new ConcurrentHashMap<>();

			EnabledItems(ModConfigSpec.Builder builder) {
				itemsEnableList = builder.comment("Disable / enable any items here (disables their recipes)").define("enabledItems", new ArrayList<>());
			}

			public boolean isItemEnabled(Item item) {
				return RegistryHelper.getRegistryName(BuiltInRegistries.ITEM, item).map(this::isItemEnabled).orElse(false);
			}

			public boolean isItemEnabled(ResourceLocation itemRegistryName) {
				if (!COMMON_SPEC.isLoaded()) {
					return true;
				}
				if (enabledMap.isEmpty()) {
					loadEnabledMap();
				}
				return enabledMap.computeIfAbsent(itemRegistryName, irn -> {
					addEnabledItemToConfig(itemRegistryName);
					return true;
				});
			}

			private void addEnabledItemToConfig(ResourceLocation itemRegistryName) {
				List<String> list = itemsEnableList.get();
				list.add(itemRegistryName + "|true");
				itemsEnableList.set(list);
			}

			private void loadEnabledMap() {
				for (String itemEnabled : itemsEnableList.get()) {
					String[] data = itemEnabled.split("\\|");
					if (data.length == 2) {
						enabledMap.put(ResourceLocation.fromNamespaceAndPath(data[0]), Boolean.valueOf(data[1]));
					} else {
						SophisticatedCore.LOGGER.error("Wrong data for enabledItems - expected registry name|true/false when {} was provided", itemEnabled);
					}
				}
			}
		}

	}
}
