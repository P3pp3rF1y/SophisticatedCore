package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;

import java.util.Optional;

public class DataGenerators {
	private DataGenerators() {
	}

	public static void gatherData(GatherDataEvent.Client evt) {
		evt.createProvider(CoreFluidTagsProvider::new);
		evt.createProvider(CoreRecipeProvider.Runner::new);
		evt.createProvider(CoreModelProvider::new);
	}

	private static class CoreModelProvider extends SophisticatedModelProvider {
		public CoreModelProvider(PackOutput output) {
			super(output, SophisticatedCore.MOD_ID);
		}

		@Override
		protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
			itemModels.itemModelOutput.accept(ModFluids.XP_BUCKET.get(),
					new DynamicFluidContainerModel.Unbaked(
							new DynamicFluidContainerModel.Textures(Optional.of(Identifier.withDefaultNamespace("item/bucket")),
									Optional.of(Identifier.withDefaultNamespace("item/bucket")),
									Optional.of(Identifier.fromNamespaceAndPath("neoforge", "item/mask/bucket_fluid")), Optional.empty()),
							ModFluids.XP_STILL.get(), false, false, false));
		}
	}
}
