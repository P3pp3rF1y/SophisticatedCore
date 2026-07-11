package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.network.SyncDatapackSettingsTemplatePayload;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.DatapackSettingsTemplateManager;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTemplateStorage;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplatePersistanceContainer {
	private static final Pattern EXPORT_FILE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_\\-\\s]+");
	private static final int TOTAL_ORDINAL_SAVE_SLOTS = 10;
	private static final String ACTION_TAG = "action";
	private static final String SAVE_SLOT_TAG = "saveSlot";
	private static final String LOAD_SLOT_TAG = "loadSlot";
	static final String TEMPLATE_PERSISTANCE_TAG = "templatePersistance";
	private final SettingsContainerMenu<?> settingsContainer;
	private final RegistryAccess registryAccess;

	private final List<IPersistanceSlot> saveSlots = new ArrayList<>();
	private int saveSlotIndex = 0;
	private final List<IPersistanceSlot> loadSlots = new ArrayList<>();
	private int loadSlotIndex = -1;
	@Nullable
	private TemplateSettingsHandler selectedTemplate;
	private Runnable onSlotsRefreshed = () -> {
	};

	public TemplatePersistanceContainer(SettingsContainerMenu<?> settingsContainer, RegistryAccess registryAccess) {
		this.settingsContainer = settingsContainer;
		this.registryAccess = registryAccess;

		initSlots();
	}

	public void setOnSlotsRefreshed(Runnable onSlotsRefreshed) {
		this.onSlotsRefreshed = onSlotsRefreshed;
	}

	private void initSlots() {
		saveSlots.clear();
		int i;
		for (i = 0; i < getNumberOfSaves(); i++) {
			saveSlots.add(new OrdinalPersistanceSlot(i + 1));
		}
		if (i < TOTAL_ORDINAL_SAVE_SLOTS - 1) {
			saveSlots.add(new OrdinalPersistanceSlot(i + 1));
		}
		getNamedSaves().forEach(name -> saveSlots.add(new NamedPersistanceSlot(name)));
		saveSlots.add(new EditNamePersistanceSlot(""));

		loadSlots.clear();
		for (i = 0; i < getNumberOfSaves(); i++) {
			loadSlots.add(new OrdinalPersistanceSlot(i + 1));
		}
		getNamedSaves().forEach(name -> loadSlots.add(new NamedPersistanceSlot(name)));

		DatapackSettingsTemplateManager.getTemplates().forEach(
				(datapackName, templates) -> templates.forEach((templateName, templateNbt) -> loadSlots.add(new DatapackSlot(datapackName, templateName))));

		if (loadSlotIndex == -1 && !loadSlots.isEmpty()) {
			loadSlotIndex = 0;
		} else if (loadSlotIndex != -1 && loadSlots.isEmpty()) {
			loadSlotIndex = -1;
		}
		updateSelectedTemplate();
		onSlotsRefreshed.run();
	}

	public void handlePacket(CompoundTag data) {
		data.getString(ACTION_TAG).ifPresent(action -> {
			switch (action) {
				case "saveTemplate" -> data.getString("slotName").ifPresent(this::saveTemplate);
				case "loadTemplate" -> loadTemplate();
				case "exportTemplate" -> data.getString("fileName").ifPresent(this::exportTemplate);
			}
		});
		data.getBoolean(SAVE_SLOT_TAG).ifPresent(this::scrollSaveSlot);
		data.getBoolean(LOAD_SLOT_TAG).ifPresent(this::scrollLoadSlot);
	}

	private void sendDataToServer(Supplier<CompoundTag> compoundSupplier) {
		settingsContainer.sendDataToServer(() -> {
			CompoundTag compound = new CompoundTag();
			compound.put(TEMPLATE_PERSISTANCE_TAG, compoundSupplier.get());
			return compound;
		});
	}

	private Player getPlayer() {
		return settingsContainer.getPlayer();
	}

	public void loadTemplate() {
		if (selectedTemplate == null) {
			return;
		}
		TemplateSettingsHandler templateToLoad = createTemplateSettingsHandler(selectedTemplate.getSettingsData().copy());
		templateToLoad.getSettingsCategories().values()
				.forEach(category -> category.deleteSlotSettingsFrom(settingsContainer.getStorageWrapper().getInventoryHandler().size()));
		settingsContainer.getStorageWrapper().getSettingsHandler().getSettingsCategories().values().forEach(category -> {
			// noinspection unchecked
			overwriteCategory(category.getClass(), category, templateToLoad.getTypeCategory(category.getClass()));
		});

		sendDataToServer(() -> NBTHelper.putString(new CompoundTag(), ACTION_TAG, "loadTemplate"));

		if (getPlayer().level().isClientSide()) {
			getPlayer().sendSystemMessage(
					Component.translatable(TranslationHelper.INSTANCE.translSettingsMessage("load_template"), loadSlots.get(loadSlotIndex).getSlotName()));
		}
	}

	private <T extends ISettingsCategory<T, ?>> void overwriteCategory(Class<T> categoryClazz, ISettingsCategory<?, ?> currentCategory,
			ISettingsCategory<?, ?> otherCategory) {
		// noinspection unchecked
		((T) currentCategory).overwriteWith(((T) otherCategory));
	}

	public void saveTemplate(String slotName) {
		SettingsTemplateStorage settingsTemplateStorage = SettingsTemplateStorage.get();

		IPersistanceSlot saveSlot = saveSlots.get(saveSlotIndex);
		saveSlot.setSlotName(slotName);
		if (saveSlot.showsTextbox() && slotName.isBlank()) {
			getPlayer().sendSystemMessage(
					Component.translatable(TranslationHelper.INSTANCE.translSettingsMessage("save_template.empty_name")).withStyle(ChatFormatting.RED));
			return;
		}
		saveSlot.persistTo(getPlayer(), settingsTemplateStorage, settingsContainer.getStorageWrapper().getSettingsHandler().getSettingsData().copy());

		sendDataToServer(() -> NBTHelper.putString(NBTHelper.putString(new CompoundTag(), ACTION_TAG, "saveTemplate"), "slotName", slotName));

		initSlots();

		moveSaveSlotIndexTo(saveSlot.getSlotName());

		if (getPlayer().level().isClientSide()) {
			getPlayer().sendSystemMessage(Component.translatable(TranslationHelper.INSTANCE.translSettingsMessage("save_template"), saveSlot.getSlotName()));
		}
	}

	private void moveSaveSlotIndexTo(String slotName) {
		for (int i = 0; i < saveSlots.size(); i++) {
			if (saveSlots.get(i).getSlotName().equals(slotName)) {
				saveSlotIndex = i;
				break;
			}
		}
	}

	public void scrollSaveSlot(boolean next) {
		saveSlotIndex += next ? 1 : -1;

		if (saveSlotIndex < 0) {
			saveSlotIndex = saveSlots.size() - 1;
		}
		if (saveSlotIndex >= saveSlots.size()) {
			saveSlotIndex = 0;
		}

		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), SAVE_SLOT_TAG, next));
	}

	public void scrollLoadSlot(boolean next) {
		if (loadSlots.isEmpty()) {
			loadSlotIndex = -1;
			return;
		}

		loadSlotIndex += next ? 1 : -1;

		if (loadSlotIndex < 0) {
			loadSlotIndex = loadSlots.size() - 1;
		}
		if (loadSlotIndex >= loadSlots.size()) {
			loadSlotIndex = 0;
		}

		updateSelectedTemplate();
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), LOAD_SLOT_TAG, next));
	}

	private void updateSelectedTemplate() {
		if (loadSlotIndex > -1 && loadSlotIndex < loadSlots.size()) {
			ContainerContents.SettingsData data = loadSlots.get(loadSlotIndex).getSettingsData(getPlayer(), SettingsTemplateStorage.get());
			selectedTemplate = createTemplateSettingsHandler(data);
		}
	}

	private TemplateSettingsHandler createTemplateSettingsHandler(ContainerContents.SettingsData data) {
		return new TemplateSettingsHandler(data, registryAccess, settingsContainer.getStorageWrapper().getSettingsHandler().getPlayerSettingsName()) {
			@Override
			protected SettingsHandler getCurrentSettingsHandler() {
				return settingsContainer.getStorageWrapper().getSettingsHandler();
			}
		};
	}

	public MutableComponent getSaveSlotTooltipName() {
		return saveSlots.get(saveSlotIndex).getSlotTooltipName();
	}

	public MutableComponent getLoadSlotTooltipName() {
		return loadSlots.get(loadSlotIndex).getSlotTooltipName();
	}

	public int getLoadSlot() {
		return loadSlotIndex;
	}

	private int getNumberOfSaves() {
		return SettingsTemplateStorage.get().getPlayerTemplates(settingsContainer.getPlayer()).size();
	}

	private List<String> getNamedSaves() {
		return new ArrayList<>(SettingsTemplateStorage.get().getPlayerNamedTemplates(settingsContainer.getPlayer()).keySet());
	}

	public Optional<TemplateSettingsHandler> getSelectedTemplate() {
		return Optional.ofNullable(selectedTemplate);
	}

	public boolean showsTextbox() {
		return saveSlots.get(saveSlotIndex).showsTextbox();
	}

	public void refreshTemplateSlots() {
		initSlots();
		onSlotsRefreshed.run();
	}

	public void exportTemplate(String fileName) {
		if (fileName.isEmpty()) {
			getPlayer().sendSystemMessage(
					Component.translatable(TranslationHelper.INSTANCE.translSettingsMessage("export_template.empty_name")).withStyle(ChatFormatting.RED));
			return;
		}

		Matcher matcher = EXPORT_FILE_NAME_PATTERN.matcher(fileName);
		if (!matcher.matches()) {
			getPlayer().sendSystemMessage(Component.translatable(TranslationHelper.INSTANCE.translSettingsMessage("export_template.invalid_characters"),
					findNonMatchingCharacters(matcher, fileName)).withStyle(ChatFormatting.RED));
			return;
		}

		fileName = fileName.replace(' ', '_');

		fileName = fileName.toLowerCase(Locale.ROOT);

		String finalFileName = fileName;
		sendDataToServer(() -> NBTHelper.putString(NBTHelper.putString(new CompoundTag(), ACTION_TAG, "exportTemplate"), "fileName", finalFileName));

		if (getPlayer() instanceof ServerPlayer serverPlayer) {
			ServerLevel serverLevel = serverPlayer.level();
			Path datapacksDir = serverLevel.getServer().getWorldPath(LevelResource.DATAPACK_DIR);

			String playersFolder = getPlayer().getScoreboardName().toLowerCase(Locale.ROOT) + "_soph_templates";

			Path datapackRoot = datapacksDir.resolve(playersFolder);
			Path templatesDir = datapackRoot.resolve("data/" + playersFolder + "/sophisticated_settingstemplates");

			if (!initDatapackStructure(datapackRoot, templatesDir)) {
				return;
			}

			Path exportPath = templatesDir.resolve(fileName + ".snbt");
			ContainerContents.SettingsData data = settingsContainer.getStorageWrapper().getSettingsHandler().getSettingsData().copy();
			try {
				RegistryOps<Tag> ops = RegistryHelper.getRegistryAccess().orElseThrow().createSerializationContext(NbtOps.INSTANCE);
				NbtToSnbt.writeSnbt(CachedOutput.NO_CACHE, exportPath,
						(new SnbtPrinterTagVisitor()).visit(ContainerContents.SettingsData.CODEC.encode(data, ops, ops.empty()).getOrThrow()));
			} catch (IOException e) {
				SophisticatedCore.LOGGER.error("Error writing template export", e);
				return;
			}

			DatapackSettingsTemplateManager.putTemplate(playersFolder, fileName, data);

			PacketDistributor.sendToPlayer(serverPlayer, new SyncDatapackSettingsTemplatePayload(playersFolder, fileName, data));

			initSlots();

			getPlayer().sendSystemMessage(Component.translatable(TranslationHelper.INSTANCE.translSettingsMessage("export_template"),
					serverLevel.getServer().getWorldPath(LevelResource.ROOT).relativize(exportPath).toString()));
		}
	}

	public static String findNonMatchingCharacters(Matcher matcher, String input) {
		StringBuilder nonMatchingCharacters = new StringBuilder();

		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (!matcher.reset(String.valueOf(c)).matches()) {
				nonMatchingCharacters.append(c);
			}
		}

		return nonMatchingCharacters.toString();
	}

	public boolean templateHasTooManySlots() {
		return selectedTemplate != null && selectedTemplate.getSettingsCategories().values().stream()
				.anyMatch(category -> category.isLargerThanNumberOfSlots(settingsContainer.getStorageWrapper().getInventoryHandler().size()));
	}

	private static boolean initDatapackStructure(Path datapackRoot, Path templatesDir) {
		try {
			Files.createDirectories(templatesDir);
		} catch (IOException e) {
			SophisticatedCore.LOGGER.error("Error creating directory for template export", e);
			return false;
		}
		Path packMcmetaFile = datapackRoot.resolve("pack.mcmeta");
		if (!Files.exists(packMcmetaFile)) {
			try {
				Files.writeString(packMcmetaFile, """
						{
						    "pack": {
						        "pack_format": 15,
						        "description": "Sophisticated Settings Templates data pack"
						    }
						}
						""");
			} catch (IOException e) {
				SophisticatedCore.LOGGER.error("Error creating pack.mcmeta for template export", e);
				return false;
			}
		}
		return true;
	}

	public Optional<String> getLoadSlotSource() {
		return loadSlots.get(loadSlotIndex).getSlotSource();
	}

	public abstract static class TemplateSettingsHandler extends SettingsHandler {

		protected TemplateSettingsHandler(ContainerContents.SettingsData settingsData, RegistryAccess registryAccess, String playerSettingsName) {
			super(settingsData, () -> {
			}, NoopStorageWrapper.INSTANCE::getInventoryHandler, NoopStorageWrapper.INSTANCE::getRenderDataHandler, playerSettingsName);
		}

		protected abstract SettingsHandler getCurrentSettingsHandler();

		@Override
		protected void addItemDisplayCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderDataHandler> renderDataHandlerSupplier,
				ContainerContents.SettingsData settingsData) {
			int itemNumberLimit = getCurrentSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).getItemNumberLimit();
			this.addSettingsCategory(settingsData, ItemDisplaySettingsCategory.NAME, markContentsDirty,
					(categoryData, saveNbt) -> new ItemDisplaySettingsCategory(inventoryHandlerSupplier, renderDataHandlerSupplier, categoryData, saveNbt,
							itemNumberLimit, () -> getTypeCategory(MemorySettingsCategory.class)),
					ItemDisplaySettingsCategoryData::new);
		}
	}

	private interface IPersistanceSlot {
		String getName();

		String getSlotName();

		default void serialize(CompoundTag tag) {
			tag.putString("name", getName());
		}

		default void persistTo(Player player, SettingsTemplateStorage settingsTemplateStorage, ContainerContents.SettingsData settingsData) {
			// noop
		}

		default boolean showsTextbox() {
			return false;
		}

		default void setSlotName(String slotName) {
			// noop
		}

		default MutableComponent getSlotTooltipName() {
			return Component.literal(getSlotName());
		}

		ContainerContents.SettingsData getSettingsData(Player player, SettingsTemplateStorage settingsTemplateStorage);

		default Optional<String> getSlotSource() {
			return Optional.empty();
		}
	}

	private static class OrdinalPersistanceSlot implements IPersistanceSlot {
		private final int slot;

		public OrdinalPersistanceSlot(int slot) {
			this.slot = slot;
		}

		@Override
		public String getName() {
			return "ordinal";
		}

		@Override
		public String getSlotName() {
			return String.valueOf(slot);
		}

		@Override
		public void serialize(CompoundTag tag) {
			IPersistanceSlot.super.serialize(tag);
			tag.putInt("slot", slot);
		}

		@Override
		public void persistTo(Player player, SettingsTemplateStorage settingsTemplateStorage, ContainerContents.SettingsData settingsData) {
			settingsTemplateStorage.putPlayerTemplate(player, slot, settingsData);
		}

		@Override
		public ContainerContents.SettingsData getSettingsData(Player player, SettingsTemplateStorage settingsTemplateStorage) {
			return settingsTemplateStorage.getPlayerTemplates(player).getOrDefault(slot, new ContainerContents.SettingsData());
		}
	}

	private static class NamedPersistanceSlot implements IPersistanceSlot {
		protected String slotName;

		public NamedPersistanceSlot(String slotName) {
			this.slotName = slotName;
		}

		@Override
		public String getName() {
			return "named";
		}

		@Override
		public String getSlotName() {
			return slotName;
		}

		@Override
		public void serialize(CompoundTag tag) {
			IPersistanceSlot.super.serialize(tag);
			tag.putString("slot", slotName);
		}

		@Override
		public void persistTo(Player player, SettingsTemplateStorage settingsTemplateStorage, ContainerContents.SettingsData settingsCopy) {
			settingsTemplateStorage.putPlayerNamedTemplate(player, slotName, settingsCopy);
		}

		@Override
		public ContainerContents.SettingsData getSettingsData(Player player, SettingsTemplateStorage settingsTemplateStorage) {
			return settingsTemplateStorage.getPlayerNamedTemplates(player).getOrDefault(slotName, new ContainerContents.SettingsData());
		}
	}

	private static class DatapackSlot implements IPersistanceSlot {

		private final String datapackName;
		private final String templateName;

		public DatapackSlot(String datapackName, String templateName) {
			this.datapackName = datapackName;
			this.templateName = templateName;
		}

		@Override
		public String getName() {
			return "datapack";
		}

		@Override
		public String getSlotName() {
			return templateName;
		}

		@Override
		public ContainerContents.SettingsData getSettingsData(Player player, SettingsTemplateStorage settingsTemplateStorage) {
			return DatapackSettingsTemplateManager.getTemplateData(datapackName, templateName).orElseGet(ContainerContents.SettingsData::new);
		}

		@Override
		public Optional<String> getSlotSource() {
			return Optional.of(datapackName);
		}
	}

	private static class EditNamePersistanceSlot extends NamedPersistanceSlot {
		public EditNamePersistanceSlot(String slotName) {
			super(slotName);
		}

		@Override
		public boolean showsTextbox() {
			return true;
		}

		@Override
		public void setSlotName(String slotName) {
			this.slotName = slotName;
		}

		@Override
		public MutableComponent getSlotTooltipName() {
			return Component.translatable(TranslationHelper.INSTANCE.translSettingsButton("save_template.custom_name_slot"));
		}
	}
}
