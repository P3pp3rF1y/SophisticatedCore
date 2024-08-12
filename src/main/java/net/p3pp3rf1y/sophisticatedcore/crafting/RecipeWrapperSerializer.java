package net.p3pp3rf1y.sophisticatedcore.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.Nullable;
import java.util.function.Function;

public class RecipeWrapperSerializer<T extends Recipe<?>, R extends Recipe<?> & IWrapperRecipe<T>> implements RecipeSerializer<R> {
	@Nullable
	private MapCodec<R> codec;
	@Nullable
	private StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;
	private final Function<T, R> initialize;
	private final RecipeSerializer<T> recipeSerializer;

	public RecipeWrapperSerializer(Function<T, R> initialize, RecipeSerializer<T> recipeSerializer) {
		this.initialize = initialize;
		this.recipeSerializer = recipeSerializer;
	}

	@Override
	public MapCodec<R> codec() {
		if (this.codec == null) {
			this.codec = recipeSerializer.codec().xmap(initialize, IWrapperRecipe::getCompose);
		}

		return this.codec;
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
		if (this.streamCodec == null) {
			this.streamCodec = new StreamCodec<>() {
				@Override
				public R decode(RegistryFriendlyByteBuf buffer) {
					T compose = recipeSerializer.streamCodec().decode(buffer);
					return compose == null ? null : initialize.apply(compose);
				}

				@Override
				public void encode(RegistryFriendlyByteBuf pBuffer, R pValue) {
					recipeSerializer.streamCodec().encode(pBuffer, pValue.getCompose());
				}
			};
		}

		return this.streamCodec;
	}
}

