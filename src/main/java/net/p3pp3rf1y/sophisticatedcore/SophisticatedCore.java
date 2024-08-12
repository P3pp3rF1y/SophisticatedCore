package net.p3pp3rf1y.sophisticatedcore;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.p3pp3rf1y.sophisticatedcore.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticatedcore.common.CommonEventHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatRegistry;
import net.p3pp3rf1y.sophisticatedcore.data.DataGenerators;
import net.p3pp3rf1y.sophisticatedcore.init.ModCompat;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.StorageWrapperRepository;
import net.p3pp3rf1y.sophisticatedcore.settings.DatapackSettingsTemplateManager;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SophisticatedCore.MOD_ID)
public class SophisticatedCore {
	public static final String MOD_ID = "sophisticatedcore";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	public final CommonEventHandler commonEventHandler = new CommonEventHandler();

	@SuppressWarnings("java:S1118") //needs to be public for mod to work
	public SophisticatedCore(IEventBus modBus, Dist dist, ModContainer container) {
		container.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
		container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
		commonEventHandler.registerHandlers(modBus);
		ModCompat.register();
		if (dist == Dist.CLIENT) {
			ClientEventHandler.registerHandlers(modBus);
		}
		Config.COMMON.initListeners(modBus);
		ModCoreDataComponents.register(modBus);
		modBus.addListener((FMLConstructModEvent event) -> construct(event, modBus));
		modBus.addListener(SophisticatedCore::setup);
		modBus.addListener(DataGenerators::gatherData);

		IEventBus eventBus = NeoForge.EVENT_BUS;
		eventBus.addListener(SophisticatedCore::serverStarted);
		eventBus.addListener(SophisticatedCore::serverStopped);
		eventBus.addListener(SophisticatedCore::onResourceReload);
	}

	private static void serverStarted(ServerStartedEvent event) {
		ServerLevel world = event.getServer().getLevel(Level.OVERWORLD);
		if (world != null) {
			RecipeHelper.setLevel(world);
			StorageWrapperRepository.clearCache();
		}
	}

	private static void serverStopped(ServerStartedEvent event) {
		StorageWrapperRepository.clearCache();
	}

	private static void construct(FMLConstructModEvent event, IEventBus modBus) {
		event.enqueueWork(() -> CompatRegistry.initCompats(modBus));
	}

	private static void setup(FMLCommonSetupEvent event) {
		event.enqueueWork(CompatRegistry::setupCompats);
	}

	private static void onResourceReload(AddReloadListenerEvent event) {
		event.addListener(DatapackSettingsTemplateManager.Loader.INSTANCE);
	}

	public static ResourceLocation getRL(String regName) {
		return ResourceLocation.parse(getRegistryName(regName));
	}

	public static String getRegistryName(String regName) {
		return MOD_ID + ":" + regName;
	}
}
