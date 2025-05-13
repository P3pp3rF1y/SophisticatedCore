package net.p3pp3rf1y.sophisticatedcore.compat.reliquary;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeWrapper;
import reliquary.entities.potion.ThrownPotion;
import reliquary.init.ModItems;

import java.lang.reflect.Method;

public class ReliquaryCompat implements ICompat {
	@Override
	public void setup() {
		AlchemyUpgradeWrapper.addItemDefinition(new AlchemyUpgradeWrapper.AlchemyItemDefinition(stack -> stack.getItem() == ModItems.POTION.get(), AlchemyUpgradeWrapper::getDefaultConditionForPotion,
				AlchemyUpgradeWrapper::shouldApplyPotionEffectsTo, AlchemyUpgradeWrapper::stackPotionEffectsMatch, ItemStack::getUseDuration,
				(stack, livingEntity) -> {
					ItemStack remainingItem = stack.getItem().finishUsingItem(stack, livingEntity.level(), livingEntity);
					if (livingEntity instanceof Player) {
						return remainingItem;
					}
					return new ItemStack(ModItems.EMPTY_POTION_VIAL.get());
				}));
		AlchemyUpgradeWrapper.addItemDefinition(new AlchemyUpgradeWrapper.AlchemyItemDefinition(stack -> stack.getItem() == ModItems.SPLASH_POTION.get(), AlchemyUpgradeWrapper::getDefaultConditionForPotion,
				AlchemyUpgradeWrapper::shouldApplyPotionEffectsTo, AlchemyUpgradeWrapper::stackPotionEffectsMatch,
				(stack, livingEntity) -> {
					Level level = livingEntity.level();
					level.playSound(null, livingEntity.getX() + livingEntity.getBbWidth() / 2, livingEntity.getY(), livingEntity.getZ() + livingEntity.getBbWidth() / 2, SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
					ThrownPotion thrownPotion = new ThrownPotion(level, new Vec3(livingEntity.getX() + livingEntity.getBbWidth() / 2, livingEntity.getY() + livingEntity.getEyeHeight(), livingEntity.getZ() + livingEntity.getBbWidth() / 2), stack.copy());
					level.addFreshEntity(thrownPotion);
					onHit(thrownPotion, new EntityHitResult(livingEntity, new Vec3(livingEntity.getX(), livingEntity.getY() + livingEntity.getEyeHeight(), livingEntity.getZ())));
					return 1;
				}, (stack, livingEntity) -> ItemStack.EMPTY, false)
		);
	}

	private static final Method ON_HIT = ObfuscationReflectionHelper.findMethod(ThrownPotion.class, "onHit", HitResult.class);

	private static void onHit(ThrownPotion thrownPotion, HitResult hitResult) {
		try {
			ON_HIT.invoke(thrownPotion, hitResult);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
