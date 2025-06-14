package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.subtypes;

import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.subtypes.PropertyBasedSubtypeInterpreterWrapper;

import javax.annotation.Nullable;

public class JeiSubtypeInterpreter extends PropertyBasedSubtypeInterpreterWrapper implements ISubtypeInterpreter<ItemStack> {
	public static JeiSubtypeInterpreter of(PropertyBasedSubtypeInterpreter wrapped) {
		return new JeiSubtypeInterpreter(wrapped);
	}

	private JeiSubtypeInterpreter(PropertyBasedSubtypeInterpreter wrapped) {
		super(wrapped);
	}

	@Override
	public final @Nullable Object getSubtypeData(ItemStack ingredient, UidContext context) {
		return getComparableData(ingredient);
	}
}
