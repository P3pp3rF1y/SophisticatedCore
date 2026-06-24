package net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public enum AlchemyCondition implements StringRepresentable {
	NEVER(le -> false), ALWAYS(le -> true), UNDER_WATER(Entity::isUnderWater), ON_FIRE(Entity::isOnFire), FALLING(le -> le.fallDistance > 2), MINING(
			le -> le instanceof ServerPlayer serverPlayer && serverPlayer.gameMode.isDestroyingBlock), SPRINTING(Entity::isSprinting), HURT(
					(le, v) -> le.getHealth() > 0 && le.getHealth() < le.getMaxHealth() && le.getHealth() / le.getMaxHealth() < v, 0.75f), NEGATIVE_EFFECT(
							(le, v) -> le.getActiveEffects().stream().anyMatch(effect -> effect.getEffect().getCategory() == MobEffectCategory.HARMFUL));

	private final BiPredicate<LivingEntity, Float> predicate;
	private final float defaultValue;

	AlchemyCondition(BiPredicate<LivingEntity, Float> predicate) {
		this(predicate, -1);
	}

	AlchemyCondition(BiPredicate<LivingEntity, Float> predicate, float defaultValue) {
		this.predicate = predicate;
		this.defaultValue = defaultValue;
	}

	AlchemyCondition(Predicate<LivingEntity> predicate) {
		this.predicate = (le, v) -> predicate.test(le);
		this.defaultValue = -1;
	}

	public boolean test(LivingEntity livingEntity, float value) {
		return predicate.test(livingEntity, value);
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	private static final Map<String, AlchemyCondition> NAME_VALUES;
	private static final AlchemyCondition[] VALUES;

	static {
		ImmutableMap.Builder<String, AlchemyCondition> builder = new ImmutableMap.Builder<>();
		for (AlchemyCondition value : AlchemyCondition.values()) {
			builder.put(value.getSerializedName(), value);
		}
		NAME_VALUES = builder.build();
		VALUES = values();
	}

	public AlchemyCondition next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	public static AlchemyCondition fromName(String name) {
		return NAME_VALUES.getOrDefault(name, NEVER);
	}

	public float defaultValue() {
		return defaultValue;
	}
}
