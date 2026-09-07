package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.crafting.EnderLinkerEndpointRecipe;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;

import javax.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class EnderLinkerItem extends ItemBase {
	private static final int LINKED_STORAGE_TOOLTIP_COLOR = 0xAE8BC7;

	public EnderLinkerItem() {
		super(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, SophisticatedCore.getRL("ender_linker"))).stacksTo(64));
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		EnderLinkerStackState state = LinkedStorageStackLifecycle.classifyLinker(stack);
		return state == EnderLinkerStackState.UNLINKED || state == EnderLinkerStackState.TARGET ? 64 : 1;
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack linker, Slot slot, ClickAction action, Player player) {
		return action == ClickAction.SECONDARY && tryLinkItemEndpoint(player, linker, slot.getItem()).isPresent();
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack linker, ItemStack endpoint, Slot slot, ClickAction action, Player player, SlotAccess carriedAccess) {
		return action == ClickAction.SECONDARY && tryLinkItemEndpoint(player, linker, endpoint).isPresent();
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack linker, UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		return tryLinkBlock(player, linker, context.getLevel(), context.getClickedPos());
	}

	private static InteractionResult tryLinkBlock(Player player, ItemStack linker, Level level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof ILinkedStorageBlockEndpoint endpoint)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Optional<LinkedStorageService.LinkResult> result = tryLinkInteractionTarget(player, linker, endpoint, pos);
		if (result.isEmpty() || result.get() != LinkedStorageService.LinkResult.SUCCESS) {
			return InteractionResult.FAIL;
		}
		return InteractionResult.SUCCESS;
	}

	public static Optional<LinkedStorageService.LinkResult> tryLinkItemInteractionTarget(Player player, ItemStack linker,
			ILinkedStorageItemInteractionTarget target, @Nullable BlockPos feedbackPos) {
		return tryLinkInteractionTarget(player, linker, target, feedbackPos);
	}

	private static Optional<LinkedStorageService.LinkResult> tryLinkItemEndpoint(Player player, ItemStack linker, ItemStack endpoint) {
		return tryLinkItemInteractionTarget(player, linker, () -> endpoint, null);
	}

	public static Optional<LinkedStorageService.LinkResult> tryLinkInteractionTarget(Player player, ItemStack linker, ILinkedStorageInteractionTarget target,
			@Nullable BlockPos feedbackPos) {
		if (!target.isLinkedStorageLinkCandidate()) {
			return Optional.empty();
		}

		LinkedStorageEndpointData previousEndpoint = target.getLinkedStorageEndpointData();
		LinkedStorageService.LinkResult result = linkWithFeedback(player, linker, feedbackPos,
				(level, linkerToBind) -> target.link(level, player.getUUID(), linkerToBind));
		if (result == LinkedStorageService.LinkResult.SUCCESS && player instanceof ServerPlayer serverPlayer) {
			LinkedStorageEndpointData currentEndpoint = target.getLinkedStorageEndpointData();
			if (!Objects.equals(previousEndpoint, currentEndpoint)) {
				target.onLinkedStorageEndpointChanged(serverPlayer, previousEndpoint, currentEndpoint);
			}
		}
		return Optional.of(result);
	}

	private static LinkedStorageService.LinkResult linkWithFeedback(Player player, ItemStack linker, @Nullable BlockPos blockPos,
			BiFunction<ServerLevel, ItemStack, LinkedStorageService.LinkResult> linkOperation) {
		if (!(player.level() instanceof ServerLevel level)) {
			return LinkedStorageService.LinkResult.SUCCESS;
		}
		if (linker.has(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT)) {
			if (!EnderLinkerEndpointRecipe.finalizePendingCraftLinker(level, linker)) {
				throw new IllegalStateException("Could not finalize pending Ender Linker craft claim");
			}
		}
		if (LinkedStorageStackLifecycle.classifyLinker(linker) == EnderLinkerStackState.UNLINKED && linker.getCount() > 1) {
			ItemStack boundLinker = linker.copyWithCount(1);
			LinkedStorageService.LinkResult result = linkOperation.apply(level, boundLinker);
			if (result != LinkedStorageService.LinkResult.SUCCESS) {
				playFailureFeedback(player, result);
				return result;
			}
			linker.shrink(1);
			if (!player.getInventory().add(boundLinker)) {
				player.drop(boundLinker, false);
			}
			playLinkFeedback(level, player, blockPos);
			return LinkedStorageService.LinkResult.SUCCESS;
		}
		LinkedStorageService.LinkResult result = linkOperation.apply(level, linker);
		if (result != LinkedStorageService.LinkResult.SUCCESS) {
			playFailureFeedback(player, result);
			return result;
		}
		playLinkFeedback(level, player, blockPos);
		return LinkedStorageService.LinkResult.SUCCESS;
	}

	@Override
	public void onCraftedBy(ItemStack stack, Player player) {
		if (player.level() instanceof ServerLevel) {
			EnderLinkerEndpointRecipe.issueCraftClaim(player, stack);
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
		if (stack.has(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT) && !EnderLinkerEndpointRecipe.finalizePendingCraftLinker(level, stack)) {
			throw new IllegalStateException("Could not finalize pending Ender Linker craft claim");
		}
	}

	private static void playLinkFeedback(ServerLevel level, Player player, @Nullable BlockPos blockPos) {
		Vec3 feedbackPos = blockPos == null ? new Vec3(player.getX(), player.getY(), player.getZ()) : Vec3.atCenterOf(blockPos);
		level.playSound(null, feedbackPos.x, feedbackPos.y, feedbackPos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 1.15F);
		if (blockPos != null) {
			level.sendParticles(ParticleTypes.WITCH, feedbackPos.x, feedbackPos.y, feedbackPos.z, 8, 0.2D, 0.25D, 0.2D, 0.01D);
		}
	}

	private static void playFailureFeedback(Player player, LinkedStorageService.LinkResult result) {
		player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 0.7F);
		player.displayClientMessage(TranslationHelper.INSTANCE.translStatusMessage("ender_linker." + result.name().toLowerCase()), true);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip,
			TooltipFlag tooltipFlag) {
		EnderLinkerStackState state = LinkedStorageStackLifecycle.classifyLinker(stack);
		boolean pendingLinker = isPendingLinker(stack, state);
		boolean boundLinker = hasBoundPresentation(stack);
		tooltip.accept(TranslationHelper.INSTANCE.translItemTooltip(this, boundLinker ? "bound" : "blank").withStyle(ChatFormatting.DARK_GRAY));
		if (state == EnderLinkerStackState.TARGET) {
			EnderLinkerTargetData target = stack.get(ModCoreDataComponents.ENDER_LINKER_TARGET);
			if (!target.groupName().getString().isEmpty()) {
				tooltip.accept(TranslationHelper.INSTANCE
						.translItemTooltip(this, "linker_target", target.groupName().copy().withStyle(style -> style.withColor(LINKED_STORAGE_TOOLTIP_COLOR)))
						.withStyle(ChatFormatting.GRAY));
			}
		} else if (pendingLinker) {
			tooltip.accept(TranslationHelper.INSTANCE.translItemTooltip(this, "linker_pending").withStyle(ChatFormatting.GRAY));
		}
	}

	public static boolean hasBoundPresentation(ItemStack stack) {
		EnderLinkerStackState state = LinkedStorageStackLifecycle.classifyLinker(stack);
		return state == EnderLinkerStackState.TARGET || isPendingLinker(stack, state);
	}

	private static boolean isPendingLinker(ItemStack stack, EnderLinkerStackState state) {
		return stack.getItem() instanceof EnderLinkerItem && state == EnderLinkerStackState.PENDING_CRAFT
				&& stack.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT).resolvesToLinkerTarget();
	}
}
