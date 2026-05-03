package net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.network.SyncRecentCraftedResultsMessage;

import java.util.*;

public class RecentCraftedResultStorage extends SavedData {
	public static final int MAX_RECENT_RESULTS_PER_INGREDIENT = 4;
	private static final String SAVED_DATA_NAME = SophisticatedCore.MOD_ID + "_recent_crafted_results";
	private static final String PLAYERS_TAG = "players";
	private static final String SCOPES_TAG = "scopes";

	private final Map<UUID, Map<ResourceLocation, Map<ResourceLocation, List<ResourceLocation>>>> playerRecentResults;
	private static Map<ResourceLocation, Map<ResourceLocation, List<ResourceLocation>>> clientRecentResults = new HashMap<>();

	private RecentCraftedResultStorage() {
		playerRecentResults = new HashMap<>();
	}

	private RecentCraftedResultStorage(Map<UUID, Map<ResourceLocation, Map<ResourceLocation, List<ResourceLocation>>>> playerRecentResults) {
		this.playerRecentResults = playerRecentResults;
	}

	public static RecentCraftedResultStorage get(ServerLevel level) {
		ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
		//noinspection ConstantConditions - overworld is loaded while server levels are available
		DimensionDataStorage storage = overworld.getDataStorage();
		return storage.computeIfAbsent(RecentCraftedResultStorage::load, RecentCraftedResultStorage::new, SAVED_DATA_NAME);
	}

	public List<ResourceLocation> getRecentResults(Player player, ResourceLocation scope, ResourceLocation ingredient) {
		return playerRecentResults.getOrDefault(player.getUUID(), Map.of())
				.getOrDefault(scope, Map.of())
				.getOrDefault(ingredient, List.of());
	}

	public static List<ResourceLocation> getClientRecentResults(ResourceLocation scope, ResourceLocation ingredient) {
		return clientRecentResults.getOrDefault(scope, Map.of()).getOrDefault(ingredient, List.of());
	}

	public static void syncToPlayer(ServerPlayer player) {
		RecentCraftedResultStorage storage = get(player.serverLevel());
		PacketHandler.INSTANCE.sendToClient(player, new SyncRecentCraftedResultsMessage(storage.serializePlayerResults(player)));
	}

	public CompoundTag serializePlayerResults(Player player) {
		return serializeScopes(playerRecentResults.getOrDefault(player.getUUID(), Map.of()));
	}

	public static void updateClientRecentResults(CompoundTag recentResults) {
		clientRecentResults = deserializeScopes(recentResults);
	}

	public boolean recordCraftedResult(Player player, ResourceLocation scope, ResourceLocation ingredient, ResourceLocation result) {
		List<ResourceLocation> recentResults = playerRecentResults.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>())
				.computeIfAbsent(scope, key -> new HashMap<>())
				.computeIfAbsent(ingredient, key -> new ArrayList<>());
		if (!recentResults.isEmpty() && recentResults.get(0).equals(result)) {
			return false;
		}

