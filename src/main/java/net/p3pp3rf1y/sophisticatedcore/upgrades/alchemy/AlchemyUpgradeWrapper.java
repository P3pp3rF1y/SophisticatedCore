package net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.EntityMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.FilterItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AlchemyUpgradeWrapper extends UpgradeWrapperBase<AlchemyUpgradeWrapper, AlchemyUpgradeItem> implements ITickableUpgrade {
	private static final int CHECK_INTERVAL = 5;
	private static final int CHECK_RADIUS = 3;
	private static final String ALCHEMY_FILTER_ATTRIBUTES_TAG = "alchemyFilterAttributes";
	private static final String MATCH_ALL_EFFECTS_TAG = "matchAllEffects";
	private static final String MATCH_EFFECT_DURATION_TAG = "matchEffectDuration";
	private static final String MATCH_EFFECT_AMPLIFIER_TAG = "matchEffectAmplifier";
	private static final String ENTITY_MATCH_TAG = "entityMatch";
	private long nextCheckTime = 0;
	private boolean applying = false;
	private LivingEntity applyingToEntity = null;
	private int remainingApplyTime = 0;
	private ItemStack stackBeingAplied = ItemStack.EMPTY;
	private AlchemyItemDefinition defBeingApplied = null;
	@Nullable
	private ObservableFilterItemStackHandler filterHandler = null;

	protected AlchemyUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
	}

	public List<AlchemyFilterAttribute> getFilterAttributes() {
		return NBTHelper.getCollection(upgrade, "", ALCHEMY_FILTER_ATTRIBUTES_TAG, Tag.TAG_COMPOUND,
				tag -> Optional.of(AlchemyFilterAttribute.deserializeNBT((CompoundTag) tag)), ArrayList::new).orElseGet(ArrayList::new);
	}

	private List<AlchemyFilterAttribute> getEmptyAttributes() {
		List<AlchemyFilterAttribute> emptyAttributes = new ArrayList<>();
		for (int i = 0; i < upgradeItem.getFilterSlotCount(); i++) {
			emptyAttributes.add(new AlchemyFilterAttribute(ItemStack.EMPTY, AlchemyCondition.NEVER));
		}
		return emptyAttributes;
	}

	private void setFilter(int slot, ItemStack filter) {
		List<AlchemyFilterAttribute> attributes = new ArrayList<>(getFilterAttributes());
		AlchemyFilterAttribute attribute = attributes.get(slot);
		if (attribute.filter().isEmpty()) {
			AlchemyCondition defaultConditionForPotion = itemDefinitions.stream().filter(def -> def.filter.test(filter)).findFirst().map(def -> def.getDefaultCondition.apply(filter)).orElse(AlchemyCondition.NEVER);
			attribute = attribute.setConditionAndValue(defaultConditionForPotion, defaultConditionForPotion.defaultValue());
		} else if (filter.isEmpty()) {
			attribute = attribute.setConditionAndValue(AlchemyCondition.NEVER, -1);
		}
		attribute = attribute.setFilter(filter);
		attributes.set(slot, attribute);
		setUpgradeStackAlchemyAttributes(attributes);
		save();
	}

	public AlchemyCondition getCondition(int slot) {
		return getFilterAttributes().get(slot).condition();
	}

	public float getValue(int slot) {
		return getFilterAttributes().get(slot).value();
	}

	public void setConditionValue(int slot, AlchemyCondition condition, float value) {
		List<AlchemyFilterAttribute> attributes = new ArrayList<>(getFilterAttributes());
		attributes.set(slot, attributes.get(slot).setConditionAndValue(condition, value));
		setUpgradeStackAlchemyAttributes(attributes);
		save();
	}

	private void setUpgradeStackAlchemyAttributes(List<AlchemyFilterAttribute> attributes) {
		NBTHelper.setList(upgrade, "", ALCHEMY_FILTER_ATTRIBUTES_TAG, attributes, AlchemyFilterAttribute::serializeNBT);
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (level.isClientSide() || nextCheckTime > level.getGameTime()) {
			return;
		}

		if (remainingApplyTime > 0) {
			remainingApplyTime--;
			if (remainingApplyTime <= 0) {
				if (defBeingApplied.hasItemUseEffects()) {
					triggerItemUseEffects(level);
				}
				applying = false;
				ItemStack remainingStack = defBeingApplied.finishUsing.apply(stackBeingAplied, applyingToEntity);
				stackBeingAplied = ItemStack.EMPTY;
				storageWrapper.getInventoryForUpgradeProcessing().insertItem(remainingStack, false);
				nextCheckTime = level.getGameTime() + CHECK_INTERVAL;
			} else if (shouldTriggerItemUseEffects()) {
				triggerItemUseEffects(level);
			}
			return;
		}

		if (entity instanceof LivingEntity livingEntity) {
			applyTo(livingEntity);
		} else {
			level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(CHECK_RADIUS), this::entityMatches).forEach(this::applyTo);
		}
		if (!applying) {
			nextCheckTime = level.getGameTime() + CHECK_INTERVAL;
		}
	}

	private boolean entityMatches(LivingEntity livingEntity) {
		if (!livingEntity.isAlive()) {
			return false;
		}

		switch (getEntityMatch()) {
			case PLAYERS -> {
				return (livingEntity instanceof Player);
			}
			case ENTITIES -> {
				return !(livingEntity instanceof Player);
			}
			case PLAYERS_AND_ENTITIES -> {
				return true;
			}
		}
		return false;
	}

	public void triggerItemUseEffects(Level level) {
		if (stackBeingAplied.getUseAnimation() == UseAnim.DRINK) {
			level.playSound(null, applyingToEntity.getX(), applyingToEntity.getY(), applyingToEntity.getZ(), stackBeingAplied.getDrinkingSound(), applyingToEntity.getSoundSource(), 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
		}

		if (stackBeingAplied.getUseAnimation() == UseAnim.EAT) {
			level.playSound(null, applyingToEntity.getX(), applyingToEntity.getY(), applyingToEntity.getZ(), applyingToEntity.getEatingSound(stackBeingAplied), applyingToEntity.getSoundSource(), 0.5F + 0.5F * level.random.nextInt(2), (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);
		}
	}

	private boolean shouldTriggerItemUseEffects() {
		if (remainingApplyTime < 2) {
			return false;
		}
		int applyTimePassed = stackBeingAplied.getUseDuration() - remainingApplyTime;
		int effectDelay = (int) (stackBeingAplied.getUseDuration() * 0.21875F);
		boolean canStartTriggering = applyTimePassed > effectDelay;
		return canStartTriggering && remainingApplyTime % 4 == 0;
	}

	private void applyTo(LivingEntity livingEntity) {
		for (AlchemyFilterAttribute filterAttribute : getFilterAttributes()) {
			if (!filterAttribute.filter().isEmpty() && filterAttribute.condition().test(livingEntity, filterAttribute.value())) {
				itemDefinitions.stream().filter(def -> def.filter.test(filterAttribute.filter())).findFirst().ifPresent(def -> {
					if (def.canApply.test(livingEntity, filterAttribute.filter(), shouldMatchAllEffects(), shouldMatchEffectAmplifier())) {
						InventoryHelper.iterate(storageWrapper.getInventoryForUpgradeProcessing(), (slot, stack) -> {
							if (def.stackMatches.test(stack, filterAttribute.filter(), shouldMatchAllEffects(), shouldMatchEffectDuration(), shouldMatchEffectAmplifier())) {
								remainingApplyTime = def.startUsing.applyAsInt(stack, livingEntity);
								if (remainingApplyTime > 0) {
									applying = true;
									stackBeingAplied = stack.copyWithCount(1);
									defBeingApplied = def;
									applyingToEntity = livingEntity;
								}
								stack.shrink(1);
								storageWrapper.getInventoryForUpgradeProcessing().setStackInSlot(slot, stack);
							}
						}, () -> applying);
					}
				});
			}
		}
	}

	private static List<AlchemyItemDefinition> itemDefinitions = List.of(
			new AlchemyItemDefinition(stack -> stack.getItem() == Items.SPLASH_POTION, AlchemyUpgradeWrapper::getDefaultConditionForPotion,
					AlchemyUpgradeWrapper::shouldApplyPotionEffectsTo, AlchemyUpgradeWrapper::stackPotionEffectsMatch, (stack, livingEntity) -> {
				Level level = livingEntity.level();
				level.playSound(null, livingEntity.getX() + livingEntity.getBbWidth() / 2, livingEntity.getY(), livingEntity.getZ() + livingEntity.getBbWidth() / 2, SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
				ThrownPotion thrownPotion = new ThrownPotion(level, livingEntity.getX() + livingEntity.getBbWidth() / 2, livingEntity.getY() + livingEntity.getEyeHeight(), livingEntity.getZ() + livingEntity.getBbWidth() / 2);
				thrownPotion.setItem(stack);
				onHit(thrownPotion, new EntityHitResult(livingEntity, new Vec3(livingEntity.getX(), livingEntity.getY() + livingEntity.getEyeHeight(), livingEntity.getZ())));
				return 1;
			}, (stack, livingEntity) -> ItemStack.EMPTY, false),
			new AlchemyItemDefinition(stack -> stack.getItem() == Items.POTION, AlchemyUpgradeWrapper::getDefaultConditionForPotion,
					AlchemyUpgradeWrapper::shouldApplyPotionEffectsTo, AlchemyUpgradeWrapper::stackPotionEffectsMatch, (stack, livingEntity) -> stack.getUseDuration(),
					(stack, livingEntity) -> {
						ItemStack remainingItem = stack.getItem().finishUsingItem(stack, livingEntity.level(), livingEntity);
						if (livingEntity instanceof Player) {
							return remainingItem;
						}
						return new ItemStack(Items.GLASS_BOTTLE);
					}),
			new AlchemyItemDefinition(stack -> stack.getItem() == Items.MILK_BUCKET, stack -> AlchemyCondition.NEGATIVE_EFFECT,
					(le, potionStack, matchAllEffects, matchEffectAmplifier) -> true,
					(stack, filter, matchAllEffects, matchEffectDuration, matchEffectAmplifier) -> stack.getItem() == Items.MILK_BUCKET,
					(stack, livingEntity) -> stack.getUseDuration(), (stack, livingEntity) -> stack.getItem().finishUsingItem(stack, livingEntity.level(), livingEntity)),
			new AlchemyItemDefinition(stack -> stack.getItem() == Items.GOLDEN_APPLE, stack -> AlchemyCondition.ALWAYS, (le, potionStack, matchAllEffects, matchEffectAmplifier) -> {
				if (le instanceof ZombieVillager zombieVillager) {
					return !zombieVillager.isConverting() && zombieVillager.hasEffect(MobEffects.WEAKNESS);
				} else if (le instanceof Player) {
					return shouldApplyFoodEffectsTo(le, potionStack, matchAllEffects, matchEffectAmplifier);
				}
				return false;
			}, AlchemyUpgradeWrapper::foodStackPotionEffectsMatch, (stack, livingEntity) -> stack.getUseDuration(),
					(stack, livingEntity) -> {
						if (livingEntity instanceof ZombieVillager zombieVillager && zombieVillager.hasEffect(MobEffects.WEAKNESS)) {
							zombieVillager.startConverting(null, livingEntity.level().random.nextInt(2401) + 3600);
							return ItemStack.EMPTY;
						}

						return stack.getItem().finishUsingItem(stack, livingEntity.level(), livingEntity);
					}),
			new AlchemyItemDefinition(stack -> {
				FoodProperties foodProperties = stack.getFoodProperties(null);
				return foodProperties != null && !getEffects(foodProperties).isEmpty();
			}, stack -> AlchemyCondition.ALWAYS, (le, potionStack, matchAllEffects, matchEffectAmplifier) -> {
				if (le instanceof Player) {
					return shouldApplyFoodEffectsTo(le, potionStack, matchAllEffects, matchEffectAmplifier);
				}
				return false;
			}, AlchemyUpgradeWrapper::foodStackPotionEffectsMatch, (stack, livingEntity) -> stack.getUseDuration(), (stack, livingEntity) -> stack.getItem().finishUsingItem(stack, livingEntity.level(), livingEntity))
	);

	private static boolean foodStackPotionEffectsMatch(ItemStack stack, ItemStack filter, boolean matchAllEffects, boolean matchEffectDuration, boolean matchEffectAmplifier) {
		if (matchAllEffects && matchEffectDuration && matchEffectAmplifier) {
			return ItemHandlerHelper.canItemStacksStack(filter, stack);
		}

		FoodProperties foodProperties = stack.getFoodProperties(null);
		FoodProperties filterFoodProperties = filter.getFoodProperties(null);

		if (foodProperties == null || filterFoodProperties == null) {
			return false;
		}

		if (getEffects(foodProperties).isEmpty() || getEffects(filterFoodProperties).isEmpty()) {
			return false;
		}

		for (Pair<Supplier<MobEffectInstance>, Float> possibleEffect : getEffects(filterFoodProperties)) {
			if (!matchEffectIn(foodProperties, possibleEffect.getFirst().get(), matchEffectDuration, matchEffectAmplifier)) {
				return false;
			}
			if (!matchAllEffects) {
				return true;
			}
		}

		return true;
	}

	private static final Method ON_HIT = ObfuscationReflectionHelper.findMethod(ThrownPotion.class, "m_6532_", HitResult.class);

	private static void onHit(ThrownPotion thrownPotion, EntityHitResult entityHitResult) {
		try {
			ON_HIT.invoke(thrownPotion, entityHitResult);
		} catch (Exception e) {
			SophisticatedCore.LOGGER.error("Failed to invoke ThrownPotion::onHit method", e);
		}
	}

	private static final Field EFFECTS = ObfuscationReflectionHelper.findField(FoodProperties.class, "f_38728_");

	private static List<Pair<Supplier<MobEffectInstance>, Float>> getEffects(FoodProperties foodProperties) {
		try {
			return (List<Pair<Supplier<MobEffectInstance>, Float>>) EFFECTS.get(foodProperties);
		} catch (IllegalAccessException e) {
			SophisticatedCore.LOGGER.error("Failed to access FoodProperties::effects field", e);
			return List.of();
		}
	}

	private static boolean matchEffectIn(FoodProperties foodProperties, MobEffectInstance filterInstance, boolean matchEffectDuration, boolean matchEffectAmplifier) {
		for (Pair<Supplier<MobEffectInstance>, Float> effectInstance : getEffects(foodProperties)) {
			MobEffectInstance effect = effectInstance.getFirst().get();
			if (effect.getEffect() == filterInstance.getEffect()
					&& (!matchEffectDuration || effect.getDuration() == filterInstance.getDuration())
					&& (!matchEffectAmplifier || effect.getAmplifier() == filterInstance.getAmplifier())) {
				return true;
			}
		}
		return false;
	}

	private static boolean stackPotionEffectsMatch(ItemStack stack, ItemStack filter, boolean matchAllEffects, boolean matchEffectDuration, boolean matchEffectAmplifier) {
		if (matchAllEffects && matchEffectDuration && matchEffectAmplifier) {
			return ItemHandlerHelper.canItemStacksStack(filter, stack);
		}

		if (stack.getTag() == null || (!stack.getTag().contains("Potion") && !stack.getTag().contains("CustomPotionEffects"))) {
			return false;
		}

		List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
		List<MobEffectInstance> filterEffects = PotionUtils.getMobEffects(filter);

		if (effects.isEmpty() || filterEffects.isEmpty()) {
			return false;
		}

		for (MobEffectInstance filterEffectInstance : filterEffects) {
			if (!matchEffectIn(effects, filterEffectInstance, matchEffectDuration, matchEffectAmplifier)) {
				return false;
			}
			if (!matchAllEffects) {
				return true;
			}
		}

		return true;
	}

	private static boolean matchEffectIn(List<MobEffectInstance> potionEffectInstances, MobEffectInstance filterEffectInstance, boolean matchEffectDuration, boolean matchEffectAmplifier) {
		for (MobEffectInstance effectInstance : potionEffectInstances) {
			if (effectInstance.getEffect() == filterEffectInstance.getEffect()
					&& (!matchEffectDuration || effectInstance.getDuration() == filterEffectInstance.getDuration())
					&& (!matchEffectAmplifier || effectInstance.getAmplifier() == filterEffectInstance.getAmplifier())) {
				return true;
			}
		}
		return false;
	}

	private static AlchemyCondition getDefaultConditionForPotion(ItemStack potionStack) {
		List<MobEffectInstance> effects = PotionUtils.getMobEffects(potionStack);
		Iterator<MobEffectInstance> it = effects.iterator();
		if (!it.hasNext()) {
			return AlchemyCondition.NEVER;
		}
		MobEffect effect = it.next().getEffect();
		if (effect == MobEffects.WATER_BREATHING) {
			return AlchemyCondition.UNDER_WATER;
		} else if (effect == MobEffects.HEAL || effect == MobEffects.REGENERATION) {
			return AlchemyCondition.HURT;
		} else if (effect == MobEffects.FIRE_RESISTANCE) {
			return AlchemyCondition.ON_FIRE;
		} else if (effect == MobEffects.MOVEMENT_SPEED) {
			return AlchemyCondition.SPRINTING;
		} else if (effect == MobEffects.DIG_SPEED) {
			return AlchemyCondition.MINING;
		} else if (effect == MobEffects.SLOW_FALLING) {
			return AlchemyCondition.FALLING;
		}
		return AlchemyCondition.ALWAYS;
	}

	private static boolean shouldApplyPotionEffectsTo(LivingEntity le, ItemStack potionStack, boolean matchAllEffects, boolean matchEffectAmplifier) {
		List<MobEffectInstance> effects = PotionUtils.getMobEffects(potionStack);
		if (effects.isEmpty()) {
			return false;
		}
		for (MobEffectInstance effectInstance : effects) {
			if (matchAllEffects) {
				if (!effectPresent(le, effectInstance.getEffect(), matchEffectAmplifier, effectInstance.getAmplifier())) {
					return true;
				}
			} else {
				if (effectPresent(le, effectInstance.getEffect(), matchEffectAmplifier, effectInstance.getAmplifier())) {
					return false;
				}
			}
		}
		return !matchAllEffects;
	}

	private static boolean effectPresent(LivingEntity le, MobEffect effect, boolean matchEffectAmplifier, int amplifier) {
		MobEffectInstance leEffectInstance = le.getEffect(effect);
		//checking for duration and amplifier greater than passed in because otherwise applying the passed in ones to an entity would do nothing
		return leEffectInstance != null && (!matchEffectAmplifier || leEffectInstance.getAmplifier() >= amplifier);
	}

	private static boolean shouldApplyFoodEffectsTo(LivingEntity le, ItemStack potionStack, boolean matchAllEffects, boolean matchEffectAmplifier) {
		FoodProperties foodProperties = potionStack.getFoodProperties(le);
		if (foodProperties == null) {
			return false;
		}
		for (Pair<java.util.function.Supplier<MobEffectInstance>, Float> possibleEffect : getEffects(foodProperties)) {
			MobEffectInstance effectInstance = possibleEffect.getFirst().get();
			if (matchAllEffects) {
				if (!effectPresent(le, effectInstance.getEffect(), matchEffectAmplifier, effectInstance.getAmplifier())) {
					return true;
				}
			} else {
				if (effectPresent(le, effectInstance.getEffect(), matchEffectAmplifier, effectInstance.getAmplifier())) {
					return false;
				}
			}
		}
		return !matchAllEffects;
	}

	public boolean isValidAlchemyItem(ItemStack stack) {
		return itemDefinitions.stream().anyMatch(def -> def.filter.test(stack)) && !InventoryHelper.hasItem(getFilterHandler(), s -> ItemHandlerHelper.canItemStacksStack(s, stack));
	}

	public ObservableFilterItemStackHandler getFilterHandler() {
		List<AlchemyFilterAttribute> filterAttributes = getFilterAttributes();
		if (filterHandler == null) {
			if (filterAttributes.size() < upgradeItem.getFilterSlotCount()) {
				filterAttributes = new ArrayList<>(getFilterAttributes());
				for (int i = filterAttributes.size(); i < upgradeItem.getFilterSlotCount(); i++) {
					filterAttributes.add(new AlchemyFilterAttribute(ItemStack.EMPTY, AlchemyCondition.NEVER));
				}
				setUpgradeStackAlchemyAttributes(filterAttributes);
			}
			filterHandler = new ObservableFilterItemStackHandler(filterAttributes.size());
			filterHandler.initFilters(filterAttributes);
		}

		return filterHandler;
	}

	public void setMatchAllEffects(boolean matchAllEffects) {
		upgrade.getOrCreateTag().putBoolean(MATCH_ALL_EFFECTS_TAG, matchAllEffects);
		save();
	}

	public boolean shouldMatchAllEffects() {
		return NBTHelper.getBoolean(upgrade, "", MATCH_ALL_EFFECTS_TAG).orElse(true);
	}

	public boolean shouldMatchEffectDuration() {
		return NBTHelper.getBoolean(upgrade, "", MATCH_EFFECT_DURATION_TAG).orElse(true);
	}

	public void setMatchEffectDuration(boolean matchEffectDuration) {
		upgrade.getOrCreateTag().putBoolean(MATCH_EFFECT_DURATION_TAG, matchEffectDuration);
		save();
	}

	public boolean shouldMatchEffectAmplifier() {
		return NBTHelper.getBoolean(upgrade, "", MATCH_EFFECT_AMPLIFIER_TAG).orElse(true);
	}

	public void setMatchEffectAmplifier(boolean matchEffectAmplifier) {
		upgrade.getOrCreateTag().putBoolean(MATCH_EFFECT_AMPLIFIER_TAG, matchEffectAmplifier);
		save();
	}

	public EntityMatch getEntityMatch() {
		return NBTHelper.getEnumConstant(upgrade, ENTITY_MATCH_TAG, EntityMatch::fromName).orElse(EntityMatch.PLAYERS_AND_ENTITIES);
	}

	public void setEntityMatch(EntityMatch entityMatch) {
		NBTHelper.setEnumConstant(upgrade, ENTITY_MATCH_TAG, entityMatch);
		save();
	}

	public record AlchemyItemDefinition(Predicate<ItemStack> filter,
										Function<ItemStack, AlchemyCondition> getDefaultCondition,
										AlchemyItemEntityMatcher canApply, AlchemyItemStackMatcher stackMatches,
										StartUsing startUsing, FinishUsing finishUsing, boolean hasItemUseEffects) {
		public AlchemyItemDefinition(Predicate<ItemStack> filter,
									 Function<ItemStack, AlchemyCondition> getDefaultCondition,
									 AlchemyItemEntityMatcher canApply, AlchemyItemStackMatcher stackMatches,
									 StartUsing startUsing, FinishUsing finishUsing) {
			this(filter, getDefaultCondition, canApply, stackMatches, startUsing, finishUsing, true);
		}
	}

	private interface AlchemyItemEntityMatcher {
		boolean test(LivingEntity entity, ItemStack stack, boolean matchAllEffects, boolean matchEffectAmplifier);
	}

	private interface AlchemyItemStackMatcher {
		boolean test(ItemStack stack, ItemStack filter, boolean matchAllEffects, boolean matchEffectDuration, boolean matchEffectAmplifier);
	}

	private interface StartUsing {
		int applyAsInt(ItemStack stack, LivingEntity livingEntity);
	}

	private interface FinishUsing {
		ItemStack apply(ItemStack stack, LivingEntity livingEntity);
	}


	public class ObservableFilterItemStackHandler extends FilterItemStackHandler {
		public ObservableFilterItemStackHandler(int filterSlotCount) {
			super(filterSlotCount);
		}

		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			setFilter(slot, stacks.get(slot));
			save();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return stack.isEmpty() || (doesNotContain(stack) && isValidAlchemyItem(stack));
		}

		private boolean doesNotContain(ItemStack stack) {
			return !InventoryHelper.hasItem(this, s -> ItemHandlerHelper.canItemStacksStack(s, stack));
		}

		public void initFilters(List<AlchemyFilterAttribute> filterAttributes) {
			for (int slot = 0; slot < filterAttributes.size(); slot++) {
				setStackInSlot(slot, filterAttributes.get(slot).filter().copy());
			}
			onLoad();
		}
	}
}
