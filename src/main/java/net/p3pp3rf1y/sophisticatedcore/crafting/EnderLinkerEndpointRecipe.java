package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ActivePendingCraftClaim;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkPendingCraftData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkPendingCraftPlan;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerItem;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerStackState;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.EnderLinkerTargetData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageItemEndpointAdapter;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointAdapters;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointStackState;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackLifecycle;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;

public class EnderLinkerEndpointRecipe extends CustomRecipe {
	private static final Map<UUID, InFlightClaim> IN_FLIGHT_CLAIMS = new HashMap<>();
	private static final Map<UUID, Map<EnderLinkPendingCraftPlan, UUID>> LATEST_IN_FLIGHT_CLAIMS = new HashMap<>();

	public EnderLinkerEndpointRecipe(CraftingBookCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return getInputs(input.size(), input::getItem).filter(inputs -> isValidForLevel(level, inputs)).isPresent();
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		return getInputs(input.size(), input::getItem).map(inputs -> {
			ItemStack result = inputs.plan().returnsLinker() ? new ItemStack(inputs.linker().getItem()) : inputs.endpoint().copyWithCount(1);
			LinkedStorageStackLifecycle.clear(result);
			result.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, new EnderLinkPendingCraftData(inputs.plan(), null));
			return result;
		}).orElse(ItemStack.EMPTY);
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
		NonNullList<ItemStack> remainingItems = NonNullList.withSize(input.size(), ItemStack.EMPTY);
		getInputs(input.size(), input::getItem).filter(inputs -> inputs.plan().returnsEndpointRemainder())
				.ifPresent(inputs -> remainingItems.set(inputs.endpointSlot(), inputs.endpoint().copyWithCount(1)));
		return remainingItems;
	}

	@Override
	public RecipeSerializer<EnderLinkerEndpointRecipe> getSerializer() {
		return ModRecipes.ENDER_LINKER_ENDPOINT_SERIALIZER.get();
	}

	public static void issueCraftClaim(Player player, ItemStack result) {
		EnderLinkPendingCraftData pendingCraft = result.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		if (pendingCraft == null) {
			return;
		}
		EnderLinkPendingCraftPlan plan = pendingCraft.plan();

		UUID playerId = player.getUUID();
		InFlightClaim inFlightClaim = pendingCraft.claimId() == null ? null : IN_FLIGHT_CLAIMS.get(pendingCraft.claimId());
		if (inFlightClaim != null && inFlightClaim.playerId().equals(playerId) && inFlightClaim.plan() == plan) {
			return;
		}
		UUID latestClaimId = LATEST_IN_FLIGHT_CLAIMS.getOrDefault(playerId, Map.of()).get(plan);
		InFlightClaim latestClaim = latestClaimId == null ? null : IN_FLIGHT_CLAIMS.get(latestClaimId);
		if (latestClaim != null && latestClaim.playerId().equals(playerId) && latestClaim.plan() == plan) {
			result.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, new EnderLinkPendingCraftData(pendingCraft.plan(), latestClaimId));
			return;
		}
		UUID claimId = UUID.randomUUID();
		result.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, new EnderLinkPendingCraftData(pendingCraft.plan(), claimId));
		IN_FLIGHT_CLAIMS.put(claimId, new InFlightClaim(claimId, playerId, plan));
		LATEST_IN_FLIGHT_CLAIMS.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(plan, claimId);
	}

	public static Optional<CraftingDiagnostic> getCraftingDiagnostic(ServerLevel level, Container inventory) {
		Optional<Inputs> inputs = getInputs(inventory.getContainerSize(), inventory::getItem);
		if (inputs.isPresent()) {
			Inputs validInputs = inputs.get();
			if (validInputs.plan() == EnderLinkPendingCraftPlan.ADD_SECONDARY) {
				return getDiagnosticForAddEndpointResult(validInputs.endpointSlot(),
						LinkedStorageService.getAddEndpointResult(level, validInputs.linker(), validInputs.endpoint()));
			}
			return Optional.empty();
		}

		int linkerSlot = findLinkerSlot(inventory);
		int alreadyLinkedEndpointSlot = linkerSlot < 0 ? -1 : findAlreadyLinkedEndpointSlot(inventory, linkerSlot);
		if (alreadyLinkedEndpointSlot >= 0) {
			return Optional.of(new CraftingDiagnostic(alreadyLinkedEndpointSlot, CraftingDiagnostic.Failure.ALREADY_LINKED));
		}
		return Optional.empty();
	}

	private static Optional<CraftingDiagnostic> getDiagnosticForAddEndpointResult(int endpointSlot, LinkedStorageService.LinkResult result) {
		return switch (result) {
			case SUCCESS -> Optional.empty();
			case ALREADY_LINKED -> Optional.of(new CraftingDiagnostic(endpointSlot, CraftingDiagnostic.Failure.ALREADY_LINKED));
			case INCOMPATIBLE_ENDPOINT -> Optional.of(new CraftingDiagnostic(endpointSlot, CraftingDiagnostic.Failure.INCOMPATIBLE_ENDPOINT));
			case ENDPOINT_HAS_CONTENTS -> Optional.of(new CraftingDiagnostic(endpointSlot, CraftingDiagnostic.Failure.ENDPOINT_HAS_CONTENTS));
			default -> Optional.empty();
		};
	}

	public static boolean completeCraft(ServerLevel level, Player player, Container inventory, ItemStack result) {
		Optional<Inputs> liveInputs = getInputs(inventory.getContainerSize(), inventory::getItem);
		EnderLinkPendingCraftData pendingCraft = result.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		EnderLinkPendingCraftPlan plan = pendingCraft == null ? liveInputs.map(Inputs::plan).orElse(null) : pendingCraft.plan();
		if (plan == null) {
			return false;
		}
		InFlightClaim claim = findInFlightClaim(player, pendingCraft, plan);
		if (claim == null) {
			return false;
		}
		if (liveInputs.isEmpty() || liveInputs.get().plan() != plan) {
			return false;
		}
		Inputs inputs = liveInputs.get();
		LinkedStorageEndpointData endpointData;
		if (plan == EnderLinkPendingCraftPlan.CREATE_PRIMARY) {
			if (!LinkedStorageService.link(level, player.getUUID(), inputs.linker().copyWithCount(1), inputs.endpoint())) {
				return false;
			}
			endpointData = inputs.endpoint().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		} else if (plan == EnderLinkPendingCraftPlan.BIND_EXISTING_ENDPOINT) {
			endpointData = inputs.endpoint().get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		} else {
			if (LinkedStorageStackLifecycle.classifyLinker(inputs.linker()) == EnderLinkerStackState.PENDING_CRAFT
					&& !finalizePendingCraftLinker(level, inputs.linker())) {
				return false;
			}
			ItemStack endpoint = inputs.endpoint().copyWithCount(1);
			if (!LinkedStorageService.link(level, inputs.linker().copyWithCount(1), endpoint)) {
				return false;
			}
			endpointData = endpoint.get(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
		}
		LinkedStorageGroupsSavedData.get(level).manager()
				.activatePendingCraftClaim(new ActivePendingCraftClaim(claim.claimId(), endpointData.groupId(), endpointData.endpointId(), inputs.plan()));
		IN_FLIGHT_CLAIMS.remove(claim.claimId());
		Map<EnderLinkPendingCraftPlan, UUID> latestClaims = LATEST_IN_FLIGHT_CLAIMS.get(player.getUUID());
		if (latestClaims != null) {
			latestClaims.remove(plan, claim.claimId());
			if (latestClaims.isEmpty()) {
				LATEST_IN_FLIGHT_CLAIMS.remove(player.getUUID());
			}
		}
		return true;
	}

	public static void clearInFlightClaim(UUID playerId) {
		IN_FLIGHT_CLAIMS.entrySet().removeIf(entry -> entry.getValue().playerId().equals(playerId));
		LATEST_IN_FLIGHT_CLAIMS.remove(playerId);
	}

	public static void clearInFlightClaims() {
		IN_FLIGHT_CLAIMS.clear();
		LATEST_IN_FLIGHT_CLAIMS.clear();
	}

	public static boolean finalizePendingCraftLinker(ServerLevel level, ItemStack linker) {
		EnderLinkPendingCraftData pendingCraft = linker.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		if (pendingCraft == null || !pendingCraft.resolvesToLinkerTarget() || pendingCraft.claimId() == null) {
			return false;
		}
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		Optional<ActivePendingCraftClaim> claim = manager.getActivePendingCraftClaim(pendingCraft.claimId())
				.filter(activeClaim -> activeClaim.plan() == pendingCraft.plan());
		if (claim.isEmpty()) {
			return false;
		}
		linker.remove(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		linker.set(ModCoreDataComponents.ENDER_LINKER_TARGET, createTarget(manager, claim.get().groupId()));
		manager.consumeActivePendingCraftClaim(claim.get().claimId());
		return true;
	}

	public static boolean finalizePendingCraftResult(ServerLevel level, ItemStack result) {
		EnderLinkPendingCraftData pendingCraft = result.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		return pendingCraft != null
				&& (pendingCraft.resolvesToLinkerTarget() ? finalizePendingCraftLinker(level, result) : finalizePendingCraftEndpoint(level, result));
	}

	private static boolean finalizePendingCraftEndpoint(ServerLevel level, ItemStack endpoint) {
		EnderLinkPendingCraftData pendingCraft = endpoint.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		if (pendingCraft == null || pendingCraft.claimId() == null) {
			return false;
		}
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(level).manager();
		Optional<ActivePendingCraftClaim> claim = manager.getActivePendingCraftClaim(pendingCraft.claimId())
				.filter(activeClaim -> activeClaim.plan() == pendingCraft.plan());
		Optional<ILinkedStorageItemEndpointAdapter> adapter = LinkedStorageEndpointAdapters.find(endpoint);
		if (claim.isEmpty() || adapter.isEmpty() || !manager.usesHostFactory(claim.get().groupId(), adapter.get().factoryId())) {
			return false;
		}
		endpoint.remove(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		adapter.get().bindEndpoint(level, endpoint, new LinkedStorageEndpointData(claim.get().groupId(), claim.get().endpointId()));
		manager.consumeActivePendingCraftClaim(claim.get().claimId());
		return true;
	}

	public static void finalizeTransferredCraftResult(ServerLevel level, ItemStack transferredStack, IItemHandler storageInventory, Inventory playerInventory) {
		EnderLinkPendingCraftData pendingCraft = transferredStack.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		if (pendingCraft == null || pendingCraft.claimId() == null) {
			return;
		}
		ItemStack result = findTransferredCraftResult(pendingCraft.claimId(), storageInventory);
		if (result.isEmpty()) {
			result = findTransferredCraftResult(pendingCraft.claimId(), playerInventory);
		}
		if (!result.isEmpty()) {
			finalizePendingCraftResult(level, result);
		}
	}

	public static void finalizeDeliveredCraftResult(ServerLevel level, ItemStack craftedStack, Inventory playerInventory, ItemStack carriedStack) {
		EnderLinkPendingCraftData pendingCraft = craftedStack.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		if (pendingCraft == null || pendingCraft.claimId() == null) {
			return;
		}
		ItemStack result = hasClaim(pendingCraft.claimId(), carriedStack) ? carriedStack : findTransferredCraftResult(pendingCraft.claimId(), playerInventory);
		if (result.isEmpty()) {
			result = isUnclaimedDeliveredResult(craftedStack, carriedStack) ? carriedStack : findUnclaimedDeliveredResult(craftedStack, playerInventory);
		}
		if (!result.isEmpty()) {
			result.set(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT, pendingCraft);
			finalizePendingCraftResult(level, result);
		}
	}

	private static EnderLinkerTargetData createTarget(LinkedStorageGroupManager manager, UUID groupId) {
		Component groupName = manager.resolveVirtualHost(groupId).orElseThrow().getLinkedStorageDisplayName().orElse(Component.empty());
		return new EnderLinkerTargetData(groupId, groupName);
	}

	private static ItemStack findTransferredCraftResult(UUID claimId, IItemHandler inventory) {
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			EnderLinkPendingCraftData pendingCraft = stack.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
			if (pendingCraft != null && claimId.equals(pendingCraft.claimId())) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack findTransferredCraftResult(UUID claimId, Container inventory) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (hasClaim(claimId, stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static boolean hasClaim(UUID claimId, ItemStack stack) {
		EnderLinkPendingCraftData pendingCraft = stack.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		return pendingCraft != null && claimId.equals(pendingCraft.claimId());
	}

	private static ItemStack findUnclaimedDeliveredResult(ItemStack craftedStack, Container inventory) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (isUnclaimedDeliveredResult(craftedStack, stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static boolean isUnclaimedDeliveredResult(ItemStack craftedStack, ItemStack stack) {
		EnderLinkPendingCraftData craftedPending = craftedStack.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		EnderLinkPendingCraftData stackPending = stack.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		return craftedPending != null && stack.is(craftedStack.getItem()) && stackPending != null && stackPending.claimId() == null
				&& stackPending.plan() == craftedPending.plan();
	}

	@Nullable
	private static InFlightClaim findInFlightClaim(Player player, @Nullable EnderLinkPendingCraftData pendingCraft, EnderLinkPendingCraftPlan plan) {
		UUID playerId = player.getUUID();
		UUID claimId = pendingCraft == null ? null : pendingCraft.claimId();
		if (claimId == null) {
			claimId = LATEST_IN_FLIGHT_CLAIMS.getOrDefault(playerId, Map.of()).get(plan);
		}
		InFlightClaim claim = claimId == null ? null : IN_FLIGHT_CLAIMS.get(claimId);
		if (claim == null || !claim.playerId().equals(playerId) || claim.plan() != plan) {
			return null;
		}
		if (pendingCraft != null && pendingCraft.claimId() != null && !pendingCraft.claimId().equals(claim.claimId())) {
			return null;
		}
		return claim;
	}

	private static Optional<Inputs> getInputs(int size, IntFunction<ItemStack> getItem) {
		ItemStack linker = ItemStack.EMPTY;
		ItemStack endpoint = ItemStack.EMPTY;
		int linkerSlot = -1;
		int endpointSlot = -1;
		for (int slot = 0; slot < size; slot++) {
			ItemStack stack = getItem.apply(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (linker.isEmpty() && isLinker(stack)) {
				linker = stack;
				linkerSlot = slot;
			} else if (endpoint.isEmpty() && isEndpoint(stack)) {
				endpoint = stack;
				endpointSlot = slot;
			} else {
				return Optional.empty();
			}
		}
		if (linker.isEmpty() || endpoint.isEmpty()) {
			return Optional.empty();
		}
		EnderLinkPendingCraftPlan plan = getPlan(linker, endpoint);
		return plan == null ? Optional.empty() : Optional.of(new Inputs(linker, linkerSlot, endpoint, endpointSlot, plan));
	}

	private static int findLinkerSlot(Container inventory) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (inventory.getItem(slot).getItem() instanceof EnderLinkerItem) {
				return slot;
			}
		}
		return -1;
	}

	private static int findAlreadyLinkedEndpointSlot(Container inventory, int linkerSlot) {
		if (LinkedStorageStackLifecycle.classifyLinker(inventory.getItem(linkerSlot)) != EnderLinkerStackState.TARGET) {
			return -1;
		}
		int endpointCount = 0;
		int endpointSlot = -1;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (slot != linkerSlot && !inventory.getItem(slot).isEmpty()) {
				if (LinkedStorageStackLifecycle.classifyEndpoint(inventory.getItem(slot)) != LinkedStorageEndpointStackState.ENDPOINT) {
					return -1;
				}
				endpointCount++;
				endpointSlot = slot;
			}
		}
		return endpointCount == 1 ? endpointSlot : -1;
	}

	private static boolean isLinker(ItemStack stack) {
		EnderLinkerStackState state = LinkedStorageStackLifecycle.classifyLinker(stack);
		return stack.getItem() instanceof EnderLinkerItem
				&& (state == EnderLinkerStackState.UNLINKED || state == EnderLinkerStackState.TARGET || isPendingLinker(stack));
	}

	private static boolean isEndpoint(ItemStack stack) {
		LinkedStorageEndpointStackState state = LinkedStorageStackLifecycle.classifyEndpoint(stack);
		return state == LinkedStorageEndpointStackState.ENDPOINT
				|| (state == LinkedStorageEndpointStackState.UNLINKED && LinkedStorageEndpointAdapters.find(stack).isPresent());
	}

	private static boolean isValidForLevel(Level level, Inputs inputs) {
		return !(level instanceof ServerLevel serverLevel) || inputs.plan() != EnderLinkPendingCraftPlan.ADD_SECONDARY
				|| LinkedStorageService.canAddEndpoint(serverLevel, inputs.linker(), inputs.endpoint());
	}

	private static boolean isPendingLinker(ItemStack stack) {
		EnderLinkPendingCraftData pendingCraft = stack.get(ModCoreDataComponents.ENDER_LINK_PENDING_CRAFT);
		return LinkedStorageStackLifecycle.classifyLinker(stack) == EnderLinkerStackState.PENDING_CRAFT && pendingCraft.resolvesToLinkerTarget();
	}

	@Nullable
	private static EnderLinkPendingCraftPlan getPlan(ItemStack linker, ItemStack endpoint) {
		if (LinkedStorageStackLifecycle.classifyLinker(linker) == EnderLinkerStackState.UNLINKED) {
			return LinkedStorageStackLifecycle.classifyEndpoint(endpoint) == LinkedStorageEndpointStackState.ENDPOINT
					? EnderLinkPendingCraftPlan.BIND_EXISTING_ENDPOINT
					: EnderLinkPendingCraftPlan.CREATE_PRIMARY;
		}
		return (LinkedStorageStackLifecycle.classifyLinker(linker) == EnderLinkerStackState.TARGET || isPendingLinker(linker))
				&& LinkedStorageStackLifecycle.classifyEndpoint(endpoint) == LinkedStorageEndpointStackState.UNLINKED
						? EnderLinkPendingCraftPlan.ADD_SECONDARY
						: null;
	}

	private record Inputs(ItemStack linker, int linkerSlot, ItemStack endpoint, int endpointSlot, EnderLinkPendingCraftPlan plan) {
	}

	public record CraftingDiagnostic(int slot, Failure failure) {
		public enum Failure {
			ALREADY_LINKED("ender_linker.already_linked"), INCOMPATIBLE_ENDPOINT("ender_linker.incompatible_endpoint"), ENDPOINT_HAS_CONTENTS(
					"ender_linker.endpoint_has_contents");

			private final String statusMessage;

			Failure(String statusMessage) {
				this.statusMessage = statusMessage;
			}

			public String statusMessage() {
				return statusMessage;
			}
		}
	}

	private record InFlightClaim(UUID claimId, UUID playerId, EnderLinkPendingCraftPlan plan) {
	}
}
