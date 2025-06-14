package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.template.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;

import java.util.List;
import java.util.function.Function;

public abstract class SophisticatedModelProvider extends ModelProvider {
	public SophisticatedModelProvider(PackOutput output, String modId) {
		super(output, modId);
	}

	protected void generateCubeBottomTopReuseTopOnBottom(BlockModelGenerators blockModels, Block block) {
		blockModels.createTrivialBlock(block,
				TexturedModel.createDefault(
						b -> new TextureMapping()
								.put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top"))
								.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side"))
								.put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(block, "_top")),
						ModelTemplates.CUBE_BOTTOM_TOP
				));
	}

	protected void generateCustomLoaderModels(BlockModelGenerators blockModels, Class<? extends Block> blockClass, String loaderName, Function<ResourceLocation, ItemModel.Unbaked> createItemModel) {
		BuiltInRegistries.BLOCK.entrySet().stream()
				.filter(entry -> entry.getKey().location().getNamespace().equals(modId)
						&& blockClass.isAssignableFrom(entry.getValue().getClass()))
				.forEach(entry -> {
					Block block = entry.getValue();
					TexturedModel.Provider provider = TexturedModel.createDefault(b -> new TextureMapping(),
							ExtendedModelTemplateBuilder.builder().customLoader(() -> createSimpleCustomLoaderBuilder(loaderName), loader -> {
							}).build()
					);
					ResourceLocation blockModelId = provider.create(block, blockModels.modelOutput);
					blockModels.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, blockModelId));
					blockModels.itemModelOutput.accept(block.asItem(), createItemModel.apply(blockModelId));
				});
	}

	protected void addItemClasses(List<Item> itemList, List<Class<? extends Item>> itemClasses) {
		BuiltInRegistries.ITEM.entrySet().stream()
				.filter(entry -> entry.getKey().location().getNamespace().equals(modId)
						&& itemClasses.stream().anyMatch(itemClass -> itemClass.isAssignableFrom(entry.getValue().getClass())))
				.forEach(entry -> itemList.add(entry.getValue()));
	}

	private CustomLoaderBuilder createSimpleCustomLoaderBuilder(String name) {
		return new CustomLoaderBuilder(ResourceLocation.fromNamespaceAndPath(modId, name), false) {
			@Override
			protected CustomLoaderBuilder copyInternal() {
				return createSimpleCustomLoaderBuilder(name);
			}
		};
	}
}
