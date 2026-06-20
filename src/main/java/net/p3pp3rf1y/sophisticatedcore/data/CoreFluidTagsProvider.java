package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;

import java.util.concurrent.CompletableFuture;

public class CoreFluidTagsProvider extends FluidTagsProvider {
	public CoreFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
		super(output, provider, SophisticatedCore.MOD_ID);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		tag(ModFluids.EXPERIENCE_TAG).add(ModFluids.XP_STILL.get().builtInRegistryHolder().key());
	}
}
