package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;

public abstract class SophisticatedModelProvider extends ModelProvider {
	public SophisticatedModelProvider(PackOutput output, String modId) {
		super(output, modId);
	}

	protected void generateCubeBottomTopReuseTopOnBottom(BlockModelGenerators blockModels, Block block) {
		blockModels.createTrivialBlock(block,
				TexturedModel.createDefault(b -> new TextureMapping().put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top"))
						.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side"))
						.put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(block, "_top")), ModelTemplates.CUBE_BOTTOM_TOP));
	}

	protected void addItemClasses(List<Item> itemList, List<Class<? extends Item>> itemClasses) {
		BuiltInRegistries.ITEM.entrySet().stream()
				.filter(entry -> entry.getKey().location().getNamespace().equals(modId)
						&& itemClasses.stream().anyMatch(itemClass -> itemClass.isAssignableFrom(entry.getValue().getClass())))
				.forEach(entry -> itemList.add(entry.getValue()));
	}

}
