package net.p3pp3rf1y.sophisticatedcore.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.LevelEvent;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings({"squid:S2160", "squid:MaximumInheritanceDepth"})
public class CoreFakePlayer extends FakePlayer {
	private final NonNullList<ItemStack> fakePlayerHandInventory = NonNullList.withSize(2, ItemStack.EMPTY);
	private static final String FAKE_PLAYER_USERNAME = "sophisticated_core_fake_player";

	private static Map<ServerLevel, CoreFakePlayer> fakePlayers = new HashMap<>();
	private Vec3 position = Vec3.ZERO;
	private BlockPos blockPosition = BlockPos.ZERO;

	public static CoreFakePlayer get(ServerLevel level) {
		return fakePlayers.computeIfAbsent(level, CoreFakePlayer::new);
	}

	public static void onDimensionUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel level) {
			fakePlayers.remove(level);
		}
	}

	private CoreFakePlayer(ServerLevel level) {
		this(level, new GameProfile(UUID.nameUUIDFromBytes(FAKE_PLAYER_USERNAME.getBytes()), FAKE_PLAYER_USERNAME));
	}

	private CoreFakePlayer(ServerLevel level, GameProfile name) {
		super(level, name);
	}

	@Override
	protected void onEffectAdded(MobEffectInstance effect, @Nullable Entity entity) {
		// noop
	}

	@Override
	protected void onEffectUpdated(MobEffectInstance effect, boolean updateAttributes, @Nullable Entity entity) {
		// noop
	}

	@Override
	protected void onEffectsRemoved(Collection<MobEffectInstance> effects) {
		// noop
	}

	@Override
	public Vec3 position() {
		return position;
	}

	@Override
	public BlockPos blockPosition() {
		return blockPosition;
	}

	public void setPosition(Vec3 position) {
		this.position = position;
		blockPosition = BlockPos.containing(position);
	}
}
