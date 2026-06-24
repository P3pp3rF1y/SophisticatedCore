package net.p3pp3rf1y.sophisticatedcore.compat.reliquary;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeWrapper;
import reliquary.entity.potion.ThrownXRPotionEntity;
import reliquary.init.ModItems;
import reliquary.util.potions.XRPotionHelper;

import java.lang.reflect.Method;
import java.util.List;

public class ReliquaryCompat implements ICompat {
	@Override
	public void setup() {
		AlchemyUpgradeWrapper.addItemDefinition(new AlchemyUpgradeWrapper.AlchemyItemDefinition(stack -> stack.getItem() == ModItems.POTION.get(),
				potionStack -> AlchemyUpgradeWrapper.getDefaultConditionForPotionEffects(getPotionEffects(potionStack)),
				(le, potionStack, matchAllEffects, matchEffectAmplifier) -> AlchemyUpgradeWrapper.shouldApplyPotionEffectsTo(le, getPotionEffects(potionStack),
						matchAllEffects, matchEffectAmplifier),
				(stack, filter, matchAllEffects1, matchEffectDuration1, matchEffectAmplifier1) -> AlchemyUpgradeWrapper
						.potionEffectsMatch(getPotionEffects(stack), getPotionEffects(filter), matchAllEffects1, matchEffectDuration1, matchEffectAmplifier1),
				(stack, livingEntity) -> stack.getUseDuration(), (stack, livingEntity) -> {
					ItemStack remainingItem = stack.getItem().finishUsingItem(stack, livingEntity.level(), livingEntity);
					if (livingEntity instanceof Player) {
						return remainingItem;
					}
					return new ItemStack(ModItems.EMPTY_POTION_VIAL.get());
				}));
		AlchemyUpgradeWrapper.addItemDefinition(new AlchemyUpgradeWrapper.AlchemyItemDefinition(stack -> stack.getItem() == ModItems.SPLASH_POTION.get(),
				potionStack -> AlchemyUpgradeWrapper.getDefaultConditionForPotionEffects(getPotionEffects(potionStack)),
				(le, potionStack, matchAllEffects1, matchEffectAmplifier1) -> AlchemyUpgradeWrapper.shouldApplyPotionEffectsTo(le,
						getPotionEffects(potionStack), matchAllEffects1, matchEffectAmplifier1),
				(stack, filter, matchAllEffects, matchEffectDuration, matchEffectAmplifier) -> AlchemyUpgradeWrapper.potionEffectsMatch(getPotionEffects(stack),
						getPotionEffects(filter), matchAllEffects, matchEffectDuration, matchEffectAmplifier),
				(stack, livingEntity) -> {
					Level level = livingEntity.level();
					level.playSound(null, livingEntity.getX() + livingEntity.getBbWidth() / 2, livingEntity.getY(),
							livingEntity.getZ() + livingEntity.getBbWidth() / 2, SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F,
							0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
					ThrownXRPotionEntity thrownPotion = new ThrownXRPotionEntity(level, livingEntity.getX() + livingEntity.getBbWidth() / 2,
							livingEntity.getY() + livingEntity.getEyeHeight(), livingEntity.getZ() + livingEntity.getBbWidth() / 2, stack.copy());
					level.addFreshEntity(thrownPotion);
					onHit(thrownPotion, new EntityHitResult(livingEntity,
							new Vec3(livingEntity.getX(), livingEntity.getY() + livingEntity.getEyeHeight(), livingEntity.getZ())));
					return 1;
				}, (stack, livingEntity) -> ItemStack.EMPTY, false));
	}

	private static final Method ON_HIT = ObfuscationReflectionHelper.findMethod(ThrownXRPotionEntity.class, "m_6532_", HitResult.class);

	private static void onHit(ThrownXRPotionEntity thrownPotion, HitResult hitResult) {
		try {
			ON_HIT.invoke(thrownPotion, hitResult);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private List<MobEffectInstance> getPotionEffects(ItemStack stack) {
		return XRPotionHelper.getPotionEffectsFromStack(stack);
	}
}
