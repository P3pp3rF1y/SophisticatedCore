package net.p3pp3rf1y.sophisticatedcore.settings;

import com.google.common.collect.Maps;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class DatapackSettingsTemplateManager {
	private DatapackSettingsTemplateManager() {}

	private static final Map<String, Map<String, CompoundTag>> TEMPLATES = Maps.newHashMap();

	public static void putTemplate(String datapackName, String templateName, CompoundTag tag) {
		templateName = templateName.replace('_', ' ');
		templateName = capitalizeFirstLetterOfEachWord(templateName);

		TEMPLATES.computeIfAbsent(datapackName, n -> Maps.newTreeMap()).put(templateName, tag);
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

	public static Map<String, Map<String, CompoundTag>> getTemplates() {
		return TEMPLATES;
	}

	public static Optional<CompoundTag> getTemplateNbt(String datapackName, String templateName) {
		Map<String, CompoundTag> datapackTemplates = TEMPLATES.get(datapackName);
		if (datapackTemplates == null) {
			return Optional.empty();
		}

		return Optional.ofNullable(datapackTemplates.get(templateName));
	}

	@SuppressWarnings("java:S6548")
	public static class Loader extends SimplePreparableReloadListener<Map<ResourceLocation, CompoundTag>> {
		public static final Loader INSTANCE = new Loader();
		private static final String DIRECTORY = "sophisticated_settingstemplates";
		private static final String SUFFIX = ".snbt";
		private static final int PATH_SUFFIX_LENGTH = SUFFIX.length();

		private Loader() {}
		@Override
		protected Map<ResourceLocation, CompoundTag> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
			Map<ResourceLocation, CompoundTag> map = Maps.newHashMap();
			int i = DIRECTORY.length() + 1;

			pResourceManager.listResources(DIRECTORY, fileName -> fileName.getPath().endsWith(SUFFIX)).forEach((resourcelocation, resource) -> {
				String s = resourcelocation.getPath();
				ResourceLocation resourceLocationWithoutSuffix = new ResourceLocation(resourcelocation.getNamespace(), s.substring(i, s.length() - PATH_SUFFIX_LENGTH));

				try (
						InputStream inputstream = resource.open();
						Reader reader = new BufferedReader(new InputStreamReader(inputstream, StandardCharsets.UTF_8));
				) {
					String fileContents = IOUtils.toString(reader);

					CompoundTag tag = TagParser.parseTag(fileContents);
					if (map.put(resourceLocationWithoutSuffix, tag) != null) {
						throw new IllegalStateException("Duplicate data file ignored with ID " + resourceLocationWithoutSuffix);
					}
				}
				catch (IllegalArgumentException | IOException | CommandSyntaxException ex) {
					SophisticatedCore.LOGGER.error("Couldn't parse data file {} from {}", resourceLocationWithoutSuffix, resourcelocation, ex);
				}
			});

			return map;
		}

		@Override
		protected void apply(Map<ResourceLocation, CompoundTag> templates, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
			templates.forEach((resourceLocation, tag) -> {
				String datapackName = resourceLocation.getNamespace();
				String templateName = resourceLocation.getPath().substring(resourceLocation.getPath().lastIndexOf('/') + 1);
				putTemplate(datapackName, templateName, tag);
			});
		}
	}
}
