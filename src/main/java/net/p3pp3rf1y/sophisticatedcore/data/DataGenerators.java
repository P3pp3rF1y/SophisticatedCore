package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;

import java.util.Optional;
import java.util.stream.Stream;

public class DataGenerators {
	private DataGenerators() {
	}

	public static void gatherData(GatherDataEvent.Client evt) {
		evt.createProvider(CoreFluidTagsProvider::new);
		evt.createReloadableRegistryObjects(new RegistrySetBuilder().add(RecipeProvider.asBootstrap(CoreRecipeProvider::new)));
		evt.createProvider(CoreModelProvider::new);
	}

	private static class CoreModelProvider extends SophisticatedModelProvider {
		public CoreModelProvider(PackOutput output) {
			super(output, SophisticatedCore.MOD_ID);
		}

		@Override
		protected Stream<? extends Holder<Block>> getKnownBlocks() {
			return Stream.empty();
		}

		@Override
		protected Stream<? extends Holder<Item>> getKnownItems() {
			return Stream.of(ModFluids.XP_BUCKET.get().builtInRegistryHolder());
		}

		@Override
		protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
			itemModels.itemModelOutput.accept(ModFluids.XP_BUCKET.get(),
					new DynamicFluidContainerModel.Unbaked(
							new DynamicFluidContainerModel.Textures(Optional.of(new Material(Identifier.withDefaultNamespace("item/bucket"))),
									Optional.of(new Material(Identifier.withDefaultNamespace("item/bucket"))),
									Optional.of(new Material(Identifier.fromNamespaceAndPath("neoforge", "item/mask/bucket_fluid"))), Optional.empty()),
							ModFluids.XP_STILL.get(), false, false, false));
		}
	}
}
