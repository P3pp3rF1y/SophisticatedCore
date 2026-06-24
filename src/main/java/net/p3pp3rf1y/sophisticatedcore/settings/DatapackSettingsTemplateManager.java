package net.p3pp3rf1y.sophisticatedcore.settings;

import com.google.common.collect.Maps;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class DatapackSettingsTemplateManager {
	private DatapackSettingsTemplateManager() {
	}

	private static final Map<String, Map<String, ContainerContents.SettingsData>> TEMPLATES = Maps.newHashMap();

	public static void putTemplate(String datapackName, String templateName, ContainerContents.SettingsData data) {
		templateName = templateName.replace('_', ' ');
		templateName = capitalizeFirstLetterOfEachWord(templateName);

		TEMPLATES.computeIfAbsent(datapackName, n -> Maps.newTreeMap()).put(templateName, data);
	}

	private static String capitalizeFirstLetterOfEachWord(String input) {
		String[] words = input.split("\\s+"); // Split the string by one or more spaces
		StringBuilder builder = new StringBuilder();

		for (String word : words) {
			if (!word.isEmpty()) {
				// Capitalize the first letter and add the rest of the word
				String capitalizedWord = word.substring(0, 1).toUpperCase() + word.substring(1);
				builder.append(capitalizedWord).append(" ");
			}
		}

		return builder.toString().trim(); // Trim the trailing space
	}

	public static Map<String, Map<String, ContainerContents.SettingsData>> getTemplates() {
		return TEMPLATES;
	}

	public static Optional<ContainerContents.SettingsData> getTemplateData(String datapackName, String templateName) {
		Map<String, ContainerContents.SettingsData> datapackTemplates = TEMPLATES.get(datapackName);
		if (datapackTemplates == null) {
			return Optional.empty();
		}

		return Optional.ofNullable(datapackTemplates.get(templateName));
	}

	@SuppressWarnings("java:S6548")
	public static class Loader extends SimplePreparableReloadListener<Map<ResourceLocation, ContainerContents.SettingsData>> {
		public static final ResourceLocation KEY = SophisticatedCore.getRL("settings_templates");
		public static final Loader INSTANCE = new Loader();
		private static final String DIRECTORY = "sophisticated_settingstemplates";
		private static final String SUFFIX = ".snbt";
		private static final int PATH_SUFFIX_LENGTH = SUFFIX.length();

		private Loader() {
		}

		@Override
		protected Map<ResourceLocation, ContainerContents.SettingsData> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
			Map<ResourceLocation, ContainerContents.SettingsData> map = Maps.newHashMap();
			int i = DIRECTORY.length() + 1;

			resourceManager.listResources(DIRECTORY, fileName -> fileName.getPath().endsWith(SUFFIX)).forEach((resourcelocation, resource) -> {
				String s = resourcelocation.getPath();
				ResourceLocation resourceLocationWithoutSuffix = ResourceLocation.fromNamespaceAndPath(resourcelocation.getNamespace(),
						s.substring(i, s.length() - PATH_SUFFIX_LENGTH));

				try (InputStream inputstream = resource.open();
						Reader reader = new BufferedReader(new InputStreamReader(inputstream, StandardCharsets.UTF_8))) {
					String fileContents = IOUtils.toString(reader);

					RegistryOps<Tag> ops = getRegistryLookup().createSerializationContext(NbtOps.INSTANCE);
					Pair<ContainerContents.SettingsData, Tag> decodeResult = ContainerContents.SettingsData.CODEC
							.decode(ops, TagParser.parseCompoundFully(fileContents)).getOrThrow();
					if (map.put(resourceLocationWithoutSuffix, decodeResult.getFirst()) != null) {
						throw new IllegalStateException("Duplicate data file ignored with ID " + resourceLocationWithoutSuffix);
					}
				} catch (IllegalArgumentException | IllegalStateException | IOException | CommandSyntaxException ex) {
					SophisticatedCore.LOGGER.error("Couldn't parse data file {} from {}", resourceLocationWithoutSuffix, resourcelocation, ex);
				}
			});

			return map;
		}

		@Override
		protected void apply(Map<ResourceLocation, ContainerContents.SettingsData> templates, ResourceManager resourceManager, ProfilerFiller profiler) {
			templates.forEach((resourceLocation, data) -> {
				String datapackName = resourceLocation.getNamespace();
				String templateName = resourceLocation.getPath().substring(resourceLocation.getPath().lastIndexOf('/') + 1);
				putTemplate(datapackName, templateName, data);
			});
		}
	}
}
