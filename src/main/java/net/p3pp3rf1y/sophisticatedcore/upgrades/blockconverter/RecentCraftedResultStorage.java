package net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.network.SyncRecentCraftedResultsPayload;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class RecentCraftedResultStorage extends SavedData {
	public static final int MAX_RECENT_RESULTS_PER_INGREDIENT = 4;
	private static final String SCOPES_TAG = "scopes";
	private static final Codec<List<Identifier>> RECENT_RESULTS_CODEC = Codec.list(Identifier.CODEC)
			.xmap(results -> results.stream().limit(MAX_RECENT_RESULTS_PER_INGREDIENT).toList(), List::copyOf).xmap(CodecHelper::toMutable, Function.identity());
	private static final Codec<Map<Identifier, Map<Identifier, List<Identifier>>>> SCOPES_CODEC = Codec.unboundedMap(
			Identifier.CODEC,
			Codec.unboundedMap(Identifier.CODEC, RECENT_RESULTS_CODEC).xmap(CodecHelper::toMutable, Function.identity())
	).xmap(CodecHelper::toMutable, Function.identity());
	private static final SavedDataType<RecentCraftedResultStorage> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "recent_crafted_results"), RecentCraftedResultStorage::new,
			RecordCodecBuilder.create(builder -> builder.group(
					Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), SCOPES_CODEC).xmap(CodecHelper::toMutable, Function.identity())
							.fieldOf("playerRecentResults").forGetter((RecentCraftedResultStorage storage) -> storage.playerRecentResults)
			).apply(builder, RecentCraftedResultStorage::new)));

	private final Map<UUID, Map<Identifier, Map<Identifier, List<Identifier>>>> playerRecentResults;
	private static Map<Identifier, Map<Identifier, List<Identifier>>> clientRecentResults = new HashMap<>();

	private RecentCraftedResultStorage() {
		playerRecentResults = new HashMap<>();
	}

	private RecentCraftedResultStorage(Map<UUID, Map<Identifier, Map<Identifier, List<Identifier>>>> playerRecentResults) {
		this.playerRecentResults = playerRecentResults;
	}

	public static RecentCraftedResultStorage get(ServerLevel level) {
		ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
		//noinspection ConstantConditions - overworld is loaded while server levels are available
		SavedDataStorage storage = overworld.getDataStorage();
		return storage.computeIfAbsent(TYPE);
	}

	public List<Identifier> getRecentResults(Player player, Identifier scope, Identifier ingredient) {
		return playerRecentResults.getOrDefault(player.getUUID(), Map.of())
				.getOrDefault(scope, Map.of())
				.getOrDefault(ingredient, List.of());
	}

	public static List<Identifier> getClientRecentResults(Identifier scope, Identifier ingredient) {
		return clientRecentResults.getOrDefault(scope, Map.of()).getOrDefault(ingredient, List.of());
	}

	public static void syncToPlayer(ServerPlayer player) {
		RecentCraftedResultStorage storage = get((ServerLevel) player.level());
		PacketDistributor.sendToPlayer(player, new SyncRecentCraftedResultsPayload(storage.serializePlayerResults(player)));
	}

	public CompoundTag serializePlayerResults(Player player) {
		return serializeScopes(playerRecentResults.getOrDefault(player.getUUID(), Map.of()));
	}

	public static void updateClientRecentResults(CompoundTag recentResults) {
		clientRecentResults = deserializeScopes(recentResults);
	}

	public boolean recordCraftedResult(Player player, Identifier scope, Identifier ingredient, Identifier result) {
		List<Identifier> recentResults = playerRecentResults.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>())
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

	private static CompoundTag serializeScopes(Map<Identifier, Map<Identifier, List<Identifier>>> scopes) {
		CompoundTag tag = new CompoundTag();
		tag.put(SCOPES_TAG, SCOPES_CODEC.encodeStart(NbtOps.INSTANCE, scopes).getOrThrow());
		return tag;
	}

	private static Map<Identifier, Map<Identifier, List<Identifier>>> deserializeScopes(CompoundTag tag) {
		if (!tag.contains(SCOPES_TAG)) {
			return new HashMap<>();
		}
		return SCOPES_CODEC.parse(NbtOps.INSTANCE, tag.get(SCOPES_TAG)).result().orElseGet(HashMap::new);
	}
}