		recentResults.remove(result);
		recentResults.add(0, result);
		while (recentResults.size() > MAX_RECENT_RESULTS_PER_INGREDIENT) {
			recentResults.remove(recentResults.size() - 1);
		}
		setDirty();
		return true;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		CompoundTag playersTag = new CompoundTag();
		for (Map.Entry<UUID, Map<ResourceLocation, Map<ResourceLocation, List<ResourceLocation>>>> playerEntry : playerRecentResults.entrySet()) {
			CompoundTag scopesTag = new CompoundTag();
			for (Map.Entry<ResourceLocation, Map<ResourceLocation, List<ResourceLocation>>> scopeEntry : playerEntry.getValue().entrySet()) {
				CompoundTag ingredientsTag = new CompoundTag();
				for (Map.Entry<ResourceLocation, List<ResourceLocation>> ingredientEntry : scopeEntry.getValue().entrySet()) {
					ListTag resultsTag = new ListTag();
					ingredientEntry.getValue().forEach(result -> resultsTag.add(StringTag.valueOf(result.toString())));
					ingredientsTag.put(ingredientEntry.getKey().toString(), resultsTag);
				}
				scopesTag.put(scopeEntry.getKey().toString(), ingredientsTag);
			}
			playersTag.put(playerEntry.getKey().toString(), scopesTag);
		}
		tag.put(PLAYERS_TAG, playersTag);
		return tag;
	}

	private static RecentCraftedResultStorage load(CompoundTag tag) {
		Map<UUID, Map<ResourceLocation, Map<ResourceLocation, List<ResourceLocation>>>> playerRecentResults = new HashMap<>();
		CompoundTag playersTag = tag.getCompound(PLAYERS_TAG);
		for (String playerKey : playersTag.getAllKeys()) {
			UUID playerUuid = UUID.fromString(playerKey);
			Map<ResourceLocation, Map<ResourceLocation, List<ResourceLocation>>> scopes = new HashMap<>();
			CompoundTag scopesTag = playersTag.getCompound(playerKey);
			for (String scopeKey : scopesTag.getAllKeys()) {
				Map<ResourceLocation, List<ResourceLocation>> ingredients = new HashMap<>();
				CompoundTag ingredientsTag = scopesTag.getCompound(scopeKey);
				for (String ingredientKey : ingredientsTag.getAllKeys()) {
					List<ResourceLocation> results = new ArrayList<>();
					ListTag resultsTag = ingredientsTag.getList(ingredientKey, Tag.TAG_STRING);
					for (int i = 0; i < resultsTag.size() && i < MAX_RECENT_RESULTS_PER_INGREDIENT; i++) {
						results.add(new ResourceLocation(resultsTag.getString(i)));
					}
					ingredients.put(new ResourceLocation(ingredientKey), results);
				}
				scopes.put(new ResourceLocation(scopeKey), ingredients);
			}
			playerRecentResults.put(playerUuid, scopes);
		}
		return new RecentCraftedResultStorage(playerRecentResults);
	}

	private static CompoundTag serializeScopes(Map<ResourceLocation, Map<ResourceLocation, List<ResourceLocation>>> scopes) {
		CompoundTag tag = new CompoundTag();
		CompoundTag scopesTag = new CompoundTag();
		for (Map.Entry<ResourceLocation, Map<ResourceLocation, List<ResourceLocation>>> scopeEntry : scopes.entrySet()) {
			CompoundTag ingredientsTag = new CompoundTag();
			for (Map.Entry<ResourceLocation, List<ResourceLocation>> ingredientEntry : scopeEntry.getValue().entrySet()) {
				ListTag resultsTag = new ListTag();
				ingredientEntry.getValue().forEach(result -> resultsTag.add(StringTag.valueOf(result.toString())));
				ingredientsTag.put(ingredientEntry.getKey().toString(), resultsTag);
			}
			scopesTag.put(scopeEntry.getKey().toString(), ingredientsTag);
		}
		tag.put(SCOPES_TAG, scopesTag);
		return tag;
	}

	private static Map<ResourceLocation, Map<ResourceLocation, List<ResourceLocation>>> deserializeScopes(CompoundTag tag) {
		Map<ResourceLocation, Map<ResourceLocation, List<ResourceLocation>>> scopes = new HashMap<>();
		CompoundTag scopesTag = tag.getCompound(SCOPES_TAG);
		for (String scopeKey : scopesTag.getAllKeys()) {
			Map<ResourceLocation, List<ResourceLocation>> ingredients = new HashMap<>();
			CompoundTag ingredientsTag = scopesTag.getCompound(scopeKey);
			for (String ingredientKey : ingredientsTag.getAllKeys()) {
				List<ResourceLocation> results = new ArrayList<>();
				ListTag resultsTag = ingredientsTag.getList(ingredientKey, Tag.TAG_STRING);
				for (int i = 0; i < resultsTag.size() && i < MAX_RECENT_RESULTS_PER_INGREDIENT; i++) {
					results.add(new ResourceLocation(resultsTag.getString(i)));
				}
				ingredients.put(new ResourceLocation(ingredientKey), results);
			}
			scopes.put(new ResourceLocation(scopeKey), ingredients);
		}
		return scopes;
	}
}
