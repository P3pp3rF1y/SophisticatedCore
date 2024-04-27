package net.p3pp3rf1y.sophisticatedcore.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.Nullable;
import java.util.function.Function;

public class RecipeWrapperSerializer<T extends Recipe<?>, R extends Recipe<?> & IWrapperRecipe<T>> implements RecipeSerializer<R> {
	@Nullable
	private Codec<R> codec;
	private final Function<T, R> initialize;
	private final RecipeSerializer<T> recipeSerializer;

	public RecipeWrapperSerializer(Function<T, R> initialize, RecipeSerializer<T> recipeSerializer) {
		this.initialize = initialize;
		this.recipeSerializer = recipeSerializer;
	}

	@Override
	public Codec<R> codec() {
		if (this.codec == null) {
			this.codec = ((MapCodec.MapCodecCodec<T>) recipeSerializer.codec()).codec().xmap(initialize, IWrapperRecipe::getCompose).codec();
		}

		return this.codec;
	}

	@Override
	public R fromNetwork(FriendlyByteBuf buffer) {
		T compose = recipeSerializer.fromNetwork(buffer);
		return compose == null ? null : initialize.apply(compose);
	}

	@Override
	public void toNetwork(FriendlyByteBuf buffer, R recipe) {
		recipeSerializer.toNetwork(buffer, recipe.getCompose());
	}
}

