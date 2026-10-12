package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorldHelperTest {
	private List<BiPredicate<Player, BlockPos>> blockChecks;
	private List<BiPredicate<Player, Entity>> entityChecks;
	private List<BiPredicate<Player, BlockPos>> originalBlockChecks;
	private List<BiPredicate<Player, Entity>> originalEntityChecks;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@BeforeEach
	void isolateChecks() throws ReflectiveOperationException {
		blockChecks = getChecks("ADDITIONAL_INTERACTION_CHECKS");
		entityChecks = getChecks("ADDITIONAL_ENTITY_INTERACTION_CHECKS");
		originalBlockChecks = new ArrayList<>(blockChecks);
		originalEntityChecks = new ArrayList<>(entityChecks);
		blockChecks.clear();
		entityChecks.clear();
	}

	@AfterEach
	void restoreChecks() {
		blockChecks.clear();
		blockChecks.addAll(originalBlockChecks);
		entityChecks.clear();
		entityChecks.addAll(originalEntityChecks);
	}

	@Test
	void playerMayInteractUsesSuppliedLevelAndOriginalPosition() {
		Player player = mock(Player.class);
		ServerLevel targetLevel = mock(ServerLevel.class);
		BlockPos pos = new BlockPos(23, 70, 41);
		when(player.mayInteract(targetLevel, pos)).thenReturn(true);
		WorldHelper.addAdditionalInteractionCheck((actor, target) -> actor == player && target.equals(pos));

		boolean allowed = WorldHelper.playerMayInteract(player, targetLevel, pos);

		assertTrue(allowed);
		verify(player).mayInteract(targetLevel, pos);
	}

	@Test
	void playerMayInteractExistingOverloadUsesPlayersLevel() {
		Player player = mock(Player.class);
		ServerLevel level = mock(ServerLevel.class);
		when(player.level()).thenReturn(level);
		when(player.mayInteract(level, BlockPos.ZERO)).thenReturn(true);

		boolean allowed = WorldHelper.playerMayInteract(player, BlockPos.ZERO);

		assertTrue(allowed);
		verify(player).mayInteract(level, BlockPos.ZERO);
	}

	@Test
	void playerMayInteractRejectsBlockWhenAnyClaimCheckDenies() {
		Player player = mock(Player.class);
		ServerLevel level = mock(ServerLevel.class);
		when(player.mayInteract(level, BlockPos.ZERO)).thenReturn(true);
		WorldHelper.addAdditionalInteractionCheck((actor, pos) -> true);
		WorldHelper.addAdditionalInteractionCheck((actor, pos) -> false);

		boolean allowed = WorldHelper.playerMayInteract(player, level, BlockPos.ZERO);

		assertFalse(allowed);
	}

	@Test
	void playerMayInteractRejectsVanillaDenialBeforeClaimChecks() {
		Player player = mock(Player.class);
		ServerLevel level = mock(ServerLevel.class);
		AtomicInteger claimChecks = new AtomicInteger();
		WorldHelper.addAdditionalInteractionCheck((actor, pos) -> {
			claimChecks.incrementAndGet();
			return true;
		});

		boolean allowed = WorldHelper.playerMayInteract(player, level, BlockPos.ZERO);

		assertFalse(allowed);
		assertEquals(0, claimChecks.get());
	}

	@Test
	void playerMayInteractAllowsClientDiscoveryWithoutServerClaimChecks() {
		Player player = mock(Player.class);
		Level level = mock(Level.class);
		when(level.isClientSide()).thenReturn(true);
		WorldHelper.addAdditionalInteractionCheck((actor, pos) -> {
			throw new AssertionError("Server claim checks must not run during client discovery");
		});

		boolean allowed = WorldHelper.playerMayInteract(player, level, BlockPos.ZERO);

		assertTrue(allowed);
		verifyNoInteractions(player);
	}

	@Test
	void playerMayInteractAllowsEntitiesWithoutClaimIntegrations() {
		Player player = mock(Player.class);
		Entity entity = mock(Entity.class);
		WorldHelper.addAdditionalInteractionCheck((actor, pos) -> false);

		boolean allowed = WorldHelper.playerMayInteract(player, entity);

		assertTrue(allowed);
	}

	@Test
	void playerMayInteractRejectsOnlyDeniedEntityInMixedTargets() {
		Player player = mock(Player.class);
		Entity allowedEntity = mock(Entity.class);
		Entity deniedEntity = mock(Entity.class);
		WorldHelper.addAdditionalEntityInteractionCheck((actor, entity) -> actor == player);
		WorldHelper.addAdditionalEntityInteractionCheck((actor, entity) -> entity != deniedEntity);

		boolean allowed = WorldHelper.playerMayInteract(player, allowedEntity);
		boolean denied = WorldHelper.playerMayInteract(player, deniedEntity);

		assertTrue(allowed);
		assertFalse(denied);
	}

	@SuppressWarnings("unchecked")
	private static <T> List<BiPredicate<Player, T>> getChecks(String name) throws ReflectiveOperationException {
		Field field = WorldHelper.class.getDeclaredField(name);
		field.setAccessible(true);
		return (List<BiPredicate<Player, T>>) field.get(null);
	}
}
