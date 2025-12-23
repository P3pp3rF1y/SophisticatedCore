package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.concurrent.CompletableFuture;

public class DataGenerators {
	private DataGenerators() {}

	public static void gatherData(GatherDataEvent evt) {
		DataGenerator generator = evt.getGenerator();
		PackOutput packOutput = generator.getPackOutput();
		CompletableFuture<HolderLookup.Provider> registries = evt.getLookupProvider();
		BlockTagsProvider blockTagProvider = new BlockTagsProvider(packOutput, evt.getLookupProvider(), SophisticatedCore.MOD_ID, evt.getExistingFileHelper()){
			@Override
			protected void addTags(HolderLookup.Provider pProvider) {
				//noop
			}
		};
		generator.addProvider(evt.includeServer(), new SCFluidTagsProvider(packOutput, registries, evt.getExistingFileHelper()));
		generator.addProvider(evt.includeServer(), new SCRecipeProvider(packOutput, registries));
		generator.addProvider(evt.includeServer(), new ItemTagProvider(packOutput, registries, blockTagProvider.contentsGetter(), evt.getExistingFileHelper()));
	}
}
