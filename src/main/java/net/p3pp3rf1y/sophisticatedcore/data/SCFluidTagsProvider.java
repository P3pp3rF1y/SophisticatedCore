package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class SCFluidTagsProvider extends FluidTagsProvider {
	public SCFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider,
			@Nullable ExistingFileHelper existingFileHelper) {
		super(output, provider, SophisticatedCore.MOD_ID, existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.Provider pProvider) {
		tag(ModFluids.EXPERIENCE_TAG).add(ModFluids.XP_STILL.get());
	}
}
