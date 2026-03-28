package net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.network.EmitConsumableClientParticlesAndSoundsPayload;
import net.p3pp3rf1y.sophisticatedcore.upgrades.EntityMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.FilterItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class AlchemyUpgradeWrapper extends UpgradeWrapperBase<AlchemyUpgradeWrapper, AlchemyUpgradeItem> implements ITickableUpgrade {
	private static final int CHECK_INTERVAL = 5;
	private static final int CHECK_RADIUS = 3;
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
		return upgrade.getOrDefault(ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES, getEmptyAttributes());
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
		upgrade.set(ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES, attributes instanceof ImmutableList ? attributes : ImmutableList.copyOf(attributes));
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
					triggerItemUseEffects();
				}
				applying = false;
				ItemStack remainingStack = defBeingApplied.finishUsing.apply(stackBeingAplied, applyingToEntity);
				stackBeingAplied = ItemStack.EMPTY;
				applyingToEntity = null;
				defBeingApplied = null;
				InventoryHelper.insert(storageWrapper.getInventoryForUpgradeProcessing(), ItemResource.of(remainingStack), remainingStack.getCount());
				nextCheckTime = level.getGameTime() + CHECK_INTERVAL;
			} else if (shouldTriggerItemUseEffects()) {
				triggerItemUseEffects();
			}
			return;
		}

		if (entity instanceof LivingEntity livingEntity) {
			applyTo(livingEntity);
		} else {
			List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(CHECK_RADIUS), this::entityMatches);
			for (LivingEntity livingEntity : entities) {
				applyTo(livingEntity);
				if (applying) {
					break;
				}
			}
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

	public void triggerItemUseEffects() {
		Consumable consumable = stackBeingAplied.get(DataComponents.CONSUMABLE);
		if (consumable == null) {
			return;
		}
		consumable.emitParticlesAndSounds(applyingToEntity.getRandom(), applyingToEntity, stackBeingAplied, 5);
		if (applyingToEntity instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, new EmitConsumableClientParticlesAndSoundsPayload(stackBeingAplied));
		}
	}

	private boolean shouldTriggerItemUseEffects() {
		if (remainingApplyTime < 2) {
			return false;
		}
		int applyTimePassed = stackBeingAplied.getUseDuration(applyingToEntity) - remainingApplyTime;
		int effectDelay = (int) (stackBeingAplied.getUseDuration(applyingToEntity) * 0.21875F);
		boolean canStartTriggering = applyTimePassed > effectDelay;
		return canStartTriggering && remainingApplyTime % 4 == 0;
	}

	private void applyTo(LivingEntity livingEntity) {
		for (AlchemyFilterAttribute filterAttribute : getFilterAttributes()) {
			if (!filterAttribute.filter().isEmpty() && filterAttribute.condition().test(livingEntity, filterAttribute.value())) {
				itemDefinitions.stream().filter(def -> def.filter.test(filterAttribute.filter())).findFirst().ifPresent(def -> {
					if (def.canApply.test(livingEntity, filterAttribute.filter(), shouldMatchAllEffects(), shouldMatchEffectAmplifier())) {
						InventoryHelper.iterate(storageWrapper.getInventoryForUpgradeProcessing(), (slot, stack) -> {
							if (def.filter().test(stack) && def.stackMatches.test(stack, filterAttribute.filter(), shouldMatchAllEffects(), shouldMatchEffectDuration(), shouldMatchEffectAmplifier())) {
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

	private static List<AlchemyItemDefinition> itemDefinitions = new ArrayList<>();

	public static void addItemDefinition(AlchemyItemDefinition itemDefinition) {
		itemDefinitions.add(itemDefinition);
	}

	static {
		addItemDefinition(new AlchemyItemDefinition(stack -> stack.getItem() == Items.OMINOUS_BOTTLE, stack -> AlchemyCondition.ALWAYS,
				(le, potionStack, matchAllEffects, matchEffectAmplifier) -> !le.hasEffect(MobEffects.BAD_OMEN),
				(stack, filter, matchAllEffects, matchEffectDuration, matchEffectAmplifier) -> stack.getItem() == Items.OMINOUS_BOTTLE,
				ItemStack::getUseDuration,
				(stack, livingEntity) -> stack.finishUsingItem(livingEntity.level(), livingEntity)
		));
		addItemDefinition(new AlchemyItemDefinition(stack -> stack.getItem() == Items.SPLASH_POTION, AlchemyUpgradeWrapper::getDefaultConditionForPotion,
				AlchemyUpgradeWrapper::shouldApplyPotionEffectsTo, AlchemyUpgradeWrapper::stackPotionEffectsMatch,
				(stack, livingEntity) -> {
					Level level = livingEntity.level();
					level.playSound(null, livingEntity.getX() + livingEntity.getBbWidth() / 2, livingEntity.getY(), livingEntity.getZ() + livingEntity.getBbWidth() / 2, SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
					ThrownSplashPotion thrownPotion = new ThrownSplashPotion(level, livingEntity.getX() + livingEntity.getBbWidth() / 2, livingEntity.getY() + livingEntity.getEyeHeight(), livingEntity.getZ() + livingEntity.getBbWidth() / 2, stack);
					onHit(thrownPotion, new EntityHitResult(livingEntity, new Vec3(livingEntity.getX(), livingEntity.getY() + livingEntity.getEyeHeight(), livingEntity.getZ())));
					return 1;
				}, (stack, livingEntity) -> ItemStack.EMPTY, false));
		addItemDefinition(new AlchemyItemDefinition(stack -> stack.getItem() == Items.POTION, AlchemyUpgradeWrapper::getDefaultConditionForPotion,
				AlchemyUpgradeWrapper::shouldApplyPotionEffectsTo, AlchemyUpgradeWrapper::stackPotionEffectsMatch, ItemStack::getUseDuration,
				(stack, livingEntity) -> {
					ItemStack remainingItem = stack.finishUsingItem(livingEntity.level(), livingEntity);
					if (livingEntity instanceof Player) {
						return remainingItem;
					}
					return new ItemStack(Items.GLASS_BOTTLE);
				}));
		addItemDefinition(new AlchemyItemDefinition(stack -> stack.getItem() == Items.GOLDEN_APPLE, stack -> AlchemyCondition.ALWAYS,
				(le, potionStack, matchAllEffects, matchEffectAmplifier) -> {
					if (le instanceof ZombieVillager zombieVillager) {
						return !zombieVillager.isConverting() && zombieVillager.hasEffect(MobEffects.WEAKNESS);
					} else if (le instanceof Player) {
						return shouldApplyConsumableEffectsTo(le, potionStack, matchAllEffects, matchEffectAmplifier);
					}
					return false;
				},
				(stack, filter, matchAllEffects, matchEffectDuration, matchEffectAmplifier) -> ItemStack.isSameItemSameComponents(filter, stack),
				ItemStack::getUseDuration,
				(stack, livingEntity) -> {
					if (livingEntity instanceof ZombieVillager zombieVillager && zombieVillager.hasEffect(MobEffects.WEAKNESS)) {
					zombieVillager.startConverting(null, livingEntity.level().getRandom().nextInt(2401) + 3600);
						return ItemStack.EMPTY;
					}

					return stack.finishUsingItem(livingEntity.level(), livingEntity);
				}));
		addItemDefinition(new AlchemyItemDefinition(
				AlchemyUpgradeWrapper::isEffectAffectingConsumable,
				AlchemyUpgradeWrapper::getDefaultConditionForConsumable,
				(le, potionStack, matchAllEffects, matchEffectAmplifier) -> {
					if (le instanceof Player) {
						return shouldApplyConsumableEffectsTo(le, potionStack, matchAllEffects, matchEffectAmplifier);
					}
					return false;
				},
				(stack, filter, matchAllEffects, matchEffectDuration, matchEffectAmplifier) -> ItemStack.isSameItemSameComponents(filter, stack),
				ItemStack::getUseDuration, (stack, livingEntity) -> stack.finishUsingItem(livingEntity.level(), livingEntity))
		);
	}

	private static AlchemyCondition getDefaultConditionForConsumable(ItemStack stack) {
		Consumable consumable = stack.get(DataComponents.CONSUMABLE);
		if (consumable == null) {
			return AlchemyCondition.NEVER;
		}

		List<ConsumeEffect> consumeEffects = consumable.onConsumeEffects();
		for (ConsumeEffect consumeEffect : consumeEffects) {
			if (consumeEffect instanceof ClearAllStatusEffectsConsumeEffect) {
				return AlchemyCondition.NEGATIVE_EFFECT;
			} else if (consumeEffect instanceof RemoveStatusEffectsConsumeEffect) {
				return AlchemyCondition.NEGATIVE_EFFECT;
			} else if (consumeEffect instanceof ApplyStatusEffectsConsumeEffect applyStatusEffectsConsumeEffect) {
				List<MobEffectInstance> effects = applyStatusEffectsConsumeEffect.effects();
				if (effects.isEmpty()) {
					return AlchemyCondition.NEVER;
				}
				return getDefaultConditionForEffect(effects.getFirst().getEffect());
			}
		}

		return AlchemyCondition.NEVER;
	}

	private static boolean isEffectAffectingConsumable(ItemStack stack) {
		Consumable consumable = stack.get(DataComponents.CONSUMABLE);
		if (consumable == null) {
			return false;
		}

		List<ConsumeEffect> consumeEffects = consumable.onConsumeEffects();
		for (ConsumeEffect consumeEffect : consumeEffects) {
			if (consumeEffect instanceof ClearAllStatusEffectsConsumeEffect || consumeEffect instanceof RemoveStatusEffectsConsumeEffect || consumeEffect instanceof ApplyStatusEffectsConsumeEffect) {
				return true;
			}
		}

		return false;
	}

	private static final Method ON_HIT = ObfuscationReflectionHelper.findMethod(AbstractThrownPotion.class, "onHit", HitResult.class);

	private static void onHit(AbstractThrownPotion thrownPotion, EntityHitResult entityHitResult) {
		try {
			ON_HIT.invoke(thrownPotion, entityHitResult);
		} catch (Exception e) {
			SophisticatedCore.LOGGER.error("Failed to invoke ThrownPotion::onHit method", e);
		}
	}

	public static boolean stackPotionEffectsMatch(ItemStack stack, ItemStack filter, boolean matchAllEffects, boolean matchEffectDuration, boolean matchEffectAmplifier) {
		if (matchAllEffects && matchEffectDuration && matchEffectAmplifier) {
			return ItemStack.isSameItemSameComponents(filter, stack);
		}

		PotionContents potioncontents = stack.get(DataComponents.POTION_CONTENTS);
		PotionContents filterPotionContents = filter.get(DataComponents.POTION_CONTENTS);

		if (potioncontents == null || filterPotionContents == null) {
			return false;
		}

		if (!potioncontents.hasEffects() || !filterPotionContents.hasEffects()) {
			return false;
		}

		for (MobEffectInstance filterEffectInstance : filterPotionContents.getAllEffects()) {
			if (!matchEffectIn(potioncontents, filterEffectInstance, matchEffectDuration, matchEffectAmplifier)) {
				return false;
			}
			if (!matchAllEffects) {
				return true;
			}
		}

		return true;
	}

	private static boolean matchEffectIn(PotionContents potionContents, MobEffectInstance filterEffectInstance, boolean matchEffectDuration, boolean matchEffectAmplifier) {
		for (MobEffectInstance effectInstance : potionContents.getAllEffects()) {
			Holder<MobEffect> effect = effectInstance.getEffect();
			if (effect == filterEffectInstance.getEffect()
					&& (!matchEffectDuration || effectInstance.getDuration() == filterEffectInstance.getDuration())
					&& (!matchEffectAmplifier || effectInstance.getAmplifier() == filterEffectInstance.getAmplifier())) {
				return true;
			}
		}
		return false;
	}

	public static AlchemyCondition getDefaultConditionForPotion(ItemStack potionStack) {
		PotionContents potioncontents = potionStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		Iterator<MobEffectInstance> it = potioncontents.getAllEffects().iterator();
		if (!it.hasNext()) {
			return AlchemyCondition.NEVER;
		}
		Holder<MobEffect> effect = it.next().getEffect();
		return getDefaultConditionForEffect(effect);
	}

	private static AlchemyCondition getDefaultConditionForEffect(Holder<MobEffect> effect) {
		if (effect == MobEffects.WATER_BREATHING) {
			return AlchemyCondition.UNDER_WATER;
		} else if (effect == MobEffects.INSTANT_HEALTH || effect == MobEffects.REGENERATION) {
			return AlchemyCondition.HURT;
		} else if (effect == MobEffects.FIRE_RESISTANCE) {
			return AlchemyCondition.ON_FIRE;
		} else if (effect == MobEffects.SPEED) {
			return AlchemyCondition.SPRINTING;
		} else if (effect == MobEffects.HASTE) {
			return AlchemyCondition.MINING;
		} else if (effect == MobEffects.SLOW_FALLING) {
			return AlchemyCondition.FALLING;
		}
		return AlchemyCondition.ALWAYS;
	}

	public static boolean shouldApplyPotionEffectsTo(LivingEntity le, ItemStack potionStack, boolean matchAllEffects, boolean matchEffectAmplifier) {
		PotionContents potioncontents = potionStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		if (!potioncontents.hasEffects()) {
			return false;
		}
		for (MobEffectInstance effectInstance : potioncontents.getAllEffects()) {
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

	private static boolean effectPresent(LivingEntity le, Holder<MobEffect> effect, boolean matchEffectAmplifier, int amplifier) {
		MobEffectInstance leEffectInstance = le.getEffect(effect);
		//checking for duration and amplifier greater than passed in because otherwise applying the passed in ones to an entity would do nothing
		return leEffectInstance != null && (!matchEffectAmplifier || leEffectInstance.getAmplifier() >= amplifier);
	}

	private static boolean shouldApplyConsumableEffectsTo(LivingEntity le, ItemStack potionStack, boolean matchAllEffects, boolean matchEffectAmplifier) {
		Consumable consumable = potionStack.get(DataComponents.CONSUMABLE);
		if (consumable == null) {
			return false;
		}
		for (ConsumeEffect consumeEffect : consumable.onConsumeEffects()) {
			if (consumeEffect instanceof ApplyStatusEffectsConsumeEffect applyStatusEffects) {
				return areEffectsMissing(le, matchAllEffects, matchEffectAmplifier, applyStatusEffects.effects());
			} else if (consumeEffect instanceof ClearAllStatusEffectsConsumeEffect) {
				return true; //always apply these effects
			} else if (consumeEffect instanceof RemoveStatusEffectsConsumeEffect removeStatusEffects) {
				return areEffectsPresent(le, matchAllEffects, matchEffectAmplifier, removeStatusEffects.effects());
			}
		}

		return false;
	}

	private static boolean areEffectsMissing(LivingEntity le, boolean matchAllEffects, boolean matchEffectAmplifier, List<MobEffectInstance> effects) {
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

	private static boolean areEffectsPresent(LivingEntity le, boolean matchAllEffects, boolean matchEffectAmplifier, HolderSet<MobEffect> effects) {
		for (Holder<MobEffect> effect : effects) {
			if (le.hasEffect(effect)) {
				if (!matchAllEffects) {
					return true;
				}
			} else if (matchAllEffects) {
				return false;
			}
		}
		return matchAllEffects;
	}

	public boolean isValidAlchemyItem(ItemStack stack) {
		return itemDefinitions.stream().anyMatch(def -> def.filter.test(stack)) && !InventoryHelper.hasItem(getFilterHandler(), s -> s.matches(stack));
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
		upgrade.set(ModCoreDataComponents.MATCH_ALL_EFFECTS, matchAllEffects);
		save();
	}

	public boolean shouldMatchAllEffects() {
		return upgrade.getOrDefault(ModCoreDataComponents.MATCH_ALL_EFFECTS, true);
	}

	public boolean shouldMatchEffectDuration() {
		return upgrade.getOrDefault(ModCoreDataComponents.MATCH_EFFECT_DURATION, true);
	}

	public void setMatchEffectDuration(boolean matchEffectDuration) {
		upgrade.set(ModCoreDataComponents.MATCH_EFFECT_DURATION, matchEffectDuration);
		save();
	}

	public boolean shouldMatchEffectAmplifier() {
		return upgrade.getOrDefault(ModCoreDataComponents.MATCH_EFFECT_AMPLIFIER, true);
	}

	public void setMatchEffectAmplifier(boolean matchEffectAmplifier) {
		upgrade.set(ModCoreDataComponents.MATCH_EFFECT_AMPLIFIER, matchEffectAmplifier);
		save();
	}

	public EntityMatch getEntityMatch() {
		return upgrade.getOrDefault(ModCoreDataComponents.ENTITY_MATCH, EntityMatch.PLAYERS_AND_ENTITIES);
	}

	public void setEntityMatch(EntityMatch entityMatch) {
		upgrade.set(ModCoreDataComponents.ENTITY_MATCH, entityMatch);
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

	public interface AlchemyItemEntityMatcher {
		boolean test(LivingEntity entity, ItemStack stack, boolean matchAllEffects, boolean matchEffectAmplifier);
	}

	public interface AlchemyItemStackMatcher {
		boolean test(ItemStack stack, ItemStack filter, boolean matchAllEffects, boolean matchEffectDuration, boolean matchEffectAmplifier);
	}

	public interface StartUsing {
		int applyAsInt(ItemStack stack, LivingEntity livingEntity);
	}

	public interface FinishUsing {
		ItemStack apply(ItemStack stack, LivingEntity livingEntity);
	}


	public class ObservableFilterItemStackHandler extends FilterItemStackHandler {
		public ObservableFilterItemStackHandler(int filterSlotCount) {
			super(filterSlotCount);
		}

		@Override
		protected void onContentsChanged(int slot, ItemStack previousContents) {
			super.onContentsChanged(slot, previousContents);
			setFilter(slot, stacks.get(slot));
			save();
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			return resource.isEmpty() || (doesNotContain(resource) && isValidAlchemyItem(resource.toStack()));
		}

		private boolean doesNotContain(ItemResource resource) {
			return !InventoryHelper.hasItem(this, s -> s.equals(resource));
		}

		public void initFilters(List<AlchemyFilterAttribute> filterAttributes) {
			for (int slot = 0; slot < filterAttributes.size(); slot++) {
				setStackInSlot(slot, filterAttributes.get(slot).filter().copy());
			}
			updateEmptyFilters();
		}
	}
}
