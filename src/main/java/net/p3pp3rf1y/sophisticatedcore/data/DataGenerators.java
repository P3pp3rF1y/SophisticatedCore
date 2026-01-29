package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

public class DataGenerators {
	private DataGenerators() {}

	public static void gatherData(GatherDataEvent evt) {
		DataGenerator generator = evt.getGenerator();
		PackOutput packOutput = generator.getPackOutput();
		CompletableFuture<HolderLookup.Provider> registries = evt.getLookupProvider();
		generator.addProvider(evt.includeServer(), new SCFluidTagsProvider(packOutput, registries, evt.getExistingFileHelper()));
		generator.addProvider(evt.includeServer(), new SCRecipeProvider(packOutput, registries));
	}
}
