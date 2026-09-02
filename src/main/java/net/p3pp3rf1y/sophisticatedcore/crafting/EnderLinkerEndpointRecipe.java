package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
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
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
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

	public EnderLinkerEndpointRecipe(net.minecraft.resources.ResourceLocation registryName, CraftingBookCategory category) {
		super(registryName, category);
	}

	@Override
	public boolean matches(CraftingContainer input, Level level) {
		return getInputs(input.getContainerSize(), input::getItem).filter(inputs -> isValidForLevel(level, inputs)).isPresent();
	}

	@Override
	public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
		return getInputs(input.getContainerSize(), input::getItem).map(inputs -> {
			ItemStack result = inputs.plan().returnsLinker() ? new ItemStack(inputs.linker().getItem()) : inputs.endpoint().copyWithCount(1);
			LinkedStorageStackLifecycle.clear(result);
			LinkedStorageStackData.setPendingCraft(result, new EnderLinkPendingCraftData(inputs.plan(), null));
			return result;
		}).orElse(ItemStack.EMPTY);
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
		NonNullList<ItemStack> remainingItems = NonNullList.withSize(input.getContainerSize(), ItemStack.EMPTY);
		getInputs(input.getContainerSize(), input::getItem).filter(inputs -> inputs.plan().returnsEndpointRemainder())
				.ifPresent(inputs -> remainingItems.set(inputs.endpointSlot(), inputs.endpoint().copyWithCount(1)));
		return remainingItems;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModRecipes.ENDER_LINKER_ENDPOINT_SERIALIZER.get();
	}

	public static void issueCraftClaim(Player player, ItemStack result) {
		EnderLinkPendingCraftData pendingCraft = LinkedStorageStackData.getPendingCraft(result);
		if (pendingCraft == null) {
			return;
		}
		UUID playerId = player.getUUID();
		InFlightClaim current = pendingCraft.claimId() == null ? null : IN_FLIGHT_CLAIMS.get(pendingCraft.claimId());
		if (current != null && current.playerId().equals(playerId) && current.plan() == pendingCraft.plan()) {
			return;
		}
		UUID latestClaimId = LATEST_IN_FLIGHT_CLAIMS.getOrDefault(playerId, Map.of()).get(pendingCraft.plan());
		InFlightClaim latest = latestClaimId == null ? null : IN_FLIGHT_CLAIMS.get(latestClaimId);
		if (latest != null && latest.playerId().equals(playerId) && latest.plan() == pendingCraft.plan()) {
			LinkedStorageStackData.setPendingCraft(result, new EnderLinkPendingCraftData(pendingCraft.plan(), latestClaimId));
			return;
		}
		UUID claimId = UUID.randomUUID();
		LinkedStorageStackData.setPendingCraft(result, new EnderLinkPendingCraftData(pendingCraft.plan(), claimId));
		IN_FLIGHT_CLAIMS.put(claimId, new InFlightClaim(claimId, playerId, pendingCraft.plan()));
		LATEST_IN_FLIGHT_CLAIMS.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(pendingCraft.plan(), claimId);
	}

	public static boolean completeCraft(ServerLevel level, Player player, Container inventory, ItemStack result) {
		Optional<Inputs> liveInputs = getInputs(inventory.getContainerSize(), inventory::getItem);
		EnderLinkPendingCraftData pendingCraft = LinkedStorageStackData.getPendingCraft(result);
		EnderLinkPendingCraftPlan plan = pendingCraft == null ? liveInputs.map(Inputs::plan).orElse(null) : pendingCraft.plan();
		InFlightClaim claim = plan == null ? null : findInFlightClaim(player, pendingCraft, plan);
		if (claim == null || liveInputs.isEmpty() || liveInputs.get().plan() != plan) {
			return false;
		}
		Inputs inputs = liveInputs.get();
		LinkedStorageEndpointData endpointData;
		if (plan == EnderLinkPendingCraftPlan.CREATE_PRIMARY) {
			if (!LinkedStorageService.link(level, player.getUUID(), inputs.linker().copyWithCount(1), inputs.endpoint())) {
				return false;
			}
			endpointData = LinkedStorageStackData.getEndpoint(inputs.endpoint());
		} else if (plan == EnderLinkPendingCraftPlan.BIND_EXISTING_ENDPOINT) {
			endpointData = LinkedStorageStackData.getEndpoint(inputs.endpoint());
		} else {
			if (LinkedStorageStackLifecycle.classifyLinker(inputs.linker()) == EnderLinkerStackState.PENDING_CRAFT
					&& !finalizePendingCraftLinker(level, inputs.linker())) {
				return false;
			}
			ItemStack endpoint = inputs.endpoint().copyWithCount(1);
			if (!LinkedStorageService.link(level, inputs.linker().copyWithCount(1), endpoint)) {
				return false;
			}
			endpointData = LinkedStorageStackData.getEndpoint(endpoint);
		}
		LinkedStorageGroupsSavedData.get(level).manager()
				.activatePendingCraftClaim(new ActivePendingCraftClaim(claim.claimId(), endpointData.groupId(), endpointData.endpointId(), plan));
		IN_FLIGHT_CLAIMS.remove(claim.claimId());
		LATEST_IN_FLIGHT_CLAIMS.computeIfPresent(player.getUUID(), (id, claims) -> {
			claims.remove(plan, claim.claimId());
			return claims.isEmpty() ? null : claims;
		});
		return true;
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
		int endpointSlot = linkerSlot < 0 ? -1 : findAlreadyLinkedEndpointSlot(inventory, linkerSlot);
		return endpointSlot < 0 ? Optional.empty() : Optional.of(new CraftingDiagnostic(endpointSlot, CraftingDiagnostic.Failure.ALREADY_LINKED));
	}

	private static Optional<CraftingDiagnostic> getDiagnosticForAddEndpointResult(int endpointSlot, LinkedStorageService.LinkResult result) {
		return switch (result) {
			case ALREADY_LINKED -> Optional.of(new CraftingDiagnostic(endpointSlot, CraftingDiagnostic.Failure.ALREADY_LINKED));
			case INCOMPATIBLE_ENDPOINT -> Optional.of(new CraftingDiagnostic(endpointSlot, CraftingDiagnostic.Failure.INCOMPATIBLE_ENDPOINT));
			case ENDPOINT_HAS_CONTENTS -> Optional.of(new CraftingDiagnostic(endpointSlot, CraftingDiagnostic.Failure.ENDPOINT_HAS_CONTENTS));
			default -> Optional.empty();
		};
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

	public static void clearInFlightClaim(UUID playerId) {
		IN_FLIGHT_CLAIMS.entrySet().removeIf(entry -> entry.getValue().playerId().equals(playerId));
		LATEST_IN_FLIGHT_CLAIMS.remove(playerId);
	}

	public static void clearInFlightClaims() {
		IN_FLIGHT_CLAIMS.clear();
		LATEST_IN_FLIGHT_CLAIMS.clear();
	}

	public static boolean finalizePendingCraftLinker(ServerLevel level, ItemStack linker) {
		EnderLinkPendingCraftData pendingCraft = LinkedStorageStackData.getPendingCraft(linker);
		if (pendingCraft == null || !pendingCraft.resolvesToLinkerTarget() || pendingCraft.claimId() == null) {
			return false;
		}
		Optional<ActivePendingCraftClaim> claim = LinkedStorageGroupsSavedData.get(level).manager().getActivePendingCraftClaim(pendingCraft.claimId())
				.filter(activeClaim -> activeClaim.plan() == pendingCraft.plan());
		if (claim.isEmpty()) {
			return false;
		}
		LinkedStorageStackData.clear(linker);
		LinkedStorageStackData.setLinkerTarget(linker, createTarget(LinkedStorageGroupsSavedData.get(level).manager(), claim.get().groupId()));
		LinkedStorageGroupsSavedData.get(level).manager().consumeActivePendingCraftClaim(claim.get().claimId());
		return true;
	}

	public static boolean finalizePendingCraftResult(ServerLevel level, ItemStack result) {
		EnderLinkPendingCraftData pendingCraft = LinkedStorageStackData.getPendingCraft(result);
		return pendingCraft != null
				&& (pendingCraft.resolvesToLinkerTarget() ? finalizePendingCraftLinker(level, result) : finalizePendingCraftEndpoint(level, result));
	}

	public static void finalizeTransferredCraftResult(ServerLevel level, ItemStack transferredStack, IItemHandler storageInventory, Inventory playerInventory) {
		EnderLinkPendingCraftData pendingCraft = LinkedStorageStackData.getPendingCraft(transferredStack);
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
		EnderLinkPendingCraftData pendingCraft = LinkedStorageStackData.getPendingCraft(craftedStack);
		if (pendingCraft == null || pendingCraft.claimId() == null) {
			return;
		}
		ItemStack result = hasClaim(pendingCraft.claimId(), carriedStack) ? carriedStack : findTransferredCraftResult(pendingCraft.claimId(), playerInventory);
		if (result.isEmpty()) {
			result = isUnclaimedDeliveredResult(craftedStack, carriedStack) ? carriedStack : findUnclaimedDeliveredResult(craftedStack, playerInventory);
		}
		if (!result.isEmpty()) {
			LinkedStorageStackData.setPendingCraft(result, pendingCraft);
			finalizePendingCraftResult(level, result);
		}
	}

	private static boolean finalizePendingCraftEndpoint(ServerLevel level, ItemStack endpoint) {
		EnderLinkPendingCraftData pendingCraft = LinkedStorageStackData.getPendingCraft(endpoint);
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
		LinkedStorageStackData.clear(endpoint);
		adapter.get().bindEndpoint(level, endpoint, new LinkedStorageEndpointData(claim.get().groupId(), claim.get().endpointId()));
		manager.consumeActivePendingCraftClaim(claim.get().claimId());
		return true;
	}

	private static EnderLinkerTargetData createTarget(LinkedStorageGroupManager manager, UUID groupId) {
		Component groupName = manager.resolveVirtualHost(groupId).orElseThrow().getLinkedStorageDisplayName().orElse(Component.empty());
		return new EnderLinkerTargetData(groupId, groupName);
	}

	private static ItemStack findTransferredCraftResult(UUID claimId, IItemHandler inventory) {
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (hasClaim(claimId, stack)) {
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
		EnderLinkPendingCraftData pendingCraft = LinkedStorageStackData.getPendingCraft(stack);
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
		EnderLinkPendingCraftData craftedPending = LinkedStorageStackData.getPendingCraft(craftedStack);
		EnderLinkPendingCraftData stackPending = LinkedStorageStackData.getPendingCraft(stack);
		return craftedPending != null && stack.is(craftedStack.getItem()) && stackPending != null && stackPending.claimId() == null
				&& stackPending.plan() == craftedPending.plan();
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
		EnderLinkPendingCraftPlan plan = linker.isEmpty() || endpoint.isEmpty() ? null : getPlan(linker, endpoint);
		return plan == null ? Optional.empty() : Optional.of(new Inputs(linker, linkerSlot, endpoint, endpointSlot, plan));
	}

	private static boolean isLinker(ItemStack stack) {
		EnderLinkerStackState state = LinkedStorageStackLifecycle.classifyLinker(stack);
		EnderLinkPendingCraftData pending = LinkedStorageStackData.getPendingCraft(stack);
		return stack.getItem() instanceof EnderLinkerItem && (state == EnderLinkerStackState.UNLINKED || state == EnderLinkerStackState.TARGET
				|| state == EnderLinkerStackState.PENDING_CRAFT && pending != null && pending.resolvesToLinkerTarget());
	}

	private static boolean isEndpoint(ItemStack stack) {
		return LinkedStorageStackLifecycle.classifyEndpoint(stack) == LinkedStorageEndpointStackState.ENDPOINT
				|| LinkedStorageStackLifecycle.classifyEndpoint(stack) == LinkedStorageEndpointStackState.UNLINKED
						&& LinkedStorageEndpointAdapters.find(stack).isPresent();
	}

	private static boolean isValidForLevel(Level level, Inputs inputs) {
		return !(level instanceof ServerLevel serverLevel) || inputs.plan() != EnderLinkPendingCraftPlan.ADD_SECONDARY
				|| LinkedStorageService.canAddEndpoint(serverLevel, inputs.linker(), inputs.endpoint());
	}

	@Nullable
	private static EnderLinkPendingCraftPlan getPlan(ItemStack linker, ItemStack endpoint) {
		if (LinkedStorageStackLifecycle.classifyLinker(linker) == EnderLinkerStackState.UNLINKED) {
			return LinkedStorageStackLifecycle.classifyEndpoint(endpoint) == LinkedStorageEndpointStackState.ENDPOINT
					? EnderLinkPendingCraftPlan.BIND_EXISTING_ENDPOINT
					: EnderLinkPendingCraftPlan.CREATE_PRIMARY;
		}
		return (LinkedStorageStackLifecycle.classifyLinker(linker) == EnderLinkerStackState.TARGET || isLinker(linker))
				&& LinkedStorageStackLifecycle.classifyEndpoint(endpoint) == LinkedStorageEndpointStackState.UNLINKED
						? EnderLinkPendingCraftPlan.ADD_SECONDARY
						: null;
	}

	@Nullable
	private static InFlightClaim findInFlightClaim(Player player, @Nullable EnderLinkPendingCraftData pendingCraft, EnderLinkPendingCraftPlan plan) {
		UUID claimId = pendingCraft == null ? LATEST_IN_FLIGHT_CLAIMS.getOrDefault(player.getUUID(), Map.of()).get(plan) : pendingCraft.claimId();
		InFlightClaim claim = claimId == null ? null : IN_FLIGHT_CLAIMS.get(claimId);
		return claim != null && claim.playerId().equals(player.getUUID()) && claim.plan() == plan ? claim : null;
	}

	private record Inputs(ItemStack linker, int linkerSlot, ItemStack endpoint, int endpointSlot, EnderLinkPendingCraftPlan plan) {
	}

	private record InFlightClaim(UUID claimId, UUID playerId, EnderLinkPendingCraftPlan plan) {
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
}
